package com.microservices.brokermessage.dto;

public class StepResult {

    private String status = "PENDING";  // PENDING, SUCCESS, FAILED
    private String message = "";
    private Object data;

    public StepResult() {}

    public StepResult(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public StepResult(String status, String message, Object data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
}
