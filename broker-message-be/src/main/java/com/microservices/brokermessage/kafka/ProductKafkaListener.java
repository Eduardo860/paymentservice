package com.microservices.brokermessage.kafka;

import com.microservices.brokermessage.dto.KafkaMessageDto;
import com.microservices.brokermessage.service.ProductRetryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ProductKafkaListener {

    private static final Logger logger = LoggerFactory.getLogger(ProductKafkaListener.class);

    @Autowired
    private ProductRetryService productRetryService;

    @KafkaListener(topics = "product_retry_jobs", groupId = "broker-message-group")
    public void onMessage(KafkaMessageDto message) {
        logger.info("Kafka [product_retry_jobs] received: entityId={} action={}",
                message.getEntityId(), message.getAction());
        try {
            productRetryService.createFromKafkaMessage(message);
        } catch (Exception e) {
            logger.error("Failed to process Kafka message [product_retry_jobs]: {}", e.getMessage());
        }
    }
}
