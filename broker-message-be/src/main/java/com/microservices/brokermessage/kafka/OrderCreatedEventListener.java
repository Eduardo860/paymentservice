package com.microservices.brokermessage.kafka;

import com.microservices.brokermessage.entity.Order;
import com.microservices.brokermessage.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Escucha el evento inventory_update_events que se publica cuando se crea una orden.
 * Guarda la orden en PostgreSQL.
 */
@Component
public class OrderCreatedEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderCreatedEventListener.class);
    
    @Autowired
    private OrderRepository orderRepository;
    
    @KafkaListener(
        topics = "inventory_update_events", 
        groupId = "order-created-postgres-group",
        containerFactory = "eventKafkaListenerContainerFactory"
    )
    public void consumeInventoryUpdateEvent(Map<String, Object> event) {
        logger.info("OrderCreatedEventListener consumed inventory_update_events: {}", event);
        
        try {
            String orderId = (String) event.get("orderId");
            
            if (orderId == null) {
                logger.warn("Invalid event data: orderId is null");
                return;
            }
            
            // Verificar si la orden ya existe en PostgreSQL
            if (orderRepository.existsById(orderId)) {
                logger.info("Order {} already exists in PostgreSQL, skipping creation", orderId);
                return;
            }
            
            // Crear nueva orden en PostgreSQL
            Order order = new Order();
            order.setId(orderId);
            order.setStatus("PENDING"); // Status inicial
            
            // Extraer productos y calcular datos si están disponibles
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> products = 
                (java.util.List<Map<String, Object>>) event.get("products");
            
            if (products != null && !products.isEmpty()) {
                // Tomar el primer producto como referencia (si solo hay uno)
                Map<String, Object> firstProduct = products.get(0);
                order.setProductId((String) firstProduct.get("productId"));
            }
            
            orderRepository.save(order);
            logger.info("Created new order {} in PostgreSQL from inventory_update_events", orderId);
            
        } catch (Exception e) {
            logger.error("Error processing inventory_update_events for order creation: {}", e.getMessage(), e);
        }
    }
}
