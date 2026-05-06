package com.microservices.brokermessage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.brokermessage.chain.payment.PaymentApiCallHandler;
import com.microservices.brokermessage.chain.payment.PaymentEmailHandler;
import com.microservices.brokermessage.chain.payment.PaymentStatusUpdateHandler;
import com.microservices.brokermessage.dto.KafkaMessageDto;
import com.microservices.brokermessage.dto.RetryContext;
import com.microservices.brokermessage.model.PaymentRetryJob;
import com.microservices.brokermessage.repository.PaymentRetryJobRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class PaymentRetryService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentRetryService.class);

    @Autowired private PaymentRetryJobRepository repository;
    @Autowired private PaymentApiCallHandler apiCallHandler;
    @Autowired private PaymentEmailHandler emailHandler;
    @Autowired private PaymentStatusUpdateHandler statusUpdateHandler;
    @Autowired private EmailService emailService;
    @Autowired private ObjectMapper objectMapper;

    @Value("${retry.max-attempts:5}")
    private int maxAttempts;

    @Value("${retry.initial-backoff-seconds:10}")
    private long initialBackoffSeconds;

    @Value("${notification.email.default-to}")
    private String notificationEmail;

    @PostConstruct
    public void buildChain() {
        apiCallHandler.setNext(emailHandler);
        emailHandler.setNext(statusUpdateHandler);
        statusUpdateHandler.setNext(null);
    }

    @Transactional
    public PaymentRetryJob createFromKafkaMessage(KafkaMessageDto message) {
        try {
            String paymentId = message.getEntityId() != null ? message.getEntityId() : "UNKNOWN";
            String action = message.getAction() != null ? message.getAction() : "CREATE";
            
            // IDEMPOTENCIA: Verificar si ya existe un job pendiente para este pago y acción
            List<PaymentRetryJob> existingJobs = repository.findByPaymentIdAndActionAndStatusIn(
                paymentId, 
                action, 
                List.of("SCHEDULED", "RUNNING")
            );
            
            if (!existingJobs.isEmpty()) {
                PaymentRetryJob existing = existingJobs.get(0);
                logger.warn("PaymentRetryJob already exists for paymentId={} action={} - Skipping duplicate. Existing job id={}", 
                    paymentId, action, existing.getId());
                return existing;
            }
            
            PaymentRetryJob job = new PaymentRetryJob();
            job.setPaymentId(paymentId);
            job.setAction(action);
            job.setStatus("SCHEDULED");
            job.setAttempt(0);
            job.setNextRunAt(OffsetDateTime.now());
            if (message.getRequestData() != null) {
                job.setRequestData(objectMapper.writeValueAsString(message.getRequestData()));
            }
            PaymentRetryJob saved = repository.save(job);
            logger.info("PaymentRetryJob created id={} paymentId={} action={}", saved.getId(), saved.getPaymentId(), saved.getAction());
            return saved;
        } catch (Exception e) {
            logger.error("Failed to create PaymentRetryJob: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public List<PaymentRetryJob> findScheduledJobs() {
        return repository.findByStatusAndNextRunAtLessThanEqual("SCHEDULED", OffsetDateTime.now());
    }

    @Transactional
    public void executeJob(PaymentRetryJob job) {
        logger.info("Executing PaymentRetryJob id={} attempt={}", job.getId(), job.getAttempt());
        job.setStatus("RUNNING");
        repository.save(job);

        RetryContext context = new RetryContext();
        context.setJobId(job.getId().toString());
        context.setEntityId(job.getPaymentId());
        context.setAction(job.getAction());
        context.setRequestDataJson(job.getRequestData());

        try {
            apiCallHandler.handle(context);
        } catch (Exception e) {
            context.setFailed(true);
            context.setErrorMessage(e.getMessage());
        }

        if (context.isFailed()) {
            handleJobFailure(job, context.getErrorMessage());
        }
    }

    private void handleJobFailure(PaymentRetryJob job, String errorMessage) {
        int newAttempt = job.getAttempt() + 1;
        job.setAttempt(newAttempt);

        if (newAttempt >= maxAttempts) {
            job.setStatus("FAILED");
            logger.warn("PaymentRetryJob id={} permanently FAILED after {} attempts", job.getId(), newAttempt);
            try {
                emailService.sendFailureEmail(notificationEmail, job.getId().toString(),
                        job.getPaymentId(), job.getAction(), errorMessage);
            } catch (Exception e) {
                logger.error("Could not send failure email: {}", e.getMessage());
            }
        } else {
            long backoffSeconds = initialBackoffSeconds * (long) Math.pow(2, newAttempt - 1);
            job.setNextRunAt(OffsetDateTime.now().plusSeconds(backoffSeconds));
            job.setStatus("SCHEDULED");
            logger.info("PaymentRetryJob id={} rescheduled in {}s (attempt {})", job.getId(), backoffSeconds, newAttempt);
        }

        job.setResponseData("{\"error\":" + safeJsonString(errorMessage) + "}");
        repository.save(job);
    }

    private String safeJsonString(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
