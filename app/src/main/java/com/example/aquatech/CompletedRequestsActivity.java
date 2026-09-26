package com.example.aquatech;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CompletedRequestsActivity extends AppCompatActivity {

    private RecyclerView rvCompletedRequests;
    private TextView tvEmptyCompleted;
    private DatabaseReference requestsRef;
    private List<ServiceLogModel> completedList;
    private CompletedRequestAdapter adapter;
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completed_requests);

        setupStatusBar();

        rvCompletedRequests = findViewById(R.id.rvCompletedRequests);
        tvEmptyCompleted = findViewById(R.id.tvEmptyCompleted);
        findViewById(R.id.btnBackCompleted).setOnClickListener(v -> finish());

        rvCompletedRequests.setLayoutManager(new LinearLayoutManager(this));
        completedList = new ArrayList<>();
        adapter = new CompletedRequestAdapter(completedList);
        rvCompletedRequests.setAdapter(adapter);

        requestsRef = FirebaseDatabase.getInstance(DB_URL).getReference("ServiceRequests");
        loadCompletedRequests();
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void loadCompletedRequests() {
        requestsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                completedList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String status = ds.child("status").getValue(String.class);

                    if (status != null && status.equalsIgnoreCase("Completed")) {
                        String customerName = ds.child("customerName").getValue(String.class);
                        if (customerName == null || customerName.isEmpty()) {
                            customerName = ds.child("name").getValue(String.class);
                        }
                        
                        String ticketId = ds.child("ticketId").getValue(String.class);
                        if (ticketId == null) ticketId = ds.getKey();

                        String serviceType = getServiceTypeLabel(ds);
                        String address = ds.child("address").getValue(String.class);
                        if (address == null) address = ds.child("customerAddress").getValue(String.class);

                        String completionTime = "Date not available";
                        Long timestamp = ds.child("completedTimestamp").getValue(Long.class);
                        if (timestamp != null) {
                            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy | hh:mm a", Locale.getDefault());
                            completionTime = sdf.format(new Date(timestamp));
                        }

                        String rawRemarks = ds.child("remarks").getValue(String.class);
                        String[] classification = classifyServiceRequest(rawRemarks, ds);

                        ServiceLogModel model = new ServiceLogModel();
                        model.setSroNumber(ticketId);
                        model.setCustomerName(customerName != null ? customerName : "Guest Customer");
                        model.setTechRole(classification[0]);
                        
                        String displayDetails = classification[1] + " — " + classification[2];
                        if (rawRemarks != null && !rawRemarks.trim().isEmpty()) {
                            displayDetails = rawRemarks + " (" + classification[1] + ")";
                        }
                        model.setRemarks(displayDetails);
                        
                        String purchaseType = ds.child("purchaseType").getValue(String.class);
                        model.setPurchaseType(purchaseType != null ? purchaseType : "SUBSCRIPTION");

                        model.setDateTime(completionTime);
                        model.setAddress(address != null ? address : "No address provided");
                        model.setStatus("Completed");

                        completedList.add(model);
                    }
                }

                tvEmptyCompleted.setVisibility(completedList.isEmpty() ? View.VISIBLE : View.GONE);
                adapter.notifyDataSetChanged();
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private boolean matchKeywords(String text, String... keywords) {
        if (text == null || text.isEmpty()) return false;
        String lower = text.toLowerCase(Locale.ROOT);
        for (String kw : keywords) {
            if (lower.contains(kw.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private String[] classifyServiceRequest(String remarks, DataSnapshot ds) {
        String p = (remarks != null ? remarks : "") + " " + getServiceTypeLabel(ds);

        String serviceCategory = "General Service";
        String serviceDesc = "Technical system inspection and evaluation.";
        String concernSummary = "General System Check";

        if (matchKeywords(p, "leak", "tulo", "baha", "basâ", "dripping", "leakage", "broken", "damage")) {
            serviceCategory = "Technical Repair";
            serviceDesc = "Onsite technical repair and parts verification.";
            concernSummary = "System Leakage & Repair";
        } else if (matchKeywords(p, "power", "board", "bukas", "patay", "electricity", "fuse", "dead", "error", "faulty")) {
            serviceCategory = "Technical Repair";
            serviceDesc = "Electrical system troubleshooting and repair.";
            concernSummary = "Electrical & Power Issue";
        } else if (matchKeywords(p, "mahina", "slow", "barado", "low pressure", "clogged", "no water")) {
            serviceCategory = "Technical Repair";
            serviceDesc = "Flow optimization and system restoration.";
            concernSummary = "Flow Optimization & Restoration";
        } else if (matchKeywords(p, "amoy", "mabaho", "malabo", "lasa", "filter", "odor", "smell", "stink", "dirty", "taste", "maintenance", "cleaning", "check")) {
            serviceCategory = "System Maintenance";
            serviceDesc = "Scheduled preventive maintenance and health check.";
            concernSummary = "Water Quality & Filter Care";
        } else if (matchKeywords(p, "install", "lipat", "kabit", "setup", "move", "relocate")) {
            serviceCategory = "Unit Installation";
            serviceDesc = "Professional unit setup and system calibration.";
            concernSummary = "Professional Unit Setup";
        }

        return new String[]{serviceCategory, concernSummary, serviceDesc};
    }

    private String getServiceTypeLabel(DataSnapshot ds) {
        StringBuilder sb = new StringBuilder();
        if (hasQty(ds, "qty_cbc") || hasQty(ds, "qty_sediment") || hasQty(ds, "qty_smsf1") || hasQty(ds, "qty_smsf10")) sb.append("Filter Preventive");
        if (hasQty(ds, "qty_wayvalve")) { if (sb.length() > 0) sb.append(", "); sb.append("Installation Kit"); }
        if (hasQty(ds, "qty_aquatal") || hasQty(ds, "qty_inline") || hasQty(ds, "qty_uvlamp") || hasQty(ds, "qty_touchpanel") || hasQty(ds, "qty_pbcboard")) { if (sb.length() > 0) sb.append(", "); sb.append("Other Parts"); }
        return sb.toString().isEmpty() ? "General Maintenance" : sb.toString();
    }

    private boolean hasQty(DataSnapshot ds, String key) {
        Object val = ds.child(key).getValue();
        return val instanceof Number && ((Number) val).intValue() > 0;
    }
}
