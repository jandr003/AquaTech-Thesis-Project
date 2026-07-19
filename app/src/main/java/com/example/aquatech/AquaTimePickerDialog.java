package com.example.aquatech;

import androidx.fragment.app.FragmentManager;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.database.FirebaseDatabase;
import java.util.Calendar;

public class AquaTimePickerDialog {

    public interface OnTimePicked {
        void onPicked(String startTime, String endTime);
    }

    private static final int OFFICE_START_HOUR = 8; // 8 AM
    private static final int OFFICE_END_HOUR = 17; // 5 PM

    public static void showStart(FragmentManager fm, OnTimePicked callback) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (hour < OFFICE_START_HOUR) hour = OFFICE_START_HOUR;
        if (hour >= OFFICE_END_HOUR) hour = OFFICE_END_HOUR - 1;

        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(hour)
                .setMinute(0)
                .setTitleText("Select Start Time")
                .setTheme(R.style.CustomTimePicker)
                .build();

        picker.addOnPositiveButtonClickListener(view -> {
            int selectedHour = picker.getHour();
            int selectedMinute = picker.getMinute();

            if (selectedHour < OFFICE_START_HOUR || selectedHour >= OFFICE_END_HOUR) {
                // Show error or just reset to a valid time
                String message = "Please select a time between 8:00 AM and 5:00 PM.";
                // You can show a Toast here if you have context
                return; // Or reset the time
            }

            String startTime = formatTime(selectedHour, selectedMinute);

            Calendar endCalendar = Calendar.getInstance();
            endCalendar.set(Calendar.HOUR_OF_DAY, selectedHour);
            endCalendar.set(Calendar.MINUTE, selectedMinute);
            endCalendar.add(Calendar.MINUTE, 30);

            if (endCalendar.get(Calendar.HOUR_OF_DAY) >= OFFICE_END_HOUR) {
                endCalendar.set(Calendar.HOUR_OF_DAY, OFFICE_END_HOUR);
                endCalendar.set(Calendar.MINUTE, 0);
            }
            String endTime = formatTime(endCalendar.get(Calendar.HOUR_OF_DAY), endCalendar.get(Calendar.MINUTE));

            callback.onPicked(startTime, endTime);
        });
        picker.show(fm, "start_time_picker");
    }

    public static void showEnd(FragmentManager fm, final String startTimeStr, OnTimePicked callback) {
        // Parse start time to set minimum for end time
        Calendar startCal = Calendar.getInstance();
        if (startTimeStr != null && !startTimeStr.equals("---")) {
            try {
                String[] parts = startTimeStr.replace(" AM", "").replace(" PM", "").split(":");
                int startHour = Integer.parseInt(parts[0]);
                int startMin = Integer.parseInt(parts[1]);
                if (startTimeStr.contains("PM") && startHour != 12) startHour += 12;
                if (startTimeStr.contains("AM") && startHour == 12) startHour = 0;
                startCal.set(Calendar.HOUR_OF_DAY, startHour);
                startCal.set(Calendar.MINUTE, startMin);
            }
            catch (Exception e) { /* default to now */ }
        }

        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(startCal.get(Calendar.HOUR_OF_DAY))
                .setMinute(startCal.get(Calendar.MINUTE))
                .setTitleText("Select End Time")
                .setTheme(R.style.CustomTimePicker)
                .build();

        picker.addOnPositiveButtonClickListener(view -> {
            int selectedHour = picker.getHour();
            int selectedMinute = picker.getMinute();

            if (selectedHour > OFFICE_END_HOUR || (selectedHour == OFFICE_END_HOUR && selectedMinute > 0)) {
                // Reset to 5:00 PM if out of bounds
                selectedHour = OFFICE_END_HOUR;
                selectedMinute = 0;
            }

            String endTime = formatTime(selectedHour, selectedMinute);
            callback.onPicked(null, endTime);
        });
        picker.show(fm, "end_time_picker");
    }

    private static String formatTime(int hour, int minute) {
        String amPm = hour >= 12 ? "PM" : "AM";
        int displayHour = hour % 12;
        if (displayHour == 0) displayHour = 12;
        return String.format("%02d:%02d %s", displayHour, minute, amPm);
    }
}
