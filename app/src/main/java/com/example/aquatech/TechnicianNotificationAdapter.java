package com.example.aquatech;

import android.graphics.Color;
import android.os.Build;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TechnicianNotificationAdapter extends RecyclerView.Adapter<TechnicianNotificationAdapter.ViewHolder> {

    private final List<NotificationModel> notificationList;

    public TechnicianNotificationAdapter(List<NotificationModel> notificationList) {
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification_tech, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationModel notif = notificationList.get(position);

        String type = notif.getType() != null ? notif.getType().toUpperCase() : "GENERAL";
        holder.tvTitle.setText(getDisplayTitle(type));
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            holder.tvBody.setText(Html.fromHtml(notif.getMessage(), Html.FROM_HTML_MODE_LEGACY));
        } else {
            holder.tvBody.setText(Html.fromHtml(notif.getMessage()));
        }

        holder.tvTime.setText(notif.getTimeAgo());
        holder.ivIcon.setImageResource(notif.getIconResId());
        holder.cardIconBg.setCardBackgroundColor(getIconBgColor(type));
    }

    private String getDisplayTitle(String type) {
        switch (type) {
            case "ASSIGNED": return "New Booking Assigned";
            case "COMPLETED": return "Job Completed";
            case "MESSAGE": return "New Message";
            case "CALL": return "Missed Call";
            case "RESUBMIT": return "Revision Required";
            case "PDF": return "Report Generated";
            default: return "System Update";
        }
    }

    private int getIconBgColor(String type) {
        switch (type) {
            case "ASSIGNED":
            case "RESUBMIT":
            case "COMPLETED":
            case "MESSAGE":
            default: return Color.parseColor("#FFF5F5");
        }
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvBody, tvTime;
        ImageView ivIcon;
        CardView cardIconBg;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotifTitle);
            tvBody = itemView.findViewById(R.id.tvNotifBody);
            tvTime = itemView.findViewById(R.id.tvNotifTime);
            ivIcon = itemView.findViewById(R.id.ivNotifIcon);
            cardIconBg = itemView.findViewById(R.id.cardIconBg);
        }
    }
}
