package com.example.aquatech;

public class HistoryItem {
    private String ticketId;
    private String date;
    private String itemsSummary; // Halimbawa: "Installation Kit, Filter Preventive"
    private String totalPrice;   // Halimbawa: "₱ 3,350"
    private String status;       // COMPLETED

    public HistoryItem(String ticketId, String date, String itemsSummary, String totalPrice, String status) {
        this.ticketId = ticketId;
        this.date = date;
        this.itemsSummary = itemsSummary;
        this.totalPrice = totalPrice;
        this.status = status;
    }

    public String getTicketId() { return ticketId; }
    public String getDate() { return date; }
    public String getItemsSummary() { return itemsSummary; }
    public String getTotalPrice() { return totalPrice; }
    public String getStatus() { return status; }
}
