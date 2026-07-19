package com.example.aquatech;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {

    private List<CalendarItem> calendarList;
    private OnDateClickListener listener;

    public interface OnDateClickListener {
        void onDateClick(CalendarItem item, int position);
    }

    public CalendarAdapter(List<CalendarItem> calendarList, OnDateClickListener listener) {
        this.calendarList = calendarList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        return new CalendarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        CalendarItem item = calendarList.get(position);
        Calendar cal = Calendar.getInstance();
        cal.setTime(item.getDate());

        // Set Day Name (Mon, Tue...) and Date Number
        holder.tvDayName.setText(new SimpleDateFormat("EEE", Locale.getDefault()).format(item.getDate()));
        holder.tvDateNumber.setText(String.valueOf(cal.get(Calendar.DAY_OF_MONTH)));

        // Handle Status Dot Color
        int dotColor;
        switch (item.getStatus()) {
            case "COMPLETED": dotColor = Color.parseColor("#00C1A4"); break;
            case "IN_PROGRESS": dotColor = Color.parseColor("#2196F3"); break;
            case "OPEN": dotColor = Color.parseColor("#FF5252"); break;
            default: dotColor = Color.TRANSPARENT; break;
        }
        holder.vStatusDot.setBackgroundColor(dotColor);

        // Handle Selection Style (Blue Pill)
        if (item.isSelected()) {
            holder.cvDay.setCardBackgroundColor(Color.parseColor("#2196F3"));
            holder.tvDayName.setTextColor(Color.WHITE);
            holder.tvDateNumber.setTextColor(Color.WHITE);
            holder.vStatusDot.setBackgroundColor(Color.WHITE); // Make dot white on blue bg
            holder.cvDay.setCardElevation(6f);
        } else {
            holder.cvDay.setCardBackgroundColor(Color.TRANSPARENT);
            holder.tvDayName.setTextColor(Color.parseColor("#888888"));
            holder.tvDateNumber.setTextColor(Color.parseColor("#1A3C40"));
            holder.cvDay.setCardElevation(0f);
        }

        holder.itemView.setOnClickListener(v -> listener.onDateClick(item, position));
    }

    @Override
    public int getItemCount() {
        return calendarList.size();
    }

    public static class CalendarViewHolder extends RecyclerView.ViewHolder {
        CardView cvDay;
        TextView tvDayName, tvDateNumber;
        View vStatusDot;

        public CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            cvDay = itemView.findViewById(R.id.cvDay);
            tvDayName = itemView.findViewById(R.id.tvDayName);
            tvDateNumber = itemView.findViewById(R.id.tvDateNumber);
            vStatusDot = itemView.findViewById(R.id.vStatusDot);
        }
    }
}
