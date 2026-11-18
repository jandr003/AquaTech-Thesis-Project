package com.example.aquatech;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class TrackTechAdapter extends RecyclerView.Adapter<TrackTechAdapter.ViewHolder> {

    private Context context;
    private List<ServiceLogModel> list;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ServiceLogModel model);
        void onTrackClick(ServiceLogModel model);
    }

    public TrackTechAdapter(Context context, List<ServiceLogModel> list, OnItemClickListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_track_tech, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ServiceLogModel item = list.get(position);
        if (item == null) {
            return;
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

        if (holder.ivTechAvatar != null) {
            if (item.getProfileImageUrl() != null && !item.getProfileImageUrl().isEmpty() && context != null) {
                Glide.with(context)
                        .load(item.getProfileImageUrl())
                        .placeholder(techAvatars[Math.abs(position) % techAvatars.length])
                        .circleCrop()
                        .into(holder.ivTechAvatar);
            } else if (item.getAvatarResId() != -1) {
                holder.ivTechAvatar.setImageResource(item.getAvatarResId());
            } else {
                holder.ivTechAvatar.setImageResource(techAvatars[Math.abs(position) % techAvatars.length]);
            }
        }

        if (holder.tvTechName != null) {
            holder.tvTechName.setText(item.getTechName() != null ? item.getTechName() : "Unknown Tech");
        }

        if (holder.tvTicketID != null) {
            holder.tvTicketID.setText(item.getSroNumber() != null ? item.getSroNumber() : "N/A");
        }

        String serviceType = item.getTechRole();
        if (serviceType == null || serviceType.isEmpty()) {
            serviceType = "General Service";
        }

        if (holder.tvServiceType != null) {
            holder.tvServiceType.setText(serviceType);
        }

        String timeRange = item.getDateTime();
        if (holder.tvServiceTime != null) {
            holder.tvServiceTime.setText(timeRange != null ? "Service Time: " + timeRange : "Service Time: Not set");
        }

        String address = item.getAddress();
        if (holder.tvLocation != null) {
            holder.tvLocation.setText(address != null ? address : "Address not available");
        }

        double techLat = item.getTechLat();
        double techLng = item.getTechLng();
        double custLat = item.getLatitude();
        double custLng = item.getLongitude();

        String liveStatus = "";
        if (techLat != 0 && techLng != 0 && custLat != 0 && custLng != 0) {
            float[] results = new float[1];
            Location.distanceBetween(techLat, techLng, custLat, custLng, results);
            float distanceMeters = results[0];
            float distanceKm = distanceMeters / 1000;

            int minutes = (int) (distanceKm / 30 * 60);
            if (minutes < 1) {
                minutes = 1;
            }

            if ("In Progress".equalsIgnoreCase(item.getStatus())) {
                liveStatus = String.format("On the way · %.1f km · %d min away", distanceKm, minutes);
                if (holder.tvLiveStatus != null) {
                    holder.tvLiveStatus.setTextColor(Color.parseColor("#8BC34A"));
                }
            } else {
                liveStatus = String.format("Waiting for acceptance · %.1f km away", distanceKm);
                if (holder.tvLiveStatus != null) {
                    holder.tvLiveStatus.setTextColor(Color.parseColor("#FF9800"));
                }
            }
        } else {
            if ("In Progress".equalsIgnoreCase(item.getStatus())) {
                liveStatus = "On the way to customer location";
                if (holder.tvLiveStatus != null) {
                    holder.tvLiveStatus.setTextColor(Color.parseColor("#8BC34A"));
                }
            } else {
                liveStatus = "Waiting for acceptance";
                if (holder.tvLiveStatus != null) {
                    holder.tvLiveStatus.setTextColor(Color.parseColor("#FF9800"));
                }
            }
        }

        if (holder.tvLiveStatus != null) {
            holder.tvLiveStatus.setText(liveStatus);
        }

        if (holder.badgeStatus != null) {
            if ("In Progress".equalsIgnoreCase(item.getStatus())) {
                holder.badgeStatus.setText("EN ROUTE");
            } else {
                holder.badgeStatus.setText("OPEN");
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });

        if (holder.btnTrackOnMap != null) {
            holder.btnTrackOnMap.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTrackClick(item);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        if (list != null) {
            return list.size();
        }
        return 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivTechAvatar;
        TextView tvTechName, tvTicketID, tvServiceType, tvServiceTime, tvLocation, tvLiveStatus, badgeStatus;
        MaterialButton btnTrackOnMap;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivTechAvatar = itemView.findViewById(R.id.ivTechAvatar);
            tvTechName = itemView.findViewById(R.id.tvTechName);
            tvTicketID = itemView.findViewById(R.id.tvTicketID);
            tvServiceType = itemView.findViewById(R.id.tvServiceType);
            tvServiceTime = itemView.findViewById(R.id.tvServiceTime);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvLiveStatus = itemView.findViewById(R.id.tvLiveStatus);
            badgeStatus = itemView.findViewById(R.id.badgeStatus);
            btnTrackOnMap = itemView.findViewById(R.id.btnTrackOnMap);
        }
    }
}
