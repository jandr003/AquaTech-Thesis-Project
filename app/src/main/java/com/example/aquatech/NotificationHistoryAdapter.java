package com.example.aquatech;

import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NotificationHistoryAdapter extends RecyclerView.Adapter<NotificationHistoryAdapter.ViewHolder> {

    private final Context context;
    private final List<NotificationModel> list;

    public NotificationHistoryAdapter(Context context, List<NotificationModel> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_notification_history, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationModel model = list.get(position);
        holder.tvMessage.setText(Html.fromHtml(model.getMessage()));
        holder.tvTime.setText(model.getTimeAgo());
        holder.ivIcon.setImageResource(model.getIconResId());

        // 🛠️ CLICK LISTENER TO REDIRECT BASED ON TYPE
        holder.itemView.setOnClickListener(v -> {
            if (model.getTicketId() != null && !model.getTicketId().isEmpty()) {
                if ("RESUBMIT".equalsIgnoreCase(model.getType())) {
                    // Open technician dashboard with resubmit flag
                    Intent intent = new Intent(context, TechnicianDashboardActivity.class);
                    intent.putExtra("RESUBMIT_TICKET_ID", model.getTicketId());
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    context.startActivity(intent);
                } else {
                    // Default to service receipt
                    Intent intent = new Intent(context, ServiceReceiptActivity.class);
                    intent.putExtra("TICKET_ID", model.getTicketId());
                    context.startActivity(intent);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvMessage, tvTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivNotifIcon);
            tvMessage = itemView.findViewById(R.id.tvNotifMessage);
            tvTime = itemView.findViewById(R.id.tvNotifTime);
        }
    }
}