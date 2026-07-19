package com.example.aquatech;

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
        
        holder.tvTicketId.setText(item.getTicketId());
        holder.tvDate.setText(item.getDate());
        holder.tvItems.setText(item.getItemsSummary());
        holder.tvPrice.setText(item.getTotalPrice());
        holder.tvStatus.setText(item.getStatus());
    }

    @Override
    public int getItemCount() {
        return historyList.size();
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
