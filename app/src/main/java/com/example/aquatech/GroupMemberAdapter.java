package com.example.aquatech;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;
import java.util.Map;

public class GroupMemberAdapter extends RecyclerView.Adapter<GroupMemberAdapter.ViewHolder> {

    private List<Map<String, String>> memberList;

    public GroupMemberAdapter(List<Map<String, String>> memberList) {
        this.memberList = memberList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group_member, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, String> member = memberList.get(position);
        String name = member.get("name");
        String techIdStr = member.get("techId");
        String status = member.get("status");
        String avatarUrl = member.get("avatarUrl");

        if (name != null && !name.isEmpty()) {
            holder.tvName.setText(name);
        } else {
            holder.tvName.setText("Technician Member");
        }

        if (techIdStr != null && !techIdStr.isEmpty()) {
            holder.tvId.setText(techIdStr);
        } else {
            holder.tvId.setText("ID: ASC-T001");
        }

        if ("Set nickname".equalsIgnoreCase(status) || (status != null && (status.contains("Set nickname") || status.contains("Nickname")))) {
            holder.tvStatus.setText(status);
            holder.tvStatus.setTextColor(Color.parseColor("#64748B"));
        } else if ("Busy".equalsIgnoreCase(status) || "Ongoing".equalsIgnoreCase(status)) {
            holder.tvStatus.setText("Ongoing Task");
            holder.tvStatus.setTextColor(Color.parseColor("#2196F3"));
        } else if ("Offline".equalsIgnoreCase(status)) {
            holder.tvStatus.setText("Offline");
            holder.tvStatus.setTextColor(Color.parseColor("#94A3B8"));
        } else {
            holder.tvStatus.setText("Available for task");
            holder.tvStatus.setTextColor(Color.parseColor("#059669"));
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
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(avatarUrl)
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
        return memberList != null ? memberList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvId, tvStatus;
        ImageView ivAvatar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvMemberName);
            tvId = itemView.findViewById(R.id.tvMemberId);
            tvStatus = itemView.findViewById(R.id.tvMemberStatus);
            ivAvatar = itemView.findViewById(R.id.ivMemberAvatar);
        }
    }
}
