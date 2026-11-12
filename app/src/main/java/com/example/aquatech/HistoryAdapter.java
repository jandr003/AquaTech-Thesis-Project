package com.example.aquatech;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<HistoryItem> historyList;

    public HistoryAdapter(List<HistoryItem> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_service_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryItem item = historyList.get(position);
        if (item == null) {
            return;
        }

        if (holder.tvTicketId != null) {
            holder.tvTicketId.setText(item.getTicketId() != null ? item.getTicketId() : "");
        }

        if (holder.tvDate != null) {
            holder.tvDate.setText(item.getDate() != null ? item.getDate() : "");
        }

        if (holder.tvItems != null) {
            holder.tvItems.setText(item.getItemsSummary() != null ? item.getItemsSummary() : "");
        }

        if (holder.tvPrice != null) {
            holder.tvPrice.setText(item.getTotalPrice() != null ? item.getTotalPrice() : "");
        }

        String status = item.getStatus();
        if (status == null || status.isEmpty()) {
            status = "COMPLETED";
        }

        status = status.replaceAll("[^a-zA-Z ]", "").trim().toUpperCase();
        if (holder.tvStatus != null) {
            holder.tvStatus.setText(status);

            if ("CANCELLED".equalsIgnoreCase(status)) {
                holder.tvStatus.setTextColor(Color.WHITE);
                holder.tvStatus.setBackgroundResource(R.drawable.bg_solid_red_pill);
            } else {
                holder.tvStatus.setTextColor(Color.WHITE);
                holder.tvStatus.setBackgroundResource(R.drawable.bg_solid_green_pill);
            }
        }
    }

    @Override
    public int getItemCount() {
        if (historyList != null) {
            return historyList.size();
        }
        return 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTicketId, tvDate, tvItems, tvPrice, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTicketId = itemView.findViewById(R.id.tvHistoryTicketId);
            tvDate = itemView.findViewById(R.id.tvHistoryDate);
            tvItems = itemView.findViewById(R.id.tvHistoryItems);
            tvPrice = itemView.findViewById(R.id.tvHistoryPrice);
            tvStatus = itemView.findViewById(R.id.tvHistoryStatus);
        }
    }
}
