package com.example.aquatech;

public class AdminLogModel {
    private String message;
    private long timestamp;

    public AdminLogModel() {}

    public AdminLogModel(String message, long timestamp) {
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
}
