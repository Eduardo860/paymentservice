package com.microservices.brokermessage.controller;

import com.microservices.brokermessage.dto.KafkaMessageDto;
import com.microservices.brokermessage.model.OrderRetryJob;
import com.microservices.brokermessage.model.PaymentRetryJob;
import com.microservices.brokermessage.model.ProductRetryJob;
import com.microservices.brokermessage.repository.OrderRetryJobRepository;
import com.microservices.brokermessage.repository.PaymentRetryJobRepository;
import com.microservices.brokermessage.repository.ProductRetryJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/retry")
public class RetryJobController {

    private static final Logger logger = LoggerFactory.getLogger(RetryJobController.class);

    @Autowired
    private ProductRetryJobRepository productRetryJobRepository;

    @Autowired
    private OrderRetryJobRepository orderRetryJobRepository;

    @Autowired
    private PaymentRetryJobRepository paymentRetryJobRepository;

    @Autowired
    private KafkaTemplate<String, KafkaMessageDto> kafkaTemplate;

    // ─── Listar jobs ──────────────────────────────────────────────────────────

    @GetMapping("/products")
    public ResponseEntity<List<ProductRetryJob>> getProductJobs() {
        return ResponseEntity.ok(productRetryJobRepository.findAll());
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<?> getProductJobById(@PathVariable UUID id) {
        return productRetryJobRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).body(err(404, "Job no encontrado: " + id)));
    }

    @GetMapping("/orders")
    public ResponseEntity<List<OrderRetryJob>> getOrderJobs() {
        return ResponseEntity.ok(orderRetryJobRepository.findAll());
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<?> getOrderJobById(@PathVariable UUID id) {
        return orderRetryJobRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).body(err(404, "Job no encontrado: " + id)));
    }

    @GetMapping("/payments")
    public ResponseEntity<List<PaymentRetryJob>> getPaymentJobs() {
        return ResponseEntity.ok(paymentRetryJobRepository.findAll());
    }

    @GetMapping("/payments/{id}")
    public ResponseEntity<?> getPaymentJobById(@PathVariable UUID id) {
        return paymentRetryJobRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).body(err(404, "Job no encontrado: " + id)));
    }

    // ─── Publicar mensajes Kafka (simulación de fallo) ────────────────────────

    @PostMapping("/trigger/product")
    public ResponseEntity<Map<String, Object>> triggerProductRetry(@RequestBody KafkaMessageDto message) {
        logger.info("Manual trigger: product_retry_jobs entityId={} action={}", message.getEntityId(), message.getAction());
        kafkaTemplate.send("product_retry_jobs", message);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("status", 200);
        res.put("message", "Mensaje enviado a product_retry_jobs");
        res.put("entityId", message.getEntityId());
        res.put("action", message.getAction());
        return ResponseEntity.ok(res);
    }

    @PostMapping("/trigger/order")
    public ResponseEntity<Map<String, Object>> triggerOrderRetry(@RequestBody KafkaMessageDto message) {
        logger.info("Manual trigger: order_retry_jobs entityId={} action={}", message.getEntityId(), message.getAction());
        kafkaTemplate.send("order_retry_jobs", message);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("status", 200);
        res.put("message", "Mensaje enviado a order_retry_jobs");
        res.put("entityId", message.getEntityId());
        res.put("action", message.getAction());
        return ResponseEntity.ok(res);
    }

    @PostMapping("/trigger/payment")
    public ResponseEntity<Map<String, Object>> triggerPaymentRetry(@RequestBody KafkaMessageDto message) {
        logger.info("Manual trigger: payments_retry_jobs entityId={} action={}", message.getEntityId(), message.getAction());
        kafkaTemplate.send("payments_retry_jobs", message);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("status", 200);
        res.put("message", "Mensaje enviado a payments_retry_jobs");
        res.put("entityId", message.getEntityId());
        res.put("action", message.getAction());
        return ResponseEntity.ok(res);
    }

    private Map<String, Object> err(int code, String message) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("status", code);
        r.put("message", message);
        return r;
    }
}
