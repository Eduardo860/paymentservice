package com.microservices.brokermessage.dto;

/**
 * Context object passed through the Chain of Responsibility.
 * Carries all state needed by handlers without coupling them to specific entities.
 */
public class RetryContext {

    private String jobId;
    private String entityId;
    private String action;           // CREATE, UPDATE, DELETE
    private String requestDataJson;  // original request payload as JSON string
    private boolean failed = false;
    private String errorMessage;
    private TrackingPayload tracking = new TrackingPayload();

    public RetryContext() {}

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getRequestDataJson() { return requestDataJson; }
    public void setRequestDataJson(String requestDataJson) { this.requestDataJson = requestDataJson; }

    public boolean isFailed() { return failed; }
    public void setFailed(boolean failed) { this.failed = failed; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public TrackingPayload getTracking() { return tracking; }
    public void setTracking(TrackingPayload tracking) { this.tracking = tracking; }
}
