package com.example.aquatech;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;


public class RequestLogAdapter extends RecyclerView.Adapter<RequestLogAdapter.ViewHolder> {

    private List<RequestLogModel> logList;

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

        // Ticket ID
        holder.tvTicketID.setText(item.getTicketId());

        // Customer ID (use the new field)
        holder.tvCustomerID.setText(item.getCustomerId());

        // Status
        String status = item.getStatus();
        holder.tvStatus.setText(status);

        // Set status color and background
        if ("Completed".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"));
            holder.tvStatus.setBackgroundResource(R.drawable.status_circle_green);
        } else if ("In Progress".equalsIgnoreCase(status) || "Ongoing".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(Color.parseColor("#FF9800"));
            holder.tvStatus.setBackgroundResource(R.drawable.status_circle_orange); // you need to create this
        } else {
            holder.tvStatus.setTextColor(Color.parseColor("#9E9E9E"));
            holder.tvStatus.setBackgroundResource(R.drawable.status_circle_gray);   // you need to create this
        }

        // Optionally display technician name in the SRO field (or any other field)
        holder.tvSRO.setText(item.getTechName());
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