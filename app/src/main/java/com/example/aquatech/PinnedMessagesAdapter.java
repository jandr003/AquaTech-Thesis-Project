package com.example.aquatech;

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

        boolean isMe = currentUid != null && currentUid.equals(msg.getSenderId());
        String senderDisplayName = isMe ? "You" : (msg.getSenderName() != null ? msg.getSenderName() : "Technician");
        holder.tvSender.setText(senderDisplayName);

        holder.tvMessage.setText(msg.getText() != null ? msg.getText() : "");

        if (msg.getTimestamp() > 0) {
            String timeStr = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date(msg.getTimestamp()));
            holder.tvTime.setText(timeStr);
        } else {
            holder.tvTime.setText("");
        }

        holder.tvStatus.setText("Delivered");
    }

    @Override
    public int getItemCount() {
        return pinnedList != null ? pinnedList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSender, tvMessage, tvTime, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSender = itemView.findViewById(R.id.tvPinnedSenderName);
            tvMessage = itemView.findViewById(R.id.tvPinnedMsgText);
            tvTime = itemView.findViewById(R.id.tvPinnedTime);
            tvStatus = itemView.findViewById(R.id.tvPinnedStatusText);
        }
    }
}
