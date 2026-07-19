package com.example.aquatech;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Map;

public class AuditLogAdapter extends RecyclerView.Adapter<AuditLogAdapter.ViewHolder> {

    private List<Map<String, String>> logList;

    public AuditLogAdapter(List<Map<String, String>> logList) {
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
        Map<String, String> item = logList.get(position);
        
        holder.tvTicketID.setText(item.getOrDefault("ticketID", "N/A"));
        holder.tvSRO.setText(item.getOrDefault("sroNumber", "N/A"));
        holder.tvCustomerID.setText(item.getOrDefault("customerID", "N/A"));
        
        String status = item.getOrDefault("status", "Pending");
        holder.tvStatus.setText(status.toUpperCase());

        if ("Completed".equalsIgnoreCase(status) || "Resolved".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(Color.parseColor("#8BC34A"));
            holder.tvStatus.setBackgroundResource(R.drawable.status_circle_green);
        } else {
            holder.tvStatus.setTextColor(Color.parseColor("#FF9800"));
            holder.tvStatus.setBackgroundResource(R.drawable.status_circle_green); // Pwedeng palitan ng orange drawable kung meron
        }
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
