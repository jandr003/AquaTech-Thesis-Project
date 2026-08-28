package com.example.aquatech;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
        holder.tvTechName.setText(item.getTechName() != null ? item.getTechName() : "Unknown Tech");
        holder.tvTicketID.setText(item.getSroNumber() != null ? item.getSroNumber() : "N/A");
        String serviceType = item.getTechRole();
        if (serviceType == null || serviceType.isEmpty()) {
            serviceType = "General Service";
        }
        holder.tvServiceType.setText(serviceType);

        String timeRange = item.getDateTime();
        holder.tvServiceTime.setText(timeRange != null ? "Service Time: " + timeRange : "Service Time: Not set");

        String address = item.getAddress();
        holder.tvLocation.setText(address != null ? address : "Address not available");

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
            if (minutes < 1) minutes = 1;

            if ("In Progress".equalsIgnoreCase(item.getStatus())) {
                liveStatus = String.format("On the way · %.1f km · %d min away", distanceKm, minutes);
                holder.tvLiveStatus.setTextColor(Color.parseColor("#8BC34A"));
            } else {
                liveStatus = String.format("Waiting for acceptance · %.1f km away", distanceKm);
                holder.tvLiveStatus.setTextColor(Color.parseColor("#FF9800"));
            }
        } else {
            if ("In Progress".equalsIgnoreCase(item.getStatus())) {
                liveStatus = "On the way to customer location";
                holder.tvLiveStatus.setTextColor(Color.parseColor("#8BC34A"));
            } else {
                liveStatus = "Waiting for acceptance";
                holder.tvLiveStatus.setTextColor(Color.parseColor("#FF9800"));
            }
        }
        holder.tvLiveStatus.setText(liveStatus);

        if ("In Progress".equalsIgnoreCase(item.getStatus())) {
            holder.badgeStatus.setText("EN ROUTE");
        } else {
            holder.badgeStatus.setText("OPEN");
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
        holder.btnTrackOnMap.setOnClickListener(v -> listener.onTrackClick(item));
    }

    @Override
    public int getItemCount() {
        return list.size();
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