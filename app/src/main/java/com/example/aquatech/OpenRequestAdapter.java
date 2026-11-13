package com.example.aquatech;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class OpenRequestAdapter extends RecyclerView.Adapter<OpenRequestAdapter.ViewHolder> {

    private List<ServiceLogModel> openList;
    private OnAssignClickListener listener;
    private OnItemClickListener itemClickListener;

    public interface OnAssignClickListener {
        void onAssignClick(ServiceLogModel ticket);
    }

    public interface OnItemClickListener {
        void onItemClick(ServiceLogModel ticket);
    }

    public OpenRequestAdapter(List<ServiceLogModel> openList, OnAssignClickListener listener, OnItemClickListener itemClickListener) {
        this.openList = openList;
        this.listener = listener;
        this.itemClickListener = itemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_open_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ServiceLogModel model = openList.get(position);
        if (model == null) {
            return;
        }

        if (holder.tvCustomerName != null) {
            holder.tvCustomerName.setText(model.getTechName() != null ? model.getTechName() : "Guest Customer");
        }

        if (holder.ivCustomerAvatar != null) {
            if (model.getProfileImageUrl() != null && !model.getProfileImageUrl().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(model.getProfileImageUrl())
                        .placeholder(R.drawable.customer_avatar1)
                        .circleCrop()
                        .into(holder.ivCustomerAvatar);
            } else if (model.getAvatarResId() > 0) {
                holder.ivCustomerAvatar.setImageResource(model.getAvatarResId());
            } else {
                holder.ivCustomerAvatar.setImageResource(R.drawable.customer_avatar1);
            }
        }

        if (holder.tvTicketID != null) {
            holder.tvTicketID.setText(model.getTicketId() != null ? model.getTicketId() : "");
        }

        if (holder.tvSRONumber != null) {
            holder.tvSRONumber.setText("#" + (model.getSroNumber() != null ? model.getSroNumber() : ""));
        }

        if (holder.tvUnitName != null) {
            holder.tvUnitName.setText(model.getUnitName() != null ? model.getUnitName() : "");
        }

        String serviceType = model.getRemarks();
        if (serviceType == null || serviceType.trim().isEmpty()) {
            serviceType = model.getTechRole();
        }
        if (serviceType == null || serviceType.trim().isEmpty()) {
            serviceType = "General Service Request";
        }

        if (holder.tvServiceType != null) {
            holder.tvServiceType.setText(serviceType);
        }

        if (holder.tvServiceTime != null) {
            holder.tvServiceTime.setText(model.getDateTime() != null ? model.getDateTime() : "");
        }

        if (holder.tvLocation != null) {
            holder.tvLocation.setText(model.getAddress() != null ? model.getAddress() : "");
        }

        if (holder.tvContact != null) {
            holder.tvContact.setText(model.getCustomerPhone() != null ? model.getCustomerPhone() : "");
        }

        if (holder.btnAssign != null) {
            holder.btnAssign.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAssignClick(model);
                }
            });
        }

        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(model);
            }
        });
    }

    @Override
    public int getItemCount() {
        if (openList != null) {
            return openList.size();
        }
        return 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCustomerAvatar;
        TextView tvCustomerName, tvUnitName, tvTicketID, tvSRONumber, tvServiceType, tvServiceTime, tvLocation, tvContact, priorityBadge;
        Button btnAssign;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCustomerAvatar = itemView.findViewById(R.id.ivCustomerAvatar);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvUnitName = itemView.findViewById(R.id.tvUnitNameOpen);
            tvTicketID = itemView.findViewById(R.id.tvTicketIDOpen);
            tvSRONumber = itemView.findViewById(R.id.tvSRONumberOpen);
            tvServiceType = itemView.findViewById(R.id.tvServiceTypeOpen);
            tvServiceTime = itemView.findViewById(R.id.tvServiceTimeOpen);
            tvLocation = itemView.findViewById(R.id.tvLocationOpen);
            tvContact = itemView.findViewById(R.id.tvContactOpen);
            priorityBadge = itemView.findViewById(R.id.priorityBadge);
            btnAssign = itemView.findViewById(R.id.btnAssignTech);
        }
    }
}
