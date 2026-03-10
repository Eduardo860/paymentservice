package com.microservices.paymentservice.controller;

import com.microservices.paymentservice.model.Payment;
import com.microservices.paymentservice.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/pagos-api")
public class PaymentController {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private PaymentRepository paymentRepository;

    @PostMapping("/procesar")
    public ResponseEntity<Payment> processPayment(@RequestBody Payment payment) {
        logger.info("Processing payment for order: {} with amount: {}", payment.getOrderId(), payment.getAmount());
        payment.setStatus("COMPLETED");
        Payment savedPayment = paymentRepository.save(payment);
        logger.info("Payment processed successfully with ID: {}", savedPayment.getId());
        return ResponseEntity.ok(savedPayment);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable String id) {
        logger.info("Fetching payment with ID: {}", id);
        Optional<Payment> payment = paymentRepository.findById(id);
        if (payment.isPresent()) {
            logger.info("Payment found: {}", id);
            return ResponseEntity.ok(payment.get());
        }
        logger.warn("Payment not found: {}", id);
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/orden/{orderId}")
    public ResponseEntity<List<Payment>> getPaymentsByOrderId(@PathVariable String orderId) {
        logger.info("Fetching payments for order: {}", orderId);
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        logger.info("Found {} payments for order: {}", payments.size(), orderId);
        return ResponseEntity.ok(payments);
    }

    @PutMapping("/{id}/reembolso")
    public ResponseEntity<Payment> refundPayment(@PathVariable String id) {
        logger.info("Processing refund for payment: {}", id);
        Optional<Payment> payment = paymentRepository.findById(id);
        if (payment.isPresent()) {
            Payment existingPayment = payment.get();
            existingPayment.setStatus("REFUNDED");
            Payment refundedPayment = paymentRepository.save(existingPayment);
            logger.info("Payment {} refunded successfully", id);
            return ResponseEntity.ok(refundedPayment);
        }
        logger.warn("Payment not found for refund: {}", id);
        return ResponseEntity.notFound().build();
    }
}
