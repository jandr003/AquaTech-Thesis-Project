package com.example.aquatech;

import java.util.Date;

public class CalendarItem {
    private Date date;
    private boolean isSelected;
    private String status; // COMPLETED, IN_PROGRESS, OPEN, NONE

    public CalendarItem(Date date, boolean isSelected, String status) {
        this.date = date;
        this.isSelected = isSelected;
        this.status = status;
    }

    public Date getDate() { return date; }
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
    public String getStatus() { return status; }
}
