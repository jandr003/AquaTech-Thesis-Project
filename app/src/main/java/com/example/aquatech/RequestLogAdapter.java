package com.example.aquatech;

import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
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
        if (item == null) {
            return;
        }

        String ticketId = item.getTicketId();
        if (ticketId != null && !ticketId.isEmpty()) {
            String upper = ticketId.toUpperCase().trim();
            if (upper.startsWith("SRO-")) {
                if (holder.tvTicketID != null) {
                    holder.tvTicketID.setText(upper);
                }
            } else if (upper.startsWith("SRO")) {
                if (holder.tvTicketID != null) {
                    holder.tvTicketID.setText("SRO-" + upper.substring(3));
                }
            } else {
                try {
                    long num = Long.parseLong(upper.replaceAll("[^0-9]", ""));
                    if (holder.tvTicketID != null) {
                        holder.tvTicketID.setText(String.format(Locale.US, "SRO-%08d", num));
                    }
                } catch (Exception e) {
                    if (holder.tvTicketID != null) {
                        holder.tvTicketID.setText("SRO-" + upper);
                    }
                }
            }
        } else if (holder.tvTicketID != null) {
            holder.tvTicketID.setText("SRO-00000001");
        }

        if (holder.tvCustomerID != null) {
            holder.tvCustomerID.setText(item.getCustomerId() != null && !item.getCustomerId().isEmpty() ? item.getCustomerId() : "ASC-C001");
        }

        if (holder.tvSRO != null) {
            String displayTech = item.getTechName();
            if (displayTech != null && !displayTech.isEmpty() && !"Unassigned".equalsIgnoreCase(displayTech)) {
                holder.tvSRO.setText(displayTech);
            } else if (item.getTimestamp() != null && item.getTimestamp() > 0) {
                String formattedDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date(item.getTimestamp()));
                holder.tvSRO.setText(formattedDate);
            } else {
                holder.tvSRO.setText("Unassigned");
            }
        }

        String rawStatus = item.getStatus();
        final String status = (rawStatus != null && !rawStatus.isEmpty()) ? rawStatus : "Completed";

        if (holder.tvStatus != null) {
            holder.tvStatus.setText(status.toUpperCase());

            if ("Cancelled".equalsIgnoreCase(status)) {
                holder.tvStatus.setTextColor(Color.WHITE);
                holder.tvStatus.setBackgroundResource(R.drawable.bg_solid_red_pill);
            } else {
                holder.tvStatus.setTextColor(Color.WHITE);
                holder.tvStatus.setBackgroundResource(R.drawable.bg_solid_green_pill);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent;
            if ("Completed".equalsIgnoreCase(status) || "Resolved".equalsIgnoreCase(status)) {
                intent = new Intent(v.getContext(), AdminServiceHistoryDetailsActivity.class);
            } else {
                intent = new Intent(v.getContext(), CustomerConcernDetailsActivity.class);
            }
            intent.putExtra("TICKET_ID", item.getTicketId());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        if (logList != null) {
            return logList.size();
        }
        return 0;
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
