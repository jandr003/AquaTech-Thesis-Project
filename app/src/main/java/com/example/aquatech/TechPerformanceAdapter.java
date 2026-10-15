package com.example.aquatech;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;
import java.util.Locale;

public class TechPerformanceAdapter extends RecyclerView.Adapter<TechPerformanceAdapter.ViewHolder> {

    private List<TechnicianModel> techList;

    public TechPerformanceAdapter(List<TechnicianModel> techList) {
        this.techList = techList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tech_performance, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TechnicianModel tech = techList.get(position);
        holder.tvTechName.setText(tech.fullName != null ? tech.fullName : "Unknown Tech");
        holder.tvTechRating.setText(String.format(Locale.getDefault(), "%.1f ★", tech.rating));

        int progressValue = (int) (tech.rating * 10);
        holder.pbTechRating.setProgress(progressValue);

        if (holder.ivAvatar != null) {
            if (tech.profileImageUrl != null && !tech.profileImageUrl.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(tech.profileImageUrl)
                        .placeholder(R.drawable.new_technician)
                        .circleCrop()
                        .into(holder.ivAvatar);
            } else {
                holder.ivAvatar.setImageResource(R.drawable.new_technician);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, TechnicianPerformanceActivity.class);
            intent.putExtra("TECH_ID", tech.techId);
            intent.putExtra("TECH_NAME", tech.fullName);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return techList != null ? techList.size() : 0;
    }

    public void updateList(List<TechnicianModel> newList) {
        this.techList = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTechName, tvTechRating;
        ImageView ivAvatar;
        ProgressBar pbTechRating;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTechName = itemView.findViewById(R.id.tvTechName);
            tvTechRating = itemView.findViewById(R.id.tvTechRating);
            ivAvatar = itemView.findViewById(R.id.ivTechAvatar);
            pbTechRating = itemView.findViewById(R.id.pbTechRating);
        }
    }
}
