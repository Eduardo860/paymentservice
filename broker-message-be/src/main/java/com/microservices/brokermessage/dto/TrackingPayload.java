package com.microservices.brokermessage.dto;

/**
 * Tracks the result of each step in the Chain of Responsibility.
 * Mirrors the required tracking payload format:
 * {
 *   "data":             { "status": "SUCCESS", "message": "" },
 *   "sendEmail":        { "status": "SUCCESS", "message": "" },
 *   "updateRetryJobs":  { "status": "SUCCESS", "message": "" }
 * }
 */
public class TrackingPayload {

    private StepResult data = new StepResult();
    private StepResult sendEmail = new StepResult();
    private StepResult updateRetryJobs = new StepResult();

    public TrackingPayload() {}

    public StepResult getData() { return data; }
    public void setData(StepResult data) { this.data = data; }

    public StepResult getSendEmail() { return sendEmail; }
    public void setSendEmail(StepResult sendEmail) { this.sendEmail = sendEmail; }

    public StepResult getUpdateRetryJobs() { return updateRetryJobs; }
    public void setUpdateRetryJobs(StepResult updateRetryJobs) { this.updateRetryJobs = updateRetryJobs; }
}
