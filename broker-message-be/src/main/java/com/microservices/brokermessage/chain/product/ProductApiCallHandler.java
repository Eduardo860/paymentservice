package com.microservices.brokermessage.chain.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.brokermessage.chain.AbstractRetryHandler;
import com.microservices.brokermessage.dto.RetryContext;
import com.microservices.brokermessage.dto.StepResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Step 1 — Calls the product service API endpoint based on the action type.
 */
@Component
public class ProductApiCallHandler extends AbstractRetryHandler {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${services.product-url}")
    private String productServiceUrl;

    @Override
    public void handle(RetryContext context) {
        logger.info("ProductApiCallHandler: action={} entityId={}", context.getAction(), context.getEntityId());
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> response;
            String action = context.getAction();

            if ("CREATE".equalsIgnoreCase(action)) {
                HttpEntity<String> request = new HttpEntity<>(context.getRequestDataJson(), headers);
                response = restTemplate.exchange(productServiceUrl, HttpMethod.POST, request, Map.class);

            } else if ("UPDATE".equalsIgnoreCase(action)) {
                String url = productServiceUrl + "/" + context.getEntityId();
                HttpEntity<String> request = new HttpEntity<>(context.getRequestDataJson(), headers);
                response = restTemplate.exchange(url, HttpMethod.PUT, request, Map.class);

            } else if ("DELETE".equalsIgnoreCase(action)) {
                String url = productServiceUrl + "/" + context.getEntityId();
                response = restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), Map.class);

            } else {
                throw new IllegalArgumentException("Unknown action: " + action);
            }

            StepResult result = new StepResult("SUCCESS", "API call successful", response.getBody());
            context.getTracking().setData(result);
            logger.info("ProductApiCallHandler: success action={} entityId={}", action, context.getEntityId());

        } catch (Exception e) {
            logger.error("ProductApiCallHandler: failed action={} error={}", context.getAction(), e.getMessage());
            context.setFailed(true);
            context.setErrorMessage(e.getMessage());
            context.getTracking().setData(new StepResult("FAILED", e.getMessage()));
            return;
        }

        handleNext(context);
    }
}
