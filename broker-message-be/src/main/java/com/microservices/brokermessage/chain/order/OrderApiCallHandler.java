package com.microservices.brokermessage.chain.order;

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
 * Step 1 — Calls the order service API endpoint based on the action type.
 */
@Component
public class OrderApiCallHandler extends AbstractRetryHandler {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${services.order-url}")
    private String orderServiceUrl;

    @Override
    public void handle(RetryContext context) {
        logger.info("OrderApiCallHandler: action={} entityId={}", context.getAction(), context.getEntityId());
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> response;
            String action = context.getAction();

            if ("CREATE".equalsIgnoreCase(action)) {
                HttpEntity<String> request = new HttpEntity<>(context.getRequestDataJson(), headers);
                response = restTemplate.exchange(orderServiceUrl, HttpMethod.POST, request, Map.class);

            } else if ("UPDATE".equalsIgnoreCase(action)) {
                String url = orderServiceUrl + "/" + context.getEntityId() + "/status";
                HttpEntity<String> request = new HttpEntity<>(context.getRequestDataJson(), headers);
                response = restTemplate.exchange(url, HttpMethod.PUT, request, Map.class);

            } else if ("DELETE".equalsIgnoreCase(action)) {
                String url = orderServiceUrl + "/" + context.getEntityId();
                response = restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), Map.class);

            } else {
                throw new IllegalArgumentException("Unknown action: " + action);
            }

            StepResult result = new StepResult("SUCCESS", "API call successful", response.getBody());
            context.getTracking().setData(result);
            logger.info("OrderApiCallHandler: success action={} entityId={}", action, context.getEntityId());

        } catch (Exception e) {
            logger.error("OrderApiCallHandler: failed action={} error={}", context.getAction(), e.getMessage());
            context.setFailed(true);
            context.setErrorMessage(e.getMessage());
            context.getTracking().setData(new StepResult("FAILED", e.getMessage()));
            return;
        }

        handleNext(context);
    }
}
