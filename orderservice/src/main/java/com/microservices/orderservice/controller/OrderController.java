package com.microservices.orderservice.controller;

import com.microservices.orderservice.model.Order;
import com.microservices.orderservice.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/orders-api")
public class OrderController {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderRepository orderRepository;

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

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Order order) {
        logger.info("Creating new order for user: {}", order.getUserId());
        Order savedOrder = orderRepository.save(order);
        logger.info("Order created successfully with ID: {}", savedOrder.getId());
        return ResponseEntity.ok(ok(savedOrder));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getOrderById(@PathVariable String id) {
        logger.info("Fetching order with ID: {}", id);
        Optional<Order> order = orderRepository.findById(id);
        if (order.isPresent()) {
            logger.info("Order found: {}", id);
            return ResponseEntity.ok(ok(order.get()));
        }
        logger.warn("Order not found: {}", id);
        return ResponseEntity.status(404).body(err(404, "Orden no encontrada: " + id));
    }

    @GetMapping("/usuario/{userId}")
    public ResponseEntity<Map<String, Object>> getOrdersByUserId(@PathVariable Long userId) {
        logger.info("Fetching orders for user: {}", userId);
        List<Order> orders = orderRepository.findByUserId(userId);
        logger.info("Found {} orders for user: {}", orders.size(), userId);
        return ResponseEntity.ok(ok(orders));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateOrderStatus(@PathVariable String id, @RequestParam String status) {
        logger.info("Updating order {} status to: {}", id, status);
        Optional<Order> order = orderRepository.findById(id);
        if (order.isPresent()) {
            Order existingOrder = order.get();
            existingOrder.setStatus(status);
            Order updatedOrder = orderRepository.save(existingOrder);
            logger.info("Order {} status updated successfully", id);
            return ResponseEntity.ok(ok(updatedOrder));
        }
        logger.warn("Order not found for status update: {}", id);
        return ResponseEntity.status(404).body(err(404, "Orden no encontrada: " + id));
    }
}
