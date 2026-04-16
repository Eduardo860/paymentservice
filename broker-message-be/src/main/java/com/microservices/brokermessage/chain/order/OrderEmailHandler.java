package com.microservices.brokermessage.chain.order;

import com.microservices.brokermessage.chain.AbstractRetryHandler;
import com.microservices.brokermessage.dto.RetryContext;
import com.microservices.brokermessage.dto.StepResult;
import com.microservices.brokermessage.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Step 2 — Sends a success notification email after the API call succeeds.
 * Email failures do not stop the chain.
 */
@Component
public class OrderEmailHandler extends AbstractRetryHandler {

    @Autowired
    private EmailService emailService;

    @Value("${notification.email.default-to}")
    private String defaultTo;

    @Override
    public void handle(RetryContext context) {
        logger.info("OrderEmailHandler: sending notification for job={}", context.getJobId());
        try {
            String subject = "[Order Retry] " + context.getAction() + " SUCCESS — entity: " + context.getEntityId();
            String body = String.format(
                "Order retry job completed successfully.%n%n" +
                "Job ID  : %s%n" +
                "Entity  : %s%n" +
                "Action  : %s%n%n" +
                "Result  : %s",
                context.getJobId(),
                context.getEntityId(),
                context.getAction(),
                context.getTracking().getData().getMessage()
            );
            emailService.sendEmail(defaultTo, subject, body);
            context.getTracking().setSendEmail(new StepResult("SUCCESS", "Email sent successfully"));
            logger.info("OrderEmailHandler: email sent for job={}", context.getJobId());

        } catch (Exception e) {
            logger.warn("OrderEmailHandler: email failed for job={} — {}", context.getJobId(), e.getMessage());
            context.getTracking().setSendEmail(new StepResult("FAILED", e.getMessage()));
        }

        handleNext(context);
    }
}
