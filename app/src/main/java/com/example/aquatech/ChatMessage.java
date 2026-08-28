package com.example.aquatech;

public class ChatMessage {
    private String text;
    private String senderId;
    private long timestamp;
    private boolean isSystem;

    private boolean isUser;

    private boolean isFile;
    private String fileName;
    private String fileSize;
    private String fileUrl;

    // Receipt fields
    private boolean isReceipt;
    private String ticketId;

    public ChatMessage() {
        // Required for Firebase
    }

    public ChatMessage(String text, String senderId, boolean isSystem) {
        this.text = text;
        this.senderId = senderId;
        this.isSystem = isSystem;
        this.timestamp = System.currentTimeMillis();
    }

    public ChatMessage(String text, boolean isUser, boolean isSystem) {
        this.text = text;
        this.isUser = isUser;
        this.isSystem = isSystem;
        this.senderId = isUser ? "user" : "aquaBuddy";
        this.timestamp = System.currentTimeMillis();
    }

    public ChatMessage(String text, String fileName, String fileSize, String fileUrl, String senderId) {
        this.text = text;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileUrl = fileUrl;
        this.senderId = senderId;
        this.isFile = true;
        this.timestamp = System.currentTimeMillis();
    }

    public ChatMessage(String text, String fileName, String fileSize, String fileUrl) {
        this.text = text;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileUrl = fileUrl;
        this.isFile = true;
        this.senderId = "";
        this.timestamp = System.currentTimeMillis();
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isSystem() { return isSystem; }
    public void setSystem(boolean system) { isSystem = system; }

    public boolean isUser() { return isUser; }
    public void setUser(boolean user) { isUser = user; }

    public boolean isFile() { return isFile; }
    public void setFile(boolean file) { isFile = file; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileSize() { return fileSize; }
    public void setFileSize(String fileSize) { this.fileSize = fileSize; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public boolean isReceipt() { return isReceipt; }
    public void setReceipt(boolean receipt) { isReceipt = receipt; }

    public String getTicketId() { return ticketId; }
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }
}