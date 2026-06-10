package com.microservices.brokermessage.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class InventoryUpdateEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(InventoryUpdateEventListener.class);
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${PRODUCT_SERVICE_URL:http://apigateway:8080/productos-api}")
    private String productServiceUrl;
    
    @KafkaListener(
        topics = "inventory_update_events", 
        groupId = "inventory-group",
        containerFactory = "eventKafkaListenerContainerFactory"
    )
    public void consumeInventoryUpdateEvent(Map<String, Object> event) {
        logger.info("Consumed inventory_update_events: {}", event);
        
        try {
            List<Map<String, Object>> products = (List<Map<String, Object>>) event.get("products");
            
            if (products == null || products.isEmpty()) {
                logger.warn("No products in inventory update event");
                return;
            }
            
            for (Map<String, Object> product : products) {
                String productId = (String) product.get("productId");
                Integer quantity = product.get("quantity") != null ? 
                    ((Number) product.get("quantity")).intValue() : 0;
                
                if (productId == null || quantity <= 0) {
                    logger.warn("Invalid product data: productId={}, quantity={}", productId, quantity);
                    continue;
                }
                
                // Llamar a productservice para reducir stock
                try {
                    Map<String, Integer> request = new HashMap<>();
                    request.put("quantity", quantity);
                    
                    restTemplate.put(
                        productServiceUrl + "/" + productId + "/reducir-stock",
                        request
                    );
                    
                    logger.info("Stock reduced for product {} by {}", productId, quantity);
                    
                } catch (Exception e) {
                    logger.error("Failed to reduce stock for product {}: {}", productId, e.getMessage());
                    // Aquí podrías guardar en retry_jobs si es necesario
                }
            }
            
        } catch (Exception e) {
            logger.error("Error processing inventory_update_events: {}", e.getMessage(), e);
        }
    }
}
