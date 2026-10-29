package com.example.aquatech;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminServiceHistoryActivity extends AppCompatActivity {

    private static final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private DatabaseReference requestsRef;

    private TextView tvHeaderTotalCount, tvStatTotal, tvStatCompleted, tvStatPending;
    private SearchView searchView;
    private ChipGroup chipGroupStatus;
    private RecyclerView rvHistory;

    private final List<ServiceLogModel> fullHistoryList = new ArrayList<>();
    private final List<ServiceLogModel> filteredHistoryList = new ArrayList<>();
    private HistoryAdapter adapter;

    private String currentStatusFilter = "ALL";
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_service_history);

        setupStatusBar();
        initializeViews();
        setupFirebase();
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initializeViews() {
        findViewById(R.id.btnBackAdminHistory).setOnClickListener(v -> finish());

        tvHeaderTotalCount = findViewById(R.id.tvTotalHistoryCount);
        tvStatTotal = findViewById(R.id.tvStatTotal);
        tvStatCompleted = findViewById(R.id.tvStatCompleted);

        searchView = findViewById(R.id.searchAdminHistory);
        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    currentSearchQuery = query != null ? query.trim().toLowerCase() : "";
                    applyFilters();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    currentSearchQuery = newText != null ? newText.trim().toLowerCase() : "";
                    applyFilters();
                    return true;
                }
            });
        }

        chipGroupStatus = findViewById(R.id.chipGroupStatus);
        if (chipGroupStatus != null) {
            chipGroupStatus.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.chipCompleted) {
                    currentStatusFilter = "COMPLETED";
                } else if (checkedId == R.id.chipCancelled) {
                    currentStatusFilter = "CANCELLED";
                } else {
                    currentStatusFilter = "ALL";
                }
                applyFilters();
            });
        }

        rvHistory = findViewById(R.id.rvAdminServiceHistory);
        if (rvHistory != null) {
            rvHistory.setLayoutManager(new LinearLayoutManager(this));
            adapter = new HistoryAdapter(filteredHistoryList);
            rvHistory.setAdapter(adapter);
        }
    }

    private void setupFirebase() {
        requestsRef = FirebaseDatabase.getInstance(DB_URL).getReference("ServiceRequests");
        requestsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullHistoryList.clear();
                int totalCount = 0;
                int completedCount = 0;
                int pendingCount = 0;

                Map<String, Integer> monthCounts = new HashMap<>();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    totalCount++;
                    String status = ds.child("status").getValue(String.class);
                    if ("Completed".equalsIgnoreCase(status)) {
                        completedCount++;
                    } else if ("Pending".equalsIgnoreCase(status) || "Submitted".equalsIgnoreCase(status) || "Submission".equalsIgnoreCase(status)) {
                        pendingCount++;
                    }

                    String ticketId = ds.getKey();
                    String custName = ds.child("customerName").getValue(String.class);
                    String techName = ds.child("assignedTechName").getValue(String.class);
                    String dateStr = ds.child("date").getValue(String.class);

                    ServiceLogModel item = new ServiceLogModel();
                    item.setTicketId(ticketId != null ? ticketId : "");
                    item.setCustomerName(custName != null ? custName : "Customer");
                    item.setTechName(techName != null ? techName : "Unassigned");
                    item.setStatus(status != null ? status : "Open");
                    item.setDateTime(dateStr != null ? dateStr : "");
                    fullHistoryList.add(item);

                    if (dateStr != null && dateStr.contains(" ")) {
                        String month = dateStr.split(" ")[0].toUpperCase();
                        monthCounts.put(month, monthCounts.getOrDefault(month, 0) + 1);
                    }
                }

                Collections.reverse(fullHistoryList);

                if (tvHeaderTotalCount != null) tvHeaderTotalCount.setText(totalCount + " Records");
                if (tvStatTotal != null) tvStatTotal.setText(String.valueOf(totalCount));
                if (tvStatCompleted != null) tvStatCompleted.setText(String.valueOf(completedCount));
                if (tvStatPending != null) tvStatPending.setText(String.valueOf(pendingCount));

                applyFilters();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void applyFilters() {
        filteredHistoryList.clear();
        for (ServiceLogModel item : fullHistoryList) {
            boolean matchesStatus = true;
            if ("COMPLETED".equalsIgnoreCase(currentStatusFilter)) {
                matchesStatus = "Completed".equalsIgnoreCase(item.getStatus());
            } else if ("IN_PROGRESS".equalsIgnoreCase(currentStatusFilter)) {
                matchesStatus = "In Progress".equalsIgnoreCase(item.getStatus()) || "Assigned".equalsIgnoreCase(item.getStatus());
            } else if ("CANCELLED".equalsIgnoreCase(currentStatusFilter)) {
                matchesStatus = "Cancelled".equalsIgnoreCase(item.getStatus());
            }

            boolean matchesSearch = true;
            if (!currentSearchQuery.isEmpty()) {
                String key = item.getTicketId() != null ? item.getTicketId().toLowerCase() : "";
                String cust = item.getCustomerName() != null ? item.getCustomerName().toLowerCase() : "";
                String tech = item.getTechName() != null ? item.getTechName().toLowerCase() : "";
                matchesSearch = key.contains(currentSearchQuery) || cust.contains(currentSearchQuery) || tech.contains(currentSearchQuery);
            }

            if (matchesStatus && matchesSearch) {
                filteredHistoryList.add(item);
            }
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private final List<ServiceLogModel> list;

        HistoryAdapter(List<ServiceLogModel> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_service_history_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ServiceLogModel item = list.get(position);

            String ticketId = item.getTicketId();
            if (ticketId != null && !ticketId.isEmpty()) {
                String upper = ticketId.toUpperCase().trim();
                if (upper.startsWith("SRO-")) {
                    holder.tvTicket.setText(upper);
                } else if (upper.startsWith("SRO")) {
                    holder.tvTicket.setText("SRO-" + upper.substring(3));
                } else {
                    try {
                        long num = Long.parseLong(upper.replaceAll("[^0-9]", ""));
                        holder.tvTicket.setText(String.format(Locale.US, "SRO-%08d", num));
                    } catch (Exception e) {
                        holder.tvTicket.setText("SRO-" + upper);
                    }
                }
            } else {
                holder.tvTicket.setText("SRO-00002346");
            }

            holder.tvCustomer.setText(item.getCustomerName() != null ? item.getCustomerName() : "Customer Name");
            
            String serviceType = item.getUnitName();
            if (serviceType == null || serviceType.isEmpty()) serviceType = "Water dispenser service";
            holder.tvServiceType.setText(serviceType);

            String techName = item.getTechName();
            if (techName == null || techName.isEmpty()) techName = "Assigned technician";
            holder.tvTech.setText("Technician: " + techName);

            String dateStr = item.getDateTime();
            holder.tvDate.setText("Service Date: " + (dateStr != null && !dateStr.isEmpty() ? dateStr : "—"));

            String status = item.getStatus();
            if (status == null || status.isEmpty()) status = "Completed";
            holder.tvStatus.setText(status);

            if ("Completed".equalsIgnoreCase(status)) {
                holder.tvStatus.setBackgroundResource(R.drawable.bg_solid_green_pill);
                holder.tvStatus.setTextColor(Color.WHITE);
            } else if ("Cancelled".equalsIgnoreCase(status)) {
                holder.tvStatus.setBackgroundResource(R.drawable.bg_pdf_status_green_pill);
                holder.tvStatus.setTextColor(Color.parseColor("#EF4444"));
            } else {
                holder.tvStatus.setBackgroundResource(R.drawable.bg_pdf_status_green_pill);
                holder.tvStatus.setTextColor(Color.parseColor("#D97706"));
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTicket, tvCustomer, tvServiceType, tvTech, tvDate, tvStatus;

            ViewHolder(View v) {
                super(v);
                tvTicket = v.findViewById(R.id.auditTicketID);
                tvCustomer = v.findViewById(R.id.auditCustomerID);
                tvServiceType = v.findViewById(R.id.auditServiceType);
                tvTech = v.findViewById(R.id.auditTechName);
                tvDate = v.findViewById(R.id.auditUnitSRO);
                tvStatus = v.findViewById(R.id.auditStatus);
            }
        }
    }
}
