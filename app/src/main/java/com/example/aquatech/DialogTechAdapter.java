package com.example.aquatech;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

public class DialogTechAdapter extends RecyclerView.Adapter<DialogTechAdapter.ViewHolder> {

    private List<Map<String, String>> techList;
    private OnTechClickListener listener;

    public interface OnTechClickListener {
        void onTechClick(String name, String uid);
    }

    public DialogTechAdapter(List<Map<String, String>> techList, OnTechClickListener listener) {
        this.techList = techList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dialog_tech, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, String> tech = techList.get(position);
        String name = tech.get("name");
        String uid = tech.get("uid");
        String techIdStr = tech.get("techId");
        String status = tech.get("status"); // ✅ Now we get status from the map

        // Display name
        String displayName = (name != null && !name.isEmpty()) ? name : "Unknown Technician";
        holder.tvName.setText(displayName);

        // Display tech ID
        holder.tvId.setText(techIdStr != null ? techIdStr : "N/A");

        // Display status with appropriate color
        if ("Busy".equalsIgnoreCase(status)) {
            holder.tvStatus.setText("Currently performing a task (Busy)");
            holder.tvStatus.setTextColor(Color.parseColor("#FF5252"));
        } else {
            holder.tvStatus.setText("Available for task");
            holder.tvStatus.setTextColor(Color.parseColor("#8BC34A"));
        }

        // Click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTechClick(displayName, uid != null ? uid : "");
            }
        });
    }

    @Override
    public int getItemCount() {
        return techList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvStatus, tvId;
        ImageView ivAvatar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvTechNameDialog);
            tvStatus = itemView.findViewById(R.id.tvTechStatusDialog);
            tvId = itemView.findViewById(R.id.tvTechIdDialog);
            ivAvatar = itemView.findViewById(R.id.ivTechAvatarDialog);
        }
    }
}