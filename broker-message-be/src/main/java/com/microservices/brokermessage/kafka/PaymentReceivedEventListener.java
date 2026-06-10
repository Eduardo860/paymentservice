package com.microservices.brokermessage.kafka;

import com.microservices.brokermessage.service.EmailService;
import com.microservices.brokermessage.service.EnvioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class PaymentReceivedEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentReceivedEventListener.class);
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private EnvioService envioService;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${ORDER_SERVICE_URL:http://apigateway:8080/orders-api}")
    private String orderServiceUrl;
    
    @Value("${PAYMENT_SERVICE_URL:http://apigateway:8080/pagos-api}")
    private String paymentServiceUrl;
    
    @KafkaListener(
        topics = "payment_received_events", 
        groupId = "payment-group",
        containerFactory = "eventKafkaListenerContainerFactory"
    )
    public void consumePaymentReceivedEvent(Map<String, Object> event) {
        logger.info("Consumed payment_received_events: {}", event);
        
        try {
            String orderId = (String) event.get("orderId");
            Double amount = event.get("amount") != null ? ((Number) event.get("amount")).doubleValue() : 0.0;
            String customerEmail = (String) event.get("customerEmail");
            
            // 1. Enviar correo de pago recibido
            if (customerEmail != null && !customerEmail.isEmpty()) {
                emailService.sendPaymentReceivedEmail(customerEmail, orderId, amount);
                logger.info("Payment received email sent for order: {}", orderId);
            }
            
            // 2. Validar si la orden está completamente pagada
            boolean isFullyPaid = validateOrderFullyPaid(orderId);
            
            if (isFullyPaid) {
                logger.info("Order {} is fully paid, creating envio", orderId);
                
                // Obtener detalles de la orden para el email
                Map<String, Object> orderResponse = restTemplate.getForObject(
                    orderServiceUrl + "/" + orderId,
                    Map.class
                );
                
                if (orderResponse != null && orderResponse.containsKey("data")) {
                    Map<String, Object> orderData = (Map<String, Object>) orderResponse.get("data");
                    String orderEmail = (String) orderData.get("customerEmail");
                    
                    if (orderEmail != null && !orderEmail.isEmpty()) {
                        // 3. Guardar en tabla envios
                        envioService.createEnvio(orderId, orderEmail);
                    }
                }
            } else {
                logger.info("Order {} is not fully paid yet", orderId);
            }
            
        } catch (Exception e) {
            logger.error("Error processing payment_received_events: {}", e.getMessage(), e);
        }
    }
    
    private boolean validateOrderFullyPaid(String orderId) {
        try {
            // Obtener la orden
            Map<String, Object> orderResponse = restTemplate.getForObject(
                orderServiceUrl + "/" + orderId,
                Map.class
            );
            
            if (orderResponse == null || !orderResponse.containsKey("data")) {
                logger.warn("Order not found: {}", orderId);
                return false;
            }
            
            Map<String, Object> orderData = (Map<String, Object>) orderResponse.get("data");
            Double totalAmount = orderData.get("totalAmount") != null ? 
                ((Number) orderData.get("totalAmount")).doubleValue() : 0.0;
            
            // Obtener pagos de la orden
            Map<String, Object> paymentsResponse = restTemplate.getForObject(
                paymentServiceUrl + "/orden/" + orderId,
                Map.class
            );
            
            if (paymentsResponse == null || !paymentsResponse.containsKey("data")) {
                return false;
            }
            
            List<Map<String, Object>> payments = 
                (List<Map<String, Object>>) paymentsResponse.get("data");
            
            // Sumar pagos completados
            double totalPaid = payments.stream()
                .filter(p -> "COMPLETED".equals(p.get("status")))
                .mapToDouble(p -> p.get("amount") != null ? 
                    ((Number) p.get("amount")).doubleValue() : 0.0)
                .sum();
            
            logger.info("Order {}: totalAmount={}, totalPaid={}", orderId, totalAmount, totalPaid);
            
            return totalPaid >= totalAmount;
            
        } catch (Exception e) {
            logger.error("Error validating order payment: {}", e.getMessage());
            return false;
        }
    }
}
