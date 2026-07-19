package com.example.aquatech;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class ManageTechniciansAdapter extends RecyclerView.Adapter<ManageTechniciansAdapter.ViewHolder> {

    private Context context;
    private List<TechnicianModel> techList;
    private OnTechActionListener listener;

    public interface OnTechActionListener {
        void onViewVerification(TechnicianModel tech);
        void onDeleteTech(TechnicianModel tech);
    }

    public ManageTechniciansAdapter(Context context, List<TechnicianModel> techList, OnTechActionListener listener) {
        this.context = context;
        this.techList = techList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_manage_tech, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TechnicianModel tech = techList.get(position);
        
        String name = tech.fullName != null ? tech.fullName : (tech.name != null ? tech.name : "Unknown Technician");
        holder.tvName.setText(name);

        // 📍 MODIFIED: Show custom ASC-T ID instead of the long UID string
        if (tech.techId != null && !tech.techId.isEmpty()) {
            holder.tvId.setText("ID: " + tech.techId);
        } else {
            // Fallback: If techId is missing in the object, show a generic placeholder
            holder.tvId.setText("ID: ASC-T (Pending)");
        }

        // DEFAULT STATUS (PENDING)
        String statusText = "PENDING";
        int statusBg = R.drawable.status_pending_orange;
        int textColor = ContextCompat.getColor(context, android.R.color.white);

        // CHECK ACCOUNT STATUS
        if ("disabled".equalsIgnoreCase(tech.accountStatus)) {
            statusText = "DISABLED";
            statusBg = R.drawable.status_cancelled_red;
        } else if (tech.verification != null && "Verified".equalsIgnoreCase(tech.verification.status)) {
            statusText = "ACTIVE";
            statusBg = R.drawable.status_completed_green;
        } else if (tech.verification != null && "Rejected".equalsIgnoreCase(tech.verification.status)) {
            statusText = "REJECTED";
            statusBg = R.drawable.status_cancelled_red;
        }

        holder.tvStatus.setText(statusText);
        holder.tvStatus.setBackgroundResource(statusBg);
        holder.tvStatus.setTextColor(textColor);

        // LOAD PROFILE IMAGE
        if (tech.profileImageUrl != null && !tech.profileImageUrl.isEmpty()) {
            Glide.with(context)
                    .load(tech.profileImageUrl)
                    .placeholder(R.drawable.new_technician)
                    .circleCrop()
                    .into(holder.ivProfile);
        } else {
            holder.ivProfile.setImageResource(R.drawable.new_technician);
        }

        // BUTTON CLICK LISTENERS
        holder.btnView.setOnClickListener(v -> {
            if (listener != null) listener.onViewVerification(tech);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteTech(tech);
        });
    }

    @Override
    public int getItemCount() {
        return techList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfile, btnView, btnDelete;
        TextView tvName, tvId, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.ivTechItemProfile);
            tvName = itemView.findViewById(R.id.tvTechItemName);
            tvId = itemView.findViewById(R.id.tvTechItemID);
            tvStatus = itemView.findViewById(R.id.tvTechItemStatus);
            btnView = itemView.findViewById(R.id.btnViewVerification);
            btnDelete = itemView.findViewById(R.id.btnDeleteTech);
        }
    }
}
