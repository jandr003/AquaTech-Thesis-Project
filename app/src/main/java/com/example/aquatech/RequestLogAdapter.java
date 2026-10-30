package com.example.aquatech;

import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class RequestLogAdapter extends RecyclerView.Adapter<RequestLogAdapter.ViewHolder> {

    private final List<RequestLogModel> logList;

    public RequestLogAdapter(List<RequestLogModel> logList) {
        this.logList = logList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_audit_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RequestLogModel item = logList.get(position);

        String ticketId = item.getTicketId();
        if (ticketId != null && !ticketId.isEmpty()) {
            String upper = ticketId.toUpperCase().trim();
            if (upper.startsWith("SRO-")) {
                holder.tvTicketID.setText(upper);
            } else if (upper.startsWith("SRO")) {
                holder.tvTicketID.setText("SRO-" + upper.substring(3));
            } else {
                try {
                    long num = Long.parseLong(upper.replaceAll("[^0-9]", ""));
                    holder.tvTicketID.setText(String.format(Locale.US, "SRO-%08d", num));
                } catch (Exception e) {
                    holder.tvTicketID.setText("SRO-" + upper);
                }
            }
        } else {
            holder.tvTicketID.setText("SRO-00002346");
        }

        holder.tvCustomerID.setText(item.getCustomerId() != null && !item.getCustomerId().isEmpty() ? item.getCustomerId() : "ASC-C001");
        holder.tvSRO.setText(item.getTechName() != null && !item.getTechName().isEmpty() ? item.getTechName() : "Sep 30, 2025");

        String status = item.getStatus();
        if (status == null || status.isEmpty()) status = "Completed";
        holder.tvStatus.setText(status.toUpperCase());

        if ("Completed".equalsIgnoreCase(status) || "Resolved".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(Color.WHITE);
            holder.tvStatus.setBackgroundResource(R.drawable.bg_solid_green_pill);
        } else if ("Cancelled".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(Color.WHITE);
            holder.tvStatus.setBackgroundResource(R.drawable.bg_solid_red_pill);
        } else if ("In Progress".equalsIgnoreCase(status) || "Ongoing".equalsIgnoreCase(status) || "Assigned".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(Color.WHITE);
            holder.tvStatus.setBackgroundResource(R.drawable.bg_solid_blue_pill);
        } else {
            holder.tvStatus.setTextColor(Color.WHITE);
            holder.tvStatus.setBackgroundResource(R.drawable.bg_solid_amber_pill);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), CustomerConcernDetailsActivity.class);
            intent.putExtra("TICKET_ID", item.getTicketId());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return logList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTicketID, tvSRO, tvCustomerID, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTicketID = itemView.findViewById(R.id.auditTicketID);
            tvSRO = itemView.findViewById(R.id.auditUnitSRO);
            tvCustomerID = itemView.findViewById(R.id.auditCustomerID);
            tvStatus = itemView.findViewById(R.id.auditStatus);
        }
    }
}
