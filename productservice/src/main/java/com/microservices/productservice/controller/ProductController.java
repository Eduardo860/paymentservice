package com.microservices.productservice.controller;

import com.microservices.productservice.model.Product;
import com.microservices.productservice.repository.ProductRepository;
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
@RequestMapping("/productos-api")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${ORDER_SERVICE_URL:http://apigateway:8080/orders-api}")
    private String orderServiceUrl;

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
            kafkaTemplate.send("product_retry_jobs", msg);
            logger.info("Published to Kafka product_retry_jobs entityId={} action={}", entityId, action);
        } catch (Exception e) {
            logger.error("Failed to publish to Kafka: {}", e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllProducts() {
        logger.info("Fetching all products");
        List<Product> products = productRepository.findAll();
        logger.info("Retrieved {} products", products.size());
        return ResponseEntity.ok(ok(products));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getProductById(@PathVariable String id) {
        logger.info("Fetching product with ID: {}", id);
        Optional<Product> product = productRepository.findById(id);
        if (product.isPresent()) {
            logger.info("Product found: {}", id);
            return ResponseEntity.ok(ok(product.get()));
        }
        logger.warn("Product not found: {}", id);
        return ResponseEntity.status(404).body(err(404, "Producto no encontrado: " + id));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createProduct(@RequestBody Product product) {
        logger.info("Creating new product: {}", product.getName());
        try {
            Product savedProduct = productRepository.save(product);
            logger.info("Product created successfully with ID: {}", savedProduct.getId());
            return ResponseEntity.ok(ok(savedProduct));
        } catch (Exception e) {
            logger.error("Failed to create product: {}", e.getMessage(), e);
            logger.info("Publishing to Kafka topic 'product_retry_jobs'...");
            try {
                publishToKafka("new", "CREATE", product);
                logger.info("Successfully published to Kafka");
            } catch (Exception kafkaEx) {
                logger.error("CRITICAL: Failed to publish to Kafka: {}", kafkaEx.getMessage(), kafkaEx);
            }
            return ResponseEntity.status(503).body(err(503, "Operación encolada para reintento: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateProduct(@PathVariable String id, @RequestBody Product product) {
        logger.info("Updating product with ID: {}", id);
        try {
            if (productRepository.existsById(id)) {
                product.setId(id);
                Product updatedProduct = productRepository.save(product);
                logger.info("Product updated successfully: {}", id);
                return ResponseEntity.ok(ok(updatedProduct));
            }
            logger.warn("Product not found for update: {}", id);
            return ResponseEntity.status(404).body(err(404, "Producto no encontrado: " + id));
        } catch (Exception e) {
            logger.error("Failed to update product {}, publishing to Kafka: {}", id, e.getMessage());
            publishToKafka(id, "UPDATE", product);
            return ResponseEntity.status(503).body(err(503, "Operación encolada para reintento: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteProduct(@PathVariable String id) {
        logger.info("Deleting product with ID: {}", id);
        try {
            if (productRepository.existsById(id)) {
                try {
                    Map<String, Object> response = restTemplate.getForObject(
                            orderServiceUrl + "/producto/" + id,
                            Map.class
                    );
                    if (response != null && response.containsKey("data") && Boolean.TRUE.equals(response.get("data"))) {
                        logger.warn("Cannot delete product {}, it is associated with an order", id);
                        return ResponseEntity.status(400).body(err(400, "El producto está asociado a una orden y no puede ser eliminado"));
                    }
                } catch (Exception e) {
                    logger.error("Error checking if product is in order: {}", e.getMessage());
                    return ResponseEntity.status(500).body(err(500, "Error verificando si el producto está en una orden: " + e.getMessage()));
                }

                productRepository.deleteById(id);
                logger.info("Product deleted successfully: {}", id);
                return ResponseEntity.ok(ok("Producto eliminado correctamente"));
            }
            logger.warn("Product not found for deletion: {}", id);
            return ResponseEntity.status(404).body(err(404, "Producto no encontrado: " + id));
        } catch (Exception e) {
            logger.error("Failed to delete product {}, publishing to Kafka: {}", id, e.getMessage());
            publishToKafka(id, "DELETE", null);
            return ResponseEntity.status(503).body(err(503, "Operación encolada para reintento: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/reducir-stock")
    public ResponseEntity<Map<String, Object>> reduceStock(@PathVariable String id, @RequestBody Map<String, Integer> request) {
        logger.info("Reducing stock for product: {}", id);
        try {
            Optional<Product> productOpt = productRepository.findById(id);
            if (!productOpt.isPresent()) {
                logger.warn("Product not found: {}", id);
                return ResponseEntity.status(404).body(err(404, "Producto no encontrado: " + id));
            }
            
            Product product = productOpt.get();
            Integer quantity = request.get("quantity");
            
            if (quantity == null || quantity <= 0) {
                return ResponseEntity.badRequest().body(err(400, "Cantidad inválida"));
            }
            
            if (product.getStock() < quantity) {
                logger.warn("Insufficient stock for product {}: available={}, requested={}", id, product.getStock(), quantity);
                return ResponseEntity.badRequest().body(err(400, "Stock insuficiente. Disponible: " + product.getStock()));
            }
            
            product.setStock(product.getStock() - quantity);
            Product updatedProduct = productRepository.save(product);
            
            logger.info("Stock reduced for product {}: new stock={}", id, updatedProduct.getStock());
            return ResponseEntity.ok(ok(updatedProduct));
            
        } catch (Exception e) {
            logger.error("Failed to reduce stock for product {}: {}", id, e.getMessage());
            return ResponseEntity.status(500).body(err(500, "Error al reducir stock: " + e.getMessage()));
        }
    }
}
