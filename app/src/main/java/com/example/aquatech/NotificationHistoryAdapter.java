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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

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
        int resId = model.getIconResId();
        holder.ivIcon.setImageResource(resId);
        if (resId == R.drawable.ic_bell_notification) {
            holder.ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.aqua_primary));
        } else {
            holder.ivIcon.clearColorFilter();
        }

        holder.itemView.setOnClickListener(v -> {
            String type = model.getType() != null ? model.getType().toUpperCase() : "";

            if ("CHAT".equalsIgnoreCase(type) || "MESSAGE".equalsIgnoreCase(type) || "CALL".equalsIgnoreCase(type) || "AQUABUDDY".equalsIgnoreCase(type) || "AI".equalsIgnoreCase(type)) {
                if (model.getId() != null && FirebaseAuth.getInstance().getCurrentUser() != null) {
                    String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    FirebaseDatabase.getInstance("https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/")
                            .getReference("CustomerNotifications").child(uid).child(model.getId()).removeValue();
                }
            }

            if ("CHAT".equalsIgnoreCase(type) || "MESSAGE".equalsIgnoreCase(type)) {
                Intent intent = new Intent(context, CustomerChatActivity.class);
                intent.putExtra("TICKET_ID", model.getTicketId());
                context.startActivity(intent);
            } else if ("AQUABUDDY".equalsIgnoreCase(type) || "AI".equalsIgnoreCase(type)) {
                Intent intent = new Intent(context, AquaBuddyActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } else if ("CALL".equalsIgnoreCase(type)) {
                Intent intent = new Intent(context, CustomerVoiceCallActivity.class);
                intent.putExtra("TICKET_ID", model.getTicketId());
                context.startActivity(intent);
            } else if ("COMPLETED".equalsIgnoreCase(type) || "PDF".equalsIgnoreCase(type)) {
                Intent intent = new Intent(context, ServiceReceiptActivity.class);
                intent.putExtra("TICKET_ID", model.getTicketId());
                context.startActivity(intent);
            } else if ("ASSIGNED".equalsIgnoreCase(type) || "IN_PROGRESS".equalsIgnoreCase(type) || "PENDING".equalsIgnoreCase(type) || "RESCHEDULED".equalsIgnoreCase(type) || "SCHEDULE".equalsIgnoreCase(type)) {
                Intent intent = new Intent(context, TrackServiceActivity.class);
                intent.putExtra("TICKET_ID", model.getTicketId());
                context.startActivity(intent);
            } else if (model.getTicketId() != null && !model.getTicketId().isEmpty()) {
                Intent intent = new Intent(context, TrackServiceActivity.class);
                intent.putExtra("TICKET_ID", model.getTicketId());
                context.startActivity(intent);
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