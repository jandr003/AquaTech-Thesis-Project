package com.example.aquatech;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
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
        
        // 📍 REMOVED " / 5.0" - Now showing only the score (e.g., 5.6, 6.1)
        holder.tvTechRating.setText(String.format(Locale.getDefault(), "%.1f", tech.rating));
        
        // 📍 PROGRESS BAR: Max is 50 (for 5.0 precision)
        int progressValue = (int)(tech.rating * 10);
        holder.pbTechRating.setProgress(progressValue);

        // Status Indicator based on accountStatus
        if ("active".equalsIgnoreCase(tech.accountStatus)) {
            holder.statusIndicator.setCardBackgroundColor(0xFF8BC34A); // Green
        } else if ("pending".equalsIgnoreCase(tech.accountStatus)) {
            holder.statusIndicator.setCardBackgroundColor(0xFFFFA000); // Orange/Amber
        } else {
            holder.statusIndicator.setCardBackgroundColor(0xFFAAAAAA); // Gray (Offline/Disabled)
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
        return techList.size();
    }

    public void updateList(List<TechnicianModel> newList) {
        this.techList = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTechName, tvTechRating;
        MaterialCardView statusIndicator;
        ProgressBar pbTechRating;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTechName = itemView.findViewById(R.id.tvTechName);
            tvTechRating = itemView.findViewById(R.id.tvTechRating);
            statusIndicator = itemView.findViewById(R.id.techStatusIndicator);
            pbTechRating = itemView.findViewById(R.id.pbTechRating);
        }
    }
}
