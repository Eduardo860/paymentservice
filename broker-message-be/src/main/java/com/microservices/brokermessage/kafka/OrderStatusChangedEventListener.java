package com.microservices.brokermessage.kafka;

import com.microservices.brokermessage.entity.Order;
import com.microservices.brokermessage.repository.OrderRepository;
import com.microservices.brokermessage.service.EmailService;
import com.microservices.brokermessage.service.EnvioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Component
public class OrderStatusChangedEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderStatusChangedEventListener.class);
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private EnvioService envioService;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @KafkaListener(
        topics = "order_status_changed_events", 
        groupId = "order-status-group",
        containerFactory = "eventKafkaListenerContainerFactory"
    )
    public void consumeOrderStatusChangedEvent(Map<String, Object> event) {
        logger.info("Consumed order_status_changed_events: {}", event);
        
        try {
            String orderId = (String) event.get("orderId");
            String newStatus = (String) event.get("newStatus");
            String customerEmail = (String) event.get("customerEmail");
            
            if (orderId == null || newStatus == null) {
                logger.warn("Invalid event data: orderId={}, newStatus={}", orderId, newStatus);
                return;
            }
            
            // 1. Guardar/Actualizar orden en PostgreSQL
            saveOrUpdateOrderInPostgres(orderId, newStatus, customerEmail);
            
            // 2. Enviar correo de notificación
            if (customerEmail != null && !customerEmail.isEmpty()) {
                emailService.sendStatusChangeEmail(customerEmail, orderId, newStatus);
                logger.info("Status change email sent for order: {}", orderId);
            }
            
            // 3. Si status = PAGADO, guardar en envios
            if ("PAGADO".equalsIgnoreCase(newStatus)) {
                logger.info("Order {} marked as PAGADO, creating envio", orderId);
                
                if (customerEmail != null && !customerEmail.isEmpty()) {
                    envioService.createEnvio(orderId, customerEmail);
                } else {
                    logger.warn("Cannot create envio for order {} - missing customer email", orderId);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error processing order_status_changed_events: {}", e.getMessage(), e);
        }
    }
    
    private void saveOrUpdateOrderInPostgres(String orderId, String newStatus, String customerEmail) {
        try {
            // Buscar si ya existe la orden en PostgreSQL
            Optional<Order> existingOrder = orderRepository.findById(orderId);
            
            if (existingOrder.isPresent()) {
                // Actualizar orden existente
                Order order = existingOrder.get();
                order.setStatus(newStatus);
                if (customerEmail != null) {
                    order.setCustomerEmail(customerEmail);
                }
                orderRepository.save(order);
                logger.info("Updated order {} in PostgreSQL with status: {}", orderId, newStatus);
            } else {
                // Obtener datos completos de la orden desde OrderService
                logger.info("Order {} not found in PostgreSQL, fetching from OrderService", orderId);
                fetchAndSaveOrderFromMongoService(orderId, newStatus);
            }
            
        } catch (Exception e) {
            logger.error("Failed to save/update order {} in PostgreSQL: {}", orderId, e.getMessage(), e);
        }
    }
    
    private void fetchAndSaveOrderFromMongoService(String orderId, String newStatus) {
        try {
            String orderServiceUrl = System.getenv().getOrDefault(
                "ORDER_SERVICE_URL", 
                "http://apigateway:8080/ordenes"
            );
            
            String url = orderServiceUrl + "/" + orderId;
            logger.info("Fetching order from: {}", url);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && response.get("data") != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> orderData = (Map<String, Object>) response.get("data");
                
                Order order = new Order();
                order.setId((String) orderData.get("id"));
                order.setUserId(((Number) orderData.get("userId")).longValue());
                order.setProductId((String) orderData.get("productId"));
                order.setTotalAmount(((Number) orderData.get("totalAmount")).doubleValue());
                order.setStatus(newStatus);
                order.setCustomerEmail((String) orderData.get("customerEmail"));
                
                orderRepository.save(order);
                logger.info("Saved new order {} in PostgreSQL from OrderService", orderId);
            } else {
                logger.warn("No order data received from OrderService for orderId: {}", orderId);
            }
            
        } catch (Exception e) {
            logger.error("Failed to fetch order from OrderService: {}", e.getMessage(), e);
        }
    }
}
