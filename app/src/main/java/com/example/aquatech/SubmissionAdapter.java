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
        if (item == null) {
            return;
        }

        if (holder.tvTicketID != null) {
            holder.tvTicketID.setText(item.getSroNumber() != null ? item.getSroNumber() : "");
        }

        if (holder.tvTechName != null) {
            String name = item.getTechName();
            holder.tvTechName.setText("Submitted by: " + (name != null ? name : "Unknown"));
        }

        if (holder.tvCustomer != null) {
            String cust = item.getCustomerName();
            holder.tvCustomer.setText("Customer: " + (cust != null ? cust : "Guest"));
        }

        if (holder.tvServiceType != null) {
            holder.tvServiceType.setText(item.getTechRole() != null ? item.getTechRole() : "General Service");
        }

        if (holder.btnApprove != null) {
            holder.btnApprove.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onApprove(item.getSroNumber());
                }
            });
        }

        if (holder.btnReject != null) {
            holder.btnReject.setOnClickListener(v -> {
                if (listener != null) {
                    if (item.getTechUid() != null) {
                        listener.onReject(item.getSroNumber(), item.getTechUid(), item.getTechName());
                    } else {
                        listener.onReject(item.getSroNumber(), "", item.getTechName());
                    }
                }
            });
        }

        if (holder.btnViewReport != null) {
            holder.btnViewReport.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEdit(item.getSroNumber());
                }
            });
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEdit(item.getSroNumber());
            }
        });
    }

    @Override
    public int getItemCount() {
        if (list != null) {
            return list.size();
        }
        return 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTicketID, tvTechName, tvServiceType, tvCustomer;
        Button btnApprove, btnReject, btnViewReport;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTicketID = itemView.findViewById(R.id.tvSubmitTicketID);
            tvTechName = itemView.findViewById(R.id.tvSubmitTechName);
            tvServiceType = itemView.findViewById(R.id.tvSubmitServiceType);
            tvCustomer = itemView.findViewById(R.id.tvSubmitCustomer);

            btnApprove = itemView.findViewById(R.id.btnAdminApprove);
            btnReject = itemView.findViewById(R.id.btnAdminArchive);
            btnViewReport = itemView.findViewById(R.id.btnAdminViewReport);
        }
    }
}
