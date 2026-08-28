package com.example.aquatech;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SubmissionAdapter extends RecyclerView.Adapter<SubmissionAdapter.ViewHolder> {

    private List<ServiceLogModel> list;
    private OnActionClickListener listener;

    public interface OnActionClickListener {
        void onApprove(String ticketId);
        void onReject(String ticketId, String techUid, String techName);
        void onEdit(String ticketId);
    }

    public SubmissionAdapter(List<ServiceLogModel> list, OnActionClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_submission_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ServiceLogModel item = list.get(position);

        holder.tvTicketID.setText(item.getSroNumber());
        holder.tvTechName.setText("Submitted by: " + item.getTechName());
        holder.tvCustomer.setText("Customer: " + item.getCustomerName());
        holder.tvServiceType.setText(item.getTechRole());
        holder.btnApprove.setOnClickListener(v -> listener.onApprove(item.getSroNumber()));
        holder.btnReject.setOnClickListener(v -> {
            if (item.getTechUid() != null) {
                listener.onReject(item.getSroNumber(), item.getTechUid(), item.getTechName());
            } else {
                listener.onReject(item.getSroNumber(), "", item.getTechName());
            }
        });
        // holder.btnEdit.setOnClickListener(v -> listener.onEdit(item.getSroNumber()));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTicketID, tvTechName, tvServiceType, tvCustomer;
        Button btnApprove, btnReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTicketID = itemView.findViewById(R.id.tvSubmitTicketID);
            tvTechName = itemView.findViewById(R.id.tvSubmitTechName);
            tvServiceType = itemView.findViewById(R.id.tvSubmitServiceType);
            tvCustomer = itemView.findViewById(R.id.tvSubmitCustomer);

            btnApprove = itemView.findViewById(R.id.btnAdminApprove);
            btnReject = itemView.findViewById(R.id.btnAdminArchive);
        }
    }
}