package com.microservices.brokermessage.kafka;

import com.microservices.brokermessage.dto.KafkaMessageDto;
import com.microservices.brokermessage.service.PaymentRetryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentKafkaListener {

    private static final Logger logger = LoggerFactory.getLogger(PaymentKafkaListener.class);

    @Autowired
    private PaymentRetryService paymentRetryService;

    @KafkaListener(topics = "payments_retry_jobs", groupId = "broker-message-group")
    public void onMessage(KafkaMessageDto message) {
        logger.info("Kafka [payments_retry_jobs] received: entityId={} action={}",
                message.getEntityId(), message.getAction());
        try {
            paymentRetryService.createFromKafkaMessage(message);
        } catch (Exception e) {
            logger.error("Failed to process Kafka message [payments_retry_jobs]: {}", e.getMessage());
        }
    }
}
