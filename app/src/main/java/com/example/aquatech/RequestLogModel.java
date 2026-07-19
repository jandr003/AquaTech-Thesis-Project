package com.example.aquatech;

public class RequestLogModel {
    private String ticketId;
    private String customerName;
    private String customerId;      // NEW FIELD
    private String techName;
    private String techId;
    private String status;
    private String serviceType;
    private Long timestamp;
    private Long assignedTime;
    private Long completedTime;
    private Float rating;

    // Updated constructor
    public RequestLogModel(String ticketId, String customerName, String customerId, String techName, String techId,
                           String status, String serviceType, Long timestamp,
                           Long assignedTime, Long completedTime, Float rating) {
        this.ticketId = ticketId;
        this.customerName = customerName;
        this.customerId = customerId;
        this.techName = techName;
        this.techId = techId;
        this.status = status;
        this.serviceType = serviceType;
        this.timestamp = timestamp;
        this.assignedTime = assignedTime;
        this.completedTime = completedTime;
        this.rating = rating;
    }

    // Getters
    public String getTicketId() { return ticketId != null ? ticketId : "N/A"; }
    public String getCustomerName() { return customerName != null ? customerName : "N/A"; }
    public String getCustomerId() { return customerId != null ? customerId : "N/A"; }
    public String getTechName() { return techName != null ? techName : "Unassigned"; }
    public String getTechId() { return techId != null ? techId : "N/A"; }
    public String getStatus() { return status != null ? status : "Pending"; }
    public String getServiceType() { return serviceType != null ? serviceType : "General"; }
    public Long getTimestamp() { return timestamp; }
    public Long getAssignedTime() { return assignedTime; }
    public Long getCompletedTime() { return completedTime; }
    public Float getRating() { return rating; }
}