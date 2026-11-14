package com.example.aquatech;

import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;

import java.util.List;
import java.util.Map;

public class CustomerSetupAdapter extends RecyclerView.Adapter<CustomerSetupAdapter.ViewHolder> {

    private List<Map<String, String>> unitList;
    private OnUnitClickListener listener;
    private int selectedPosition = -1;

    public interface OnUnitClickListener {
        void onUnitClick(Map<String, String> unitMap, int position);
    }

    public CustomerSetupAdapter(List<Map<String, String>> unitList, OnUnitClickListener listener) {
        this.unitList = unitList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_customer_setup_unit, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, String> unitMap = unitList.get(position);
        if (unitMap == null) {
            return;
        }

        String unitName = unitMap.get("unitName");
        String category = unitMap.get("category");
        String photoUri = unitMap.get("photoUri");

        if (holder.tvUnitName != null) {
            holder.tvUnitName.setText(unitName != null ? unitName : "Water Purifier");
        }

        if (holder.tvUnitCategory != null) {
            holder.tvUnitCategory.setText(category != null ? category : "Certified Water Purifier");
        }

        int defaultImg = getImageForUnit(unitName);
        if (holder.ivUnitImage != null) {
            if (photoUri != null && !photoUri.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(Uri.parse(photoUri))
                        .placeholder(defaultImg)
                        .into(holder.ivUnitImage);
            } else {
                holder.ivUnitImage.setImageResource(defaultImg);
            }
        }

        boolean isSelected = (position == selectedPosition);
        if (holder.cardUnitItem != null) {
            holder.cardUnitItem.setCardBackgroundColor(Color.parseColor(isSelected ? "#E3F2FD" : "#F8F8F8"));
            holder.cardUnitItem.setStrokeColor(Color.parseColor(isSelected ? "#2196F3" : "#E2E8F0"));
            holder.cardUnitItem.setStrokeWidth(isSelected ? 4 : 2);
        }

        if (holder.ivCheckIcon != null) {
            holder.ivCheckIcon.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            int prevSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            if (prevSelected >= 0) {
                notifyItemChanged(prevSelected);
            }
            notifyItemChanged(selectedPosition);

            if (listener != null) {
                listener.onUnitClick(unitMap, selectedPosition);
            }
        });
    }

    private int getImageForUnit(String name) {
        if (name == null) {
            return R.drawable.titled_design1;
        }
        String upper = name.toUpperCase();
        if (upper.contains("SLIM")) {
            return R.drawable.titled_design2;
        }
        if (upper.contains("COUNTER")) {
            return R.drawable.titled_design3;
        }
        if (upper.contains("STANDING")) {
            return R.drawable.titled_design4;
        }
        return R.drawable.titled_design1;
    }

    @Override
    public int getItemCount() {
        if (unitList != null) {
            return unitList.size();
        }
        return 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardUnitItem;
        ImageView ivUnitImage, ivCheckIcon;
        TextView tvUnitName, tvUnitCategory;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardUnitItem = itemView.findViewById(R.id.cardUnitItem);
            ivUnitImage = itemView.findViewById(R.id.ivUnitImage);
            ivCheckIcon = itemView.findViewById(R.id.ivCheckIcon);
            tvUnitName = itemView.findViewById(R.id.tvUnitName);
            tvUnitCategory = itemView.findViewById(R.id.tvUnitCategory);
        }
    }
}
