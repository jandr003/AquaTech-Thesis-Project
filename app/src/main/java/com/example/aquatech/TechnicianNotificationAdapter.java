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
        if (notif == null) {
            return;
        }

        String type = notif.getType() != null ? notif.getType().toUpperCase() : "GENERAL";
        if (holder.tvTitle != null) {
            holder.tvTitle.setText(getDisplayTitle(type));
        }

        if (holder.tvBody != null && notif.getMessage() != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                holder.tvBody.setText(Html.fromHtml(notif.getMessage(), Html.FROM_HTML_MODE_LEGACY));
            } else {
                holder.tvBody.setText(Html.fromHtml(notif.getMessage()));
            }
        }

        if (holder.tvTime != null) {
            holder.tvTime.setText(notif.getTimeAgo() != null ? notif.getTimeAgo() : "");
        }

        if (holder.ivIcon != null) {
            holder.ivIcon.setImageResource(notif.getIconResId());
        }
    }

    private String getDisplayTitle(String type) {
        if (type == null) {
            return "System Update";
        }

        switch (type) {
            case "ASSIGNED":
                return "New Booking Assigned";
            case "COMPLETED":
                return "Job Completed";
            case "MESSAGE":
                return "New Message";
            case "CALL":
                return "Missed Call";
            case "RESUBMIT":
                return "Revision Required";
            case "PDF":
                return "Report Generated";
            default:
                return "System Update";
        }
    }

    private int getIconBgColor(String type) {
        if (type == null) {
            return Color.parseColor("#FFF5F5");
        }

        switch (type) {
            case "ASSIGNED":
            case "RESUBMIT":
            case "COMPLETED":
            case "MESSAGE":
            default:
                return Color.parseColor("#FFF5F5");
        }
    }

    @Override
    public int getItemCount() {
        if (notificationList != null) {
            return notificationList.size();
        }
        return 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvBody, tvTime;
        ImageView ivIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotifTitle);
            tvBody = itemView.findViewById(R.id.tvNotifBody);
            tvTime = itemView.findViewById(R.id.tvNotifTime);
            ivIcon = itemView.findViewById(R.id.ivNotifIcon);
        }
    }
}
