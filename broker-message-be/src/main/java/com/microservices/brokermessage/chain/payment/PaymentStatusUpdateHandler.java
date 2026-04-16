package com.microservices.brokermessage.chain.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.brokermessage.chain.AbstractRetryHandler;
import com.microservices.brokermessage.dto.RetryContext;
import com.microservices.brokermessage.dto.StepResult;
import com.microservices.brokermessage.model.PaymentRetryJob;
import com.microservices.brokermessage.repository.PaymentRetryJobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Step 3 — Persists the full tracking payload and marks the job as SUCCESS.
 */
@Component
public class PaymentStatusUpdateHandler extends AbstractRetryHandler {

    @Autowired
    private PaymentRetryJobRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void handle(RetryContext context) {
        logger.info("PaymentStatusUpdateHandler: updating job={}", context.getJobId());
        try {
            Optional<PaymentRetryJob> optJob = repository.findById(UUID.fromString(context.getJobId()));
            if (optJob.isPresent()) {
                PaymentRetryJob job = optJob.get();
                job.setStatus("SUCCESS");
                job.setResponseData(objectMapper.writeValueAsString(context.getTracking()));
                repository.save(job);
                context.getTracking().setUpdateRetryJobs(new StepResult("SUCCESS", "Job updated to SUCCESS"));
                logger.info("PaymentStatusUpdateHandler: job={} marked as SUCCESS", context.getJobId());
            } else {
                logger.warn("PaymentStatusUpdateHandler: job not found id={}", context.getJobId());
                context.getTracking().setUpdateRetryJobs(new StepResult("FAILED", "Job not found: " + context.getJobId()));
            }
        } catch (Exception e) {
            logger.error("PaymentStatusUpdateHandler: failed for job={} — {}", context.getJobId(), e.getMessage());
            context.getTracking().setUpdateRetryJobs(new StepResult("FAILED", e.getMessage()));
        }

        handleNext(context);
    }
}
