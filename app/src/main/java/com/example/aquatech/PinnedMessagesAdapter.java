package com.example.aquatech;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PinnedMessagesAdapter extends RecyclerView.Adapter<PinnedMessagesAdapter.ViewHolder> {

    private List<ChatMessage> pinnedList;
    private String currentUid;

    public PinnedMessagesAdapter(List<ChatMessage> pinnedList, String currentUid) {
        this.pinnedList = pinnedList;
        this.currentUid = currentUid;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pinned_message_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessage msg = pinnedList.get(position);
        if (msg == null) {
            return;
        }

        boolean isMe = currentUid != null && currentUid.equals(msg.getSenderId());
        String senderDisplayName = isMe ? "You" : (msg.getSenderName() != null ? msg.getSenderName() : "Technician");
        if (holder.tvSender != null) {
            holder.tvSender.setText(senderDisplayName);
        }

        if (holder.tvMessage != null) {
            holder.tvMessage.setText(msg.getText() != null ? msg.getText() : "");
        }

        if (holder.tvTime != null) {
            if (msg.getTimestamp() > 0) {
                String timeStr = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date(msg.getTimestamp()));
                holder.tvTime.setText(timeStr);
            } else {
                holder.tvTime.setText("");
            }
        }

        if (holder.tvStatus != null) {
            holder.tvStatus.setText("Delivered");
        }

        int[] techAvatars = {
                R.drawable.technician_avatar1,
                R.drawable.technician_avatar2,
                R.drawable.technician_avatar3,
                R.drawable.technician_avatar4,
                R.drawable.technician_avatar5,
                R.drawable.technician_avatar6,
                R.drawable.technician_avatar7,
                R.drawable.technician_avatar8
        };
        int defaultAvatar = techAvatars[Math.abs(position) % techAvatars.length];

        if (holder.ivAvatar != null) {
            if (msg.getFileUrl() != null && !msg.getFileUrl().isEmpty() && msg.getFileUrl().contains("http")) {
                Glide.with(holder.itemView.getContext())
                        .load(msg.getFileUrl())
                        .placeholder(defaultAvatar)
                        .circleCrop()
                        .into(holder.ivAvatar);
            } else {
                holder.ivAvatar.setImageResource(defaultAvatar);
            }
        }
    }

    @Override
    public int getItemCount() {
        if (pinnedList != null) {
            return pinnedList.size();
        }
        return 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSender, tvMessage, tvTime, tvStatus;
        ImageView ivAvatar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSender = itemView.findViewById(R.id.tvPinnedSenderName);
            tvMessage = itemView.findViewById(R.id.tvPinnedMsgText);
            tvTime = itemView.findViewById(R.id.tvPinnedTime);
            tvStatus = itemView.findViewById(R.id.tvPinnedStatusText);
            ivAvatar = itemView.findViewById(R.id.ivPinnedSenderAvatar);
        }
    }
}
