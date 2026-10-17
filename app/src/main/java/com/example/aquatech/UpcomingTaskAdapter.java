package com.example.aquatech;

import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UpcomingTaskAdapter extends RecyclerView.Adapter<UpcomingTaskAdapter.ViewHolder> {

    private List<DataSnapshot> taskList;
    private OnUpcomingClickListener listener;

    public interface OnUpcomingClickListener {
        void onAcceptClick(DataSnapshot snapshot);
        void onMessageClick(DataSnapshot snapshot);
    }

    public UpcomingTaskAdapter(List<DataSnapshot> taskList, OnUpcomingClickListener listener) {
        this.taskList = taskList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_upcoming_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DataSnapshot ds = taskList.get(position);

        String name = ds.child("customerName").getValue(String.class);
        holder.tvName.setText(name != null ? name : "Unknown");
        String tId = ds.child("ticketId").getValue(String.class);
        holder.tvTicketId.setText("#" + (tId != null ? tId : "---"));
        String time = ds.child("timeRange").getValue(String.class);
        if (time == null || time.isEmpty()) {
            String start = ds.child("startTime").getValue(String.class);
            String end = ds.child("endTime").getValue(String.class);
            if (start != null && end != null) time = start + " - " + end;
            else time = "Not Specified";
        }
        holder.tvTime.setText(time);

        String addr = ds.child("address").getValue(String.class);
        holder.tvAddress.setText(addr != null ? addr : "No Address");
        String sro = ds.child("referenceNo").getValue(String.class);
        if (sro == null) sro = ds.child("sroNumber").getValue(String.class);

        if (sro != null && !sro.isEmpty()) {
            holder.tvSro.setText(sro);
        } else {
            holder.tvSro.setText("No SRO Attached");
        }

        List<String> categories = new ArrayList<>();
        if (hasQty(ds, "qty_cbc") || hasQty(ds, "qty_sediment") || hasQty(ds, "qty_inline") || hasQty(ds, "qty_smsf1") || hasQty(ds, "qty_smsf10")) {
            categories.add("Filter Preventive");
        }
        if (hasQty(ds, "qty_aquatal") || hasQty(ds, "qty_uvlamp") || hasQty(ds, "qty_touchpanel") || hasQty(ds, "qty_pbcboard")) {
            categories.add("Other Parts");
        }
        if (hasQty(ds, "qty_wayvalve")) {
            categories.add("Installation Kit");
        }

        StringBuilder categoryStr = new StringBuilder();
        for (int i = 0; i < categories.size(); i++) {
            categoryStr.append(categories.get(i));
            if (i < categories.size() - 2) categoryStr.append(", ");
            else if (i == categories.size() - 2) categoryStr.append(" and ");
        }

        if (categoryStr.length() > 0) holder.tvServiceType.setText(categoryStr.toString());
        else holder.tvServiceType.setText("General Service");

        Long timestamp = ds.child("assignedTimestamp").getValue(Long.class);
        if (timestamp != null) {
            Date date = new Date(timestamp);
            holder.tvDateStatus.setText(toVerticalDateString(date));
        } else {
            holder.tvDateStatus.setText("N\nO\nW");
        }

        holder.btnAccept.setOnClickListener(v -> {
            if (listener != null) listener.onAcceptClick(ds);
        });

        holder.btnMessage.setOnClickListener(v -> {
            if (listener != null) listener.onMessageClick(ds);
        });
    }

    private CharSequence toVerticalDateString(Date d) {
        String day = "26";
        String month = "FEB";
        if (d != null) {
            SimpleDateFormat df = new SimpleDateFormat("dd", Locale.getDefault());
            SimpleDateFormat mf = new SimpleDateFormat("MMM", Locale.getDefault());
            day = df.format(d);
            month = mf.format(d).toUpperCase(Locale.ROOT);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(day);
        for (int i = 0; i < month.length(); i++) {
            sb.append("\n");
            sb.append(month.charAt(i));
        }

        String fullText = sb.toString();
        SpannableString spannable = new SpannableString(fullText);

        spannable.setSpan(new AbsoluteSizeSpan(20, true), 0, day.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, day.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        if (fullText.length() > day.length()) {
            spannable.setSpan(new AbsoluteSizeSpan(12, true), day.length() + 1, fullText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new StyleSpan(Typeface.BOLD), day.length() + 1, fullText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return spannable;
    }

    private boolean hasQty(DataSnapshot ds, String key) {
        Object val = ds.child(key).getValue();
        return val instanceof Number && ((Number) val).intValue() > 0;
    }

    @Override
    public int getItemCount() {
        return taskList != null ? taskList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvTicketId, tvServiceType, tvTime, tvAddress, tvSro, tvDateStatus;
        View btnAccept;
        ImageView btnMessage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvUpcomingCustomerName);
            tvTicketId = itemView.findViewById(R.id.tvUpcomingTicketId);
            tvServiceType = itemView.findViewById(R.id.tvUpcomingServiceType);
            tvTime = itemView.findViewById(R.id.tvUpcomingTime);
            tvAddress = itemView.findViewById(R.id.tvUpcomingAddress);
            tvSro = itemView.findViewById(R.id.tvUpcomingSro);
            tvDateStatus = itemView.findViewById(R.id.statusOpenGreen);
            btnAccept = itemView.findViewById(R.id.btnAcceptUpcoming);
            btnMessage = itemView.findViewById(R.id.btnChatUpcoming);
        }
    }
}
