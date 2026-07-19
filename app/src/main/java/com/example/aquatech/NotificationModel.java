package com.example.aquatech;

public class NotificationModel {
    private String id;
    private String message;
    private long timestamp;
    private String type; // e.g., "PDF", "ASSIGNED", "MESSAGE", "COMPLETED", "CALL", "RESUBMIT"
    private String ticketId;
    private String senderName; // Added to support caller name in notifications
    private int iconResId = -1; // Default to -1

    public NotificationModel() {
    }

    public NotificationModel(String id, String message, long timestamp, String type, String ticketId) {
        this.id = id;
        this.message = message;
        this.timestamp = timestamp;
        this.type = type;
        this.ticketId = ticketId;
    }

    public NotificationModel(String id, String message, long timestamp, String type, String ticketId, String senderName) {
        this.id = id;
        this.message = message;
        this.timestamp = timestamp;
        this.type = type;
        this.ticketId = ticketId;
        this.senderName = senderName;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTicketId() { return ticketId; }
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    // Manual setter for icon
    public void setIconResId(int iconResId) { this.iconResId = iconResId; }

    // Helper to get relative time
    public String getTimeAgo() {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        if (diff < 60000) return "Just now";
        if (diff < 3600000) return (diff / 60000) + "m ago";
        if (diff < 86400000) return (diff / 3600000) + "h ago";
        return (diff / 86400000) + "d ago";
    }

    // Helper for icon (Check if manual override exists, else use type logic)
    public int getIconResId() {
        if (iconResId != -1) return iconResId;

        if (type == null) return R.drawable.asc_logooo;
        switch (type) {
            case "PDF": return R.drawable.robot_icon_new;
            case "ASSIGNED":
            case "COMPLETED": return R.drawable.maintenance_technician1;
            case "MESSAGE": return R.drawable.message_notification_1;
            case "CALL": return R.drawable.telephone_icon;
            case "RESUBMIT": return R.drawable.alert_icon; // o anumang icon na gusto mo
            default: return R.drawable.asc_logooo;
        }
    }
}
