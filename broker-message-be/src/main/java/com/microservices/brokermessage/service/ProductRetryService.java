package com.microservices.brokermessage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.brokermessage.chain.product.ProductApiCallHandler;
import com.microservices.brokermessage.chain.product.ProductEmailHandler;
import com.microservices.brokermessage.chain.product.ProductStatusUpdateHandler;
import com.microservices.brokermessage.dto.KafkaMessageDto;
import com.microservices.brokermessage.dto.RetryContext;
import com.microservices.brokermessage.model.ProductRetryJob;
import com.microservices.brokermessage.repository.ProductRetryJobRepository;
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
public class ProductRetryService {

    private static final Logger logger = LoggerFactory.getLogger(ProductRetryService.class);

    @Autowired private ProductRetryJobRepository repository;
    @Autowired private ProductApiCallHandler apiCallHandler;
    @Autowired private ProductEmailHandler emailHandler;
    @Autowired private ProductStatusUpdateHandler statusUpdateHandler;
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
    public ProductRetryJob createFromKafkaMessage(KafkaMessageDto message) {
        try {
            String productId = message.getEntityId() != null ? message.getEntityId() : "UNKNOWN";
            String action = message.getAction() != null ? message.getAction() : "CREATE";
            
            // IDEMPOTENCIA: Verificar si ya existe un job pendiente para este producto y acción
            List<ProductRetryJob> existingJobs = repository.findByProductIdAndActionAndStatusIn(
                productId, 
                action, 
                List.of("SCHEDULED", "RUNNING")
            );
            
            if (!existingJobs.isEmpty()) {
                ProductRetryJob existing = existingJobs.get(0);
                logger.warn("ProductRetryJob already exists for productId={} action={} - Skipping duplicate. Existing job id={}", 
                    productId, action, existing.getId());
                return existing;
            }
            
            ProductRetryJob job = new ProductRetryJob();
            job.setProductId(productId);
            job.setAction(action);
            job.setStatus("SCHEDULED");
            job.setAttempt(0);
            job.setNextRunAt(OffsetDateTime.now());
            if (message.getRequestData() != null) {
                job.setRequestData(objectMapper.writeValueAsString(message.getRequestData()));
            }
            ProductRetryJob saved = repository.save(job);
            logger.info("ProductRetryJob created id={} productId={} action={}", saved.getId(), saved.getProductId(), saved.getAction());
            return saved;
        } catch (Exception e) {
            logger.error("Failed to create ProductRetryJob: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public List<ProductRetryJob> findScheduledJobs() {
        return repository.findByStatusAndNextRunAtLessThanEqual("SCHEDULED", OffsetDateTime.now());
    }

    @Transactional
    public void executeJob(ProductRetryJob job) {
        logger.info("Executing ProductRetryJob id={} attempt={}", job.getId(), job.getAttempt());
        job.setStatus("RUNNING");
        repository.save(job);

        RetryContext context = new RetryContext();
        context.setJobId(job.getId().toString());
        context.setEntityId(job.getProductId());
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

    private void handleJobFailure(ProductRetryJob job, String errorMessage) {
        int newAttempt = job.getAttempt() + 1;
        job.setAttempt(newAttempt);

        if (newAttempt >= maxAttempts) {
            job.setStatus("FAILED");
            logger.warn("ProductRetryJob id={} permanently FAILED after {} attempts", job.getId(), newAttempt);
            try {
                emailService.sendFailureEmail(notificationEmail, job.getId().toString(),
                        job.getProductId(), job.getAction(), errorMessage);
            } catch (Exception e) {
                logger.error("Could not send failure email: {}", e.getMessage());
            }
        } else {
            long backoffSeconds = initialBackoffSeconds * (long) Math.pow(2, newAttempt - 1);
            job.setNextRunAt(OffsetDateTime.now().plusSeconds(backoffSeconds));
            job.setStatus("SCHEDULED");
            logger.info("ProductRetryJob id={} rescheduled in {}s (attempt {})", job.getId(), backoffSeconds, newAttempt);
        }

        job.setResponseData("{\"error\":" + safeJsonString(errorMessage) + "}");
        repository.save(job);
    }

    private String safeJsonString(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
