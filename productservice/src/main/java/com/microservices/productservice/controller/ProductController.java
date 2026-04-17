package com.microservices.productservice.controller;

import com.microservices.productservice.model.Product;
import com.microservices.productservice.repository.ProductRepository;
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
@RequestMapping("/productos-api")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    private ProductRepository productRepository;

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
            logger.error("Failed to create product, publishing to Kafka: {}", e.getMessage());
            publishToKafka("new", "CREATE", product);
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
}
