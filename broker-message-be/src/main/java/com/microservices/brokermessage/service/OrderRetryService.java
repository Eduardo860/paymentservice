package com.microservices.brokermessage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.brokermessage.chain.order.OrderApiCallHandler;
import com.microservices.brokermessage.chain.order.OrderEmailHandler;
import com.microservices.brokermessage.chain.order.OrderStatusUpdateHandler;
import com.microservices.brokermessage.dto.KafkaMessageDto;
import com.microservices.brokermessage.dto.RetryContext;
import com.microservices.brokermessage.model.OrderRetryJob;
import com.microservices.brokermessage.repository.OrderRetryJobRepository;
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
public class OrderRetryService {

    private static final Logger logger = LoggerFactory.getLogger(OrderRetryService.class);

    @Autowired private OrderRetryJobRepository repository;
    @Autowired private OrderApiCallHandler apiCallHandler;
    @Autowired private OrderEmailHandler emailHandler;
    @Autowired private OrderStatusUpdateHandler statusUpdateHandler;
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
    public OrderRetryJob createFromKafkaMessage(KafkaMessageDto message) {
        try {
            OrderRetryJob job = new OrderRetryJob();
            job.setOrderId(message.getEntityId() != null ? message.getEntityId() : "UNKNOWN");
            job.setAction(message.getAction() != null ? message.getAction() : "CREATE");
            job.setStatus("SCHEDULED");
            job.setAttempt(0);
            job.setNextRunAt(OffsetDateTime.now());
            if (message.getRequestData() != null) {
                job.setRequestData(objectMapper.writeValueAsString(message.getRequestData()));
            }
            OrderRetryJob saved = repository.save(job);
            logger.info("OrderRetryJob created id={} action={}", saved.getId(), saved.getAction());
            return saved;
        } catch (Exception e) {
            logger.error("Failed to create OrderRetryJob: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public List<OrderRetryJob> findScheduledJobs() {
        return repository.findByStatusAndNextRunAtLessThanEqual("SCHEDULED", OffsetDateTime.now());
    }

    @Transactional
    public void executeJob(OrderRetryJob job) {
        logger.info("Executing OrderRetryJob id={} attempt={}", job.getId(), job.getAttempt());
        job.setStatus("RUNNING");
        repository.save(job);

        RetryContext context = new RetryContext();
        context.setJobId(job.getId().toString());
        context.setEntityId(job.getOrderId());
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

    private void handleJobFailure(OrderRetryJob job, String errorMessage) {
        int newAttempt = job.getAttempt() + 1;
        job.setAttempt(newAttempt);

        if (newAttempt >= maxAttempts) {
            job.setStatus("FAILED");
            logger.warn("OrderRetryJob id={} permanently FAILED after {} attempts", job.getId(), newAttempt);
            try {
                emailService.sendFailureEmail(notificationEmail, job.getId().toString(),
                        job.getOrderId(), job.getAction(), errorMessage);
            } catch (Exception e) {
                logger.error("Could not send failure email: {}", e.getMessage());
            }
        } else {
            long backoffSeconds = initialBackoffSeconds * (long) Math.pow(2, newAttempt - 1);
            job.setNextRunAt(OffsetDateTime.now().plusSeconds(backoffSeconds));
            job.setStatus("SCHEDULED");
            logger.info("OrderRetryJob id={} rescheduled in {}s (attempt {})", job.getId(), backoffSeconds, newAttempt);
        }

        job.setResponseData("{\"error\":" + safeJsonString(errorMessage) + "}");
        repository.save(job);
    }

    private String safeJsonString(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
