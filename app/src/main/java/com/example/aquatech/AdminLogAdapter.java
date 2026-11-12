package com.example.aquatech;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AdminLogAdapter extends RecyclerView.Adapter<AdminLogAdapter.ViewHolder> {

    private List<AdminLogModel> logList;

    public AdminLogAdapter(List<AdminLogModel> logList) {
        this.logList = logList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AdminLogModel log = logList.get(position);
        if (log != null && holder.tvMessage != null) {
            holder.tvMessage.setText(log.getMessage() != null ? log.getMessage() : "");
        }
    }

    @Override
    public int getItemCount() {
        if (logList != null) {
            return logList.size();
        }
        return 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvActivityMessage);
        }
    }
}
