package com.microservices.orderservice.controller;

import com.microservices.orderservice.model.Order;
import com.microservices.orderservice.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
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

    @Autowired
    private KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${PRODUCT_SERVICE_URL:http://apigateway:8080/productos-api}")
    private String productServiceUrl;

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
            kafkaTemplate.send("order_retry_jobs", msg);
            logger.info("Published to Kafka order_retry_jobs entityId={} action={}", entityId, action);
        } catch (Exception e) {
            logger.error("Failed to publish to Kafka: {}", e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Order order) {
        logger.info("Creating new order for user: {}", order.getUserId());
        try {
            // Verificar stock
            if (order.getProducts() != null && !order.getProducts().isEmpty()) {
                for (Order.Product p : order.getProducts()) {
                    if (p.getQuantity() == null || p.getQuantity() <= 0) {
                        return ResponseEntity.badRequest().body(err(400, "La cantidad del producto debe ser mayor a cero"));
                    }
                    Map<String, Object> prodResponse = restTemplate.getForObject(
                            productServiceUrl + "/" + p.getProductId(),
                            Map.class
                    );
                    if (prodResponse == null || !prodResponse.containsKey("data")) {
                        return ResponseEntity.status(404).body(err(404, "Producto no encontrado: " + p.getProductId()));
                    }
                    Map<String, Object> prodData = (Map<String, Object>) prodResponse.get("data");
                    Integer stock = (Integer) prodData.get("stock");
                    if (stock == null || stock < p.getQuantity()) {
                        return ResponseEntity.status(400).body(err(400, "Stock insuficiente para el producto: " + p.getProductId() + ". Stock disponible: " + (stock == null ? 0 : stock)));
                    }
                }
            } else if (order.getProductId() != null) {
                Map<String, Object> prodResponse = restTemplate.getForObject(
                        productServiceUrl + "/" + order.getProductId(),
                        Map.class
                );
                if (prodResponse == null || !prodResponse.containsKey("data")) {
                    return ResponseEntity.status(404).body(err(404, "Producto no encontrado: " + order.getProductId()));
                }
                Map<String, Object> prodData = (Map<String, Object>) prodResponse.get("data");
                Integer stock = (Integer) prodData.get("stock");
                if (stock == null || stock < 1) {
                    return ResponseEntity.status(400).body(err(400, "Stock insuficiente para el producto: " + order.getProductId()));
                }
            }

            Order savedOrder = orderRepository.save(order);
            logger.info("Order created successfully with ID: {}", savedOrder.getId());
            
            // Publicar evento inventory_update_events
            publishInventoryUpdateEvent(savedOrder);
            
            return ResponseEntity.ok(ok(savedOrder));
        } catch (Exception e) {
            logger.error("Failed to create order, publishing to Kafka: {}", e.getMessage());
            publishToKafka("new", "CREATE", order);
            return ResponseEntity.status(503).body(err(503, "Operación encolada para reintento: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllOrders() {
        logger.info("Fetching all orders");
        List<Order> orders = orderRepository.findAll();
        return ResponseEntity.ok(ok(orders));
    }

    @GetMapping("/producto/{productId}")
    public ResponseEntity<Map<String, Object>> isProductInOrder(@PathVariable String productId) {
        logger.info("Checking if product {} is in any order", productId);
        List<Order> orders = orderRepository.findAll();
        boolean found = false;
        for (Order o : orders) {
            if (productId.equals(o.getProductId())) {
                found = true;
                break;
            }
            if (o.getProducts() != null) {
                for (Order.Product p : o.getProducts()) {
                    if (productId.equals(p.getProductId())) {
                        found = true;
                        break;
                    }
                }
            }
            if (found) break;
        }
        return ResponseEntity.ok(ok(found));
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
        try {
            Optional<Order> order = orderRepository.findById(id);
            if (order.isPresent()) {
                Order existingOrder = order.get();
                existingOrder.setStatus(status);
                Order updatedOrder = orderRepository.save(existingOrder);
                logger.info("Order {} status updated successfully", id);
                
                // Publicar evento order_status_changed_events
                publishOrderStatusChangedEvent(updatedOrder, status);
                
                return ResponseEntity.ok(ok(updatedOrder));
            }
            logger.warn("Order not found for status update: {}", id);
            return ResponseEntity.status(404).body(err(404, "Orden no encontrada: " + id));
        } catch (Exception e) {
            logger.error("Failed to update order {}, publishing to Kafka: {}", id, e.getMessage());
            Map<String, Object> data = new HashMap<>();
            data.put("status", status);
            publishToKafka(id, "UPDATE", data);
            return ResponseEntity.status(503).body(err(503, "Operación encolada para reintento: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateOrder(@PathVariable String id, @RequestBody Order order) {
        logger.info("Updating order: {}", id);
        try {
            Optional<Order> existingOrder = orderRepository.findById(id);
            if (existingOrder.isPresent()) {
                Order current = existingOrder.get();
                
                // Verificar si afecta inventario
                boolean affectsInventory = hasInventoryChanges(current, order);
                
                // Actualizar campos
                if (order.getTotalAmount() != null) current.setTotalAmount(order.getTotalAmount());
                if (order.getStatus() != null) current.setStatus(order.getStatus());
                if (order.getCustomerEmail() != null) current.setCustomerEmail(order.getCustomerEmail());
                if (order.getProducts() != null) current.setProducts(order.getProducts());
                
                Order updatedOrder = orderRepository.save(current);
                logger.info("Order {} updated successfully", id);
                
                // Si afecta inventario, publicar evento
                if (affectsInventory) {
                    publishInventoryUpdateEvent(updatedOrder);
                }
                
                return ResponseEntity.ok(ok(updatedOrder));
            }
            logger.warn("Order not found: {}", id);
            return ResponseEntity.status(404).body(err(404, "Orden no encontrada: " + id));
        } catch (Exception e) {
            logger.error("Failed to update order {}: {}", id, e.getMessage());
            publishToKafka(id, "UPDATE", order);
            return ResponseEntity.status(503).body(err(503, "Operación encolada para reintento: " + e.getMessage()));
        }
    }

    private boolean hasInventoryChanges(Order current, Order updated) {
        if (updated.getProducts() == null) return false;
        if (current.getProducts() == null) return true;
        return !current.getProducts().equals(updated.getProducts());
    }

    private void publishInventoryUpdateEvent(Order order) {
        try {
            if (order.getProducts() == null || order.getProducts().isEmpty()) {
                logger.warn("No products to update inventory for order: {}", order.getId());
                return;
            }
            
            Map<String, Object> event = new HashMap<>();
            event.put("id", java.util.UUID.randomUUID().toString());
            event.put("orderId", order.getId());
            event.put("products", order.getProducts());
            event.put("status", "PENDING");
            event.put("timestamp", java.time.LocalDateTime.now().toString());
            
            kafkaTemplate.send("inventory_update_events", event);
            logger.info("Published inventory_update_events for order: {}", order.getId());
        } catch (Exception e) {
            logger.error("Failed to publish inventory_update_events: {}", e.getMessage());
        }
    }

    private void publishOrderStatusChangedEvent(Order order, String newStatus) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("id", java.util.UUID.randomUUID().toString());
            event.put("orderId", order.getId());
            event.put("newStatus", newStatus);
            event.put("customerEmail", order.getCustomerEmail());
            event.put("timestamp", java.time.LocalDateTime.now().toString());
            
            kafkaTemplate.send("order_status_changed_events", event);
            logger.info("Published order_status_changed_events for order: {}", order.getId());
        } catch (Exception e) {
            logger.error("Failed to publish order_status_changed_events: {}", e.getMessage());
        }
    }
}
