package com.microservices.brokermessage.scheduler;

import com.microservices.brokermessage.model.PaymentRetryJob;
import com.microservices.brokermessage.service.PaymentRetryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentRetryScheduler {

    private static final Logger logger = LoggerFactory.getLogger(PaymentRetryScheduler.class);

    @Autowired
    private PaymentRetryService paymentRetryService;

    /**
     * Polls every 10 seconds for scheduled payment retry jobs.
     */
    @Scheduled(fixedDelay = 10_000)
    public void processScheduledJobs() {
        List<PaymentRetryJob> jobs = paymentRetryService.findScheduledJobs();
        if (!jobs.isEmpty()) {
            logger.info("PaymentRetryScheduler: processing {} scheduled job(s)", jobs.size());
            for (PaymentRetryJob job : jobs) {
                try {
                    paymentRetryService.executeJob(job);
                } catch (Exception e) {
                    logger.error("PaymentRetryScheduler: error executing job id={} — {}", job.getId(), e.getMessage());
                }
            }
        }
    }
}
