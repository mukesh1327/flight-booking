package com.cloudxplorer.authservice.model;

public class ErrorResponse {
    private String timestamp;
    private String path;
    private String code;
    private String message;
    private String correlationId;

    public ErrorResponse() {
    }

    public ErrorResponse(String timestamp, String path, String code, String message, String correlationId) {
        this.timestamp = timestamp;
        this.path = path;
        this.code = code;
        this.message = message;
        this.correlationId = correlationId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
}
