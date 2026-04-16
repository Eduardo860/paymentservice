package com.microservices.brokermessage.scheduler;

import com.microservices.brokermessage.model.ProductRetryJob;
import com.microservices.brokermessage.service.ProductRetryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductRetryScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ProductRetryScheduler.class);

    @Autowired
    private ProductRetryService productRetryService;

    /**
     * Polls every 10 seconds for scheduled product retry jobs.
     * fixedDelay ensures previous execution finishes before the next starts.
     */
    @Scheduled(fixedDelay = 10_000)
    public void processScheduledJobs() {
        List<ProductRetryJob> jobs = productRetryService.findScheduledJobs();
        if (!jobs.isEmpty()) {
            logger.info("ProductRetryScheduler: processing {} scheduled job(s)", jobs.size());
            for (ProductRetryJob job : jobs) {
                try {
                    productRetryService.executeJob(job);
                } catch (Exception e) {
                    logger.error("ProductRetryScheduler: error executing job id={} — {}", job.getId(), e.getMessage());
                }
            }
        }
    }
}
