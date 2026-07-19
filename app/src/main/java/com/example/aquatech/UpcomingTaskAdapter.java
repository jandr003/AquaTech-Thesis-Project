package com.example.aquatech;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
        
        // 1. Customer Name
        String name = ds.child("customerName").getValue(String.class);
        holder.tvName.setText(name != null ? name : "Unknown");

        // 2. Ticket ID (Top Right)
        String tId = ds.child("ticketId").getValue(String.class);
        holder.tvTicketId.setText("#" + (tId != null ? tId : "---"));

        // 3. Preferred Time
        String time = ds.child("timeRange").getValue(String.class);
        if (time == null || time.isEmpty()) {
            String start = ds.child("startTime").getValue(String.class);
            String end = ds.child("endTime").getValue(String.class);
            if (start != null && end != null) time = start + " - " + end;
            else time = "Not Specified";
        }
        holder.tvTime.setText(time);

        // 4. Address
        String addr = ds.child("address").getValue(String.class);
        holder.tvAddress.setText(addr != null ? addr : "No Address");

        // 5. 📍 REAL-TIME SRO (Strict Fetching)
        String sro = ds.child("referenceNo").getValue(String.class);
        if (sro == null) sro = ds.child("sroNumber").getValue(String.class);

        if (sro != null && !sro.isEmpty()) {
            holder.tvSro.setText(sro);
        } else {
            holder.tvSro.setText("No SRO Attached");
        }

        // 6. CATEGORY LOGIC
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

        // 7. DATE LOGIC
        Long timestamp = ds.child("assignedTimestamp").getValue(Long.class);
        if (timestamp != null) {
            Date date = new Date(timestamp);
            SimpleDateFormat dayF = new SimpleDateFormat("dd", Locale.getDefault());
            SimpleDateFormat monF = new SimpleDateFormat("MMM", Locale.getDefault());
            holder.tvDateStatus.setText(dayF.format(date) + "\n" + monF.format(date).toUpperCase());
        } else {
            holder.tvDateStatus.setText("NOW\n---");
        }

        holder.btnAccept.setOnClickListener(v -> {
            if (listener != null) listener.onAcceptClick(ds);
        });

        holder.btnMessage.setOnClickListener(v -> {
            if (listener != null) listener.onMessageClick(ds);
        });
    }

    private boolean hasQty(DataSnapshot ds, String key) {
        Object val = ds.child(key).getValue();
        return val instanceof Number && ((Number) val).intValue() > 0;
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvTicketId, tvServiceType, tvTime, tvAddress, tvSro, tvDateStatus;
        Button btnAccept;
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
