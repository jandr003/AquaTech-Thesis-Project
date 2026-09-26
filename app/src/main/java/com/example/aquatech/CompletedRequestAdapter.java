package com.example.aquatech;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CompletedRequestAdapter extends RecyclerView.Adapter<CompletedRequestAdapter.ViewHolder> {

    private List<ServiceLogModel> completedList;
    private Context context;

    public CompletedRequestAdapter(List<ServiceLogModel> completedList) {
        this.completedList = completedList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_completed_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ServiceLogModel model = completedList.get(position);

        holder.tvCustomerName.setText(model.getCustomerName() != null ? model.getCustomerName() : "Guest Customer");
        holder.tvTicketID.setText(model.getSroNumber() != null ? model.getSroNumber() : "N/A");

        if (holder.tvPurchaseType != null) {
            String pType = model.getPurchaseType();
            holder.tvPurchaseType.setText(pType != null ? pType.toUpperCase() : "SUBSCRIPTION");
        }

        holder.tvServiceType.setText(model.getTechRole() != null ? model.getTechRole() : "General Maintenance");

        if (holder.tvRemarks != null) {
            String rem = model.getRemarks();
            if (rem != null && !rem.trim().isEmpty()) {
                holder.tvRemarks.setVisibility(View.VISIBLE);
                holder.tvRemarks.setText("Remarks: " + rem);
            } else {
                holder.tvRemarks.setVisibility(View.GONE);
            }
        }

        holder.tvCompletionTime.setText("Completed on: " + (model.getDateTime() != null ? model.getDateTime() : "N/A"));
        holder.tvLocation.setText(model.getAddress() != null ? model.getAddress() : "No address provided");

        holder.btnViewDetails.setOnClickListener(v -> {
            Intent intent = new Intent(context, ServiceReportDetailsActivity.class);
            intent.putExtra("TICKET_ID", model.getSroNumber());
            intent.putExtra("CUSTOMER_NAME", model.getCustomerName());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return completedList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCustomerName, tvTicketID, tvServiceType, tvPurchaseType, tvRemarks, tvCompletionTime, tvLocation;
        Button btnViewDetails;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerNameComp);
            tvTicketID = itemView.findViewById(R.id.tvTicketIDComp);
            tvPurchaseType = itemView.findViewById(R.id.tvPurchaseTypeComp);
            tvServiceType = itemView.findViewById(R.id.tvServiceTypeComp);
            tvRemarks = itemView.findViewById(R.id.tvRemarksComp);
            tvCompletionTime = itemView.findViewById(R.id.tvCompletionTime);
            tvLocation = itemView.findViewById(R.id.tvLocationComp);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
        }
    }
}
