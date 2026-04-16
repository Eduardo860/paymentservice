package com.microservices.brokermessage.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class KafkaMessageDto {

    private String entityId;
    private String action;  // CREATE, UPDATE, DELETE
    private Map<String, Object> requestData;
    private String errorMessage;

    public KafkaMessageDto() {}

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Map<String, Object> getRequestData() { return requestData; }
    public void setRequestData(Map<String, Object> requestData) { this.requestData = requestData; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    @Override
    public String toString() {
        return "KafkaMessageDto{entityId='" + entityId + "', action='" + action + "'}";
    }
}
