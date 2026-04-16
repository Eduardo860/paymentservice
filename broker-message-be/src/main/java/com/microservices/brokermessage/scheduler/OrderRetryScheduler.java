package com.microservices.brokermessage.scheduler;

import com.microservices.brokermessage.model.OrderRetryJob;
import com.microservices.brokermessage.service.OrderRetryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderRetryScheduler {

    private static final Logger logger = LoggerFactory.getLogger(OrderRetryScheduler.class);

    @Autowired
    private OrderRetryService orderRetryService;

    /**
     * Polls every 10 seconds for scheduled order retry jobs.
     */
    @Scheduled(fixedDelay = 10_000)
    public void processScheduledJobs() {
        List<OrderRetryJob> jobs = orderRetryService.findScheduledJobs();
        if (!jobs.isEmpty()) {
            logger.info("OrderRetryScheduler: processing {} scheduled job(s)", jobs.size());
            for (OrderRetryJob job : jobs) {
                try {
                    orderRetryService.executeJob(job);
                } catch (Exception e) {
                    logger.error("OrderRetryScheduler: error executing job id={} — {}", job.getId(), e.getMessage());
                }
            }
        }
    }
}
