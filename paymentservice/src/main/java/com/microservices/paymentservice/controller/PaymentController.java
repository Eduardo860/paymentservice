package com.microservices.paymentservice.controller;

import com.microservices.paymentservice.model.Payment;
import com.microservices.paymentservice.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/pagos-api")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    private Map<String, Object> ok(Object data) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("status", 200);
        r.put("data", data);
        return r;
    }

    private Map<String, Object> err(int code, String message) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("status", code);
        r.put("message", message);
        return r;
    }

    private void publishToKafka(String entityId, String action, Object requestData) {
        try {
            Map<String, Object> msg = new HashMap<>();
            msg.put("entityId", entityId);
            msg.put("action", action);
            msg.put("requestData", requestData);
            kafkaTemplate.send("payment_retry_jobs", msg);
            logger.info("Published to Kafka payment_retry_jobs entityId={} action={}", entityId, action);
        } catch (Exception e) {
            logger.error("Failed to publish to Kafka: {}", e.getMessage());
        }
    }

    @PostMapping("/procesar")
    public ResponseEntity<Map<String, Object>> processPayment(@RequestBody Payment payment) {
        logger.info("Processing payment for order: {} with amount: {}", payment.getOrderId(), payment.getAmount());
        try {
            payment.setStatus("COMPLETED");
            Payment savedPayment = paymentRepository.save(payment);
            logger.info("Payment processed successfully with ID: {}", savedPayment.getId());
            return ResponseEntity.ok(ok(savedPayment));
        } catch (Exception e) {
            logger.error("Failed to process payment, publishing to Kafka: {}", e.getMessage());
            publishToKafka(payment.getOrderId(), "CREATE", payment);
            return ResponseEntity.status(503).body(err(503, "Operación encolada para reintento: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPaymentById(@PathVariable String id) {
        logger.info("Fetching payment with ID: {}", id);
        Optional<Payment> payment = paymentRepository.findById(id);
        if (payment.isPresent()) {
            logger.info("Payment found: {}", id);
            return ResponseEntity.ok(ok(payment.get()));
        }
        logger.warn("Payment not found: {}", id);
        return ResponseEntity.status(404).body(err(404, "Pago no encontrado: " + id));
    }

    @GetMapping("/orden/{orderId}")
    public ResponseEntity<Map<String, Object>> getPaymentsByOrderId(@PathVariable String orderId) {
        logger.info("Fetching payments for order: {}", orderId);
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        logger.info("Found {} payments for order: {}", payments.size(), orderId);
        return ResponseEntity.ok(ok(payments));
    }

    @PutMapping("/{id}/reembolso")
    public ResponseEntity<Map<String, Object>> refundPayment(@PathVariable String id) {
        logger.info("Processing refund for payment: {}", id);
        try {
            Optional<Payment> payment = paymentRepository.findById(id);
            if (payment.isPresent()) {
                Payment existingPayment = payment.get();
                existingPayment.setStatus("REFUNDED");
                Payment refundedPayment = paymentRepository.save(existingPayment);
                logger.info("Payment {} refunded successfully", id);
                return ResponseEntity.ok(ok(refundedPayment));
            }
            logger.warn("Payment not found for refund: {}", id);
            return ResponseEntity.status(404).body(err(404, "Pago no encontrado: " + id));
        } catch (Exception e) {
            logger.error("Failed to refund payment {}, publishing to Kafka: {}", id, e.getMessage());
            Map<String, Object> data = new HashMap<>();
            data.put("status", "REFUNDED");
            publishToKafka(id, "UPDATE", data);
            return ResponseEntity.status(503).body(err(503, "Operación encolada para reintento: " + e.getMessage()));
        }
    }
}
