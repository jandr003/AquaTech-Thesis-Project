package com.example.aquatech;

public class ServiceLogModel {
    private String sroNumber;
    private String ticketId;
    private String techName;
    private String techUid;          // ✅ technician UID (para sa reject)
    private String techRole;
    private String dateTime;
    private String techPhone;
    private String customerName;      // ✅ customer name
    private String customerPhone;
    private String address;
    private String techId;
    private String status;
    private String userId;            // 🔑 Added for Chat functionality
    private double latitude;
    private double longitude;
    private double techLat;
    private double techLng;
    private double totalAmount;

    // ✅ NEW FIELDS for completed requests
    private String remarks;
    private String proofUrl;
    private String feedback;
    private String preferredTime;

    // Optional, but used in CompletedRequestsActivity
    private String serviceTypeLabel;
    private String completionTime;

    // Constructors
    public ServiceLogModel() {}

    public ServiceLogModel(String sroNumber, String techName, String techRole, String status, String customerPhone) {
        this.sroNumber = sroNumber;
        this.techName = techName;
        this.techRole = techRole;
        this.status = status;
        this.customerPhone = customerPhone;
    }

    // Constructor used in CompletedRequestsActivity (maps customerName → techName, serviceTypeLabel → techRole, completionTime → dateTime)
    public ServiceLogModel(String sroNumber, String techName, String techRole, String dateTime, String address, double latitude, double longitude) {
        this.sroNumber = sroNumber;
        this.techName = techName;
        this.techRole = techRole;
        this.dateTime = dateTime;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters and Setters
    public String getTicketId() { return ticketId != null ? ticketId : sroNumber; }
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }

    public String getSroNumber() { return sroNumber; }
    public void setSroNumber(String sroNumber) { this.sroNumber = sroNumber; }

    public String getTechName() { return techName; }
    public void setTechName(String techName) { this.techName = techName; }

    public String getTechUid() { return techUid; }
    public void setTechUid(String techUid) { this.techUid = techUid; }

    public String getTechRole() { return techRole; }
    public void setTechRole(String techRole) { this.techRole = techRole; }

    public String getDateTime() { return dateTime; }
    public void setDateTime(String dateTime) { this.dateTime = dateTime; }

    public String getTechPhone() { return techPhone; }
    public void setTechPhone(String techPhone) { this.techPhone = techPhone; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getTechId() { return techId; }
    public void setTechId(String techId) { this.techId = techId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public double getTechLat() { return techLat; }
    public void setTechLat(double techLat) { this.techLat = techLat; }

    public double getTechLng() { return techLng; }
    public void setTechLng(double techLng) { this.techLng = techLng; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    // ✅ NEW GETTERS AND SETTERS
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getProofUrl() { return proofUrl; }
    public void setProofUrl(String proofUrl) { this.proofUrl = proofUrl; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public String getPreferredTime() { return preferredTime; }
    public void setPreferredTime(String preferredTime) { this.preferredTime = preferredTime; }

    // Optional, used in adapter display
    public String getCompletionTime() { return completionTime != null ? completionTime : dateTime; }
    public void setCompletionTime(String completionTime) { this.completionTime = completionTime; }
}