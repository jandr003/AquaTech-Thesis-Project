package com.example.aquatech;

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

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final List<NotificationModel> notificationList;

    public NotificationAdapter(List<NotificationModel> notificationList) {
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationModel notif = notificationList.get(position);
        if (notif == null) {
            return;
        }

        if (holder.tvMessage != null && notif.getMessage() != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                holder.tvMessage.setText(Html.fromHtml(notif.getMessage(), Html.FROM_HTML_MODE_LEGACY));
            } else {
                holder.tvMessage.setText(Html.fromHtml(notif.getMessage()));
            }
        }

        if (holder.tvTime != null) {
            holder.tvTime.setText(notif.getTimeAgo() != null ? notif.getTimeAgo() : "");
        }

        if (holder.ivIcon != null) {
            holder.ivIcon.setImageResource(notif.getIconResId());
        }
    }

    @Override
    public int getItemCount() {
        if (notificationList != null) {
            return notificationList.size();
        }
        return 0;
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        ImageView ivIcon;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvNotifMessage);
            tvTime = itemView.findViewById(R.id.tvNotifTime);
            ivIcon = itemView.findViewById(R.id.ivNotifIcon);
        }
    }
}
