package com.example.aquatech;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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

        holder.tvCustomerName.setText(model.getTechName());

        // 🛠️ TAMA NA BINDING: Ticket ID followed by SRO Number below it
        holder.tvTicketID.setText(model.getTicketId());
        if (holder.tvSRONumber != null) {
            holder.tvSRONumber.setText("#" + model.getSroNumber());
        }

        holder.tvServiceType.setText(model.getTechRole());
        holder.tvServiceTime.setText(model.getDateTime());
        holder.tvLocation.setText(model.getAddress());
        holder.tvContact.setText(model.getCustomerPhone());

        holder.btnAssign.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAssignClick(model);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(model);
            }
        });
    }


    @Override
    public int getItemCount() {
        return openList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCustomerName, tvTicketID, tvSRONumber, tvServiceType, tvServiceTime, tvLocation, tvContact, priorityBadge;
        Button btnAssign;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
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
