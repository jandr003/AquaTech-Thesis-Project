package com.example.aquatech;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
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
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubmissionsActivity extends AppCompatActivity {

    private RecyclerView rvSubmissions;
    private TextView tvEmpty;
    private DatabaseReference dbRef;
    private List<ServiceLogModel> submissionList;
    private SubmissionAdapter adapter;
    private ValueEventListener submissionsListener;
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submissions);

        setupStatusBar();

        rvSubmissions = findViewById(R.id.rvSubmissions);
        tvEmpty = findViewById(R.id.tvEmptySubmission);
        findViewById(R.id.btnBackSubmission).setOnClickListener(v -> finish());

        rvSubmissions.setLayoutManager(new LinearLayoutManager(this));
        submissionList = new ArrayList<>();

        dbRef = FirebaseDatabase.getInstance(DB_URL).getReference("ServiceRequests");

        adapter = new SubmissionAdapter(submissionList, new SubmissionAdapter.OnActionClickListener() {
            @Override
            public void onApprove(String ticketId) {
                updateTicketStatus(ticketId, "Completed");
            }

            @Override
            public void onReject(String ticketId, String techUid, String techName) {
                rejectTicket(ticketId, techUid, techName);
            }

            @Override
            public void onEdit(String ticketId) {
                Intent intent = new Intent(SubmissionsActivity.this, ServiceReportDetailsActivity.class);
                intent.putExtra("TICKET_ID", ticketId);
                startActivity(intent);
            }
        });

        rvSubmissions.setAdapter(adapter);
        setupFirebaseListener();
    }

    private void setupFirebaseListener() {
        submissionsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                submissionList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String status = ds.child("status").getValue(String.class);
                    if ("Submission".equalsIgnoreCase(status) || "Submitted".equalsIgnoreCase(status)) {
                        String ticketId = ds.child("ticketId").getValue(String.class);
                        if (ticketId == null) ticketId = ds.getKey();

                        String techUid = ds.child("assignedTechId").getValue(String.class);
                        String techName = ds.child("assignedTechName").getValue(String.class);
                        if (techName == null) techName = "Unknown Technician";

                        String customerName = ds.child("customerName").getValue(String.class);
                        if (customerName == null) customerName = "Guest";

                        String serviceType = getServiceTypeFromSnapshot(ds);

                        ServiceLogModel model = new ServiceLogModel();
                        model.setSroNumber(ticketId);
                        model.setTechName(techName);
                        model.setTechUid(techUid);
                        model.setCustomerName(customerName);
                        model.setTechRole(serviceType);
                        model.setStatus("Submission");

                        submissionList.add(model);
                    }
                }

                tvEmpty.setVisibility(submissionList.isEmpty() ? View.VISIBLE : View.GONE);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SubmissionsActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        // 📍 BUG FIX: Use addValueEventListener on root reference, not a filtered query that might trigger recursive updates
        dbRef.addValueEventListener(submissionsListener);
    }

    private String getServiceTypeFromSnapshot(DataSnapshot ds) {
        List<String> categories = new ArrayList<>();
        if (hasQty(ds, "qty_wayvalve")) categories.add("Installation Kit");
        if (hasQty(ds, "qty_aquatal") || hasQty(ds, "qty_uvlamp") || hasQty(ds, "qty_touchpanel") || hasQty(ds, "qty_pbcboard"))
            categories.add("Other Parts");
        if (hasQty(ds, "qty_cbc") || hasQty(ds, "qty_sediment") || hasQty(ds, "qty_inline") || hasQty(ds, "qty_smsf1") || hasQty(ds, "qty_smsf10"))
            categories.add("Filter Preventive");

        if (categories.isEmpty()) return "General Service";
        if (categories.size() == 1) return categories.get(0);
        if (categories.size() == 2) return categories.get(0) + " & " + categories.get(1);
        return categories.get(0) + ", " + categories.get(1) + " and " + categories.get(2);
    }

    private boolean hasQty(DataSnapshot ds, String key) {
        Object val = ds.child(key).getValue();
        return val instanceof Number && ((Number) val).intValue() > 0;
    }

    private void updateTicketStatus(String ticketId, String newStatus) {
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String tid = ds.child("ticketId").getValue(String.class);
                    if (ticketId.equals(tid) || ticketId.equals(ds.getKey())) {

                        // Kunin ang techUid bago i-update
                        String techUid = ds.child("assignedTechId").getValue(String.class);
                        String techName = ds.child("assignedTechName").getValue(String.class);

                        ds.getRef().child("status").setValue(newStatus)
                                .addOnSuccessListener(aVoid -> {
                                    AdminDashboardActivity.addAdminLog("Admin " + newStatus + " ticket #" + ticketId);
                                    Toast.makeText(SubmissionsActivity.this, "Ticket " + newStatus, Toast.LENGTH_SHORT).show();

                                    // ✅ IDAGDAG ITO — notify technician na approved na
                                    if (techUid != null && !techUid.isEmpty()) {
                                        String message = "<b>Report Approved!</b><br>Your submission for ticket #"
                                                + ticketId + " has been approved. Great job!";
                                        sendNotificationToTechnician(techUid, techName, message, ticketId);
                                    }
                                });
                        break;
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void rejectTicket(String ticketId, String techUid, String techName) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "Rejected");
        updates.put("rejectedAt", ServerValue.TIMESTAMP);

        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String tid = ds.child("ticketId").getValue(String.class);
                    if (ticketId.equals(tid) || ticketId.equals(ds.getKey())) {
                        ds.getRef().updateChildren(updates).addOnSuccessListener(aVoid -> {
                            String message = "<b>Report Rejected</b><br>Your submission for ticket #" + ticketId + " was rejected. Tap to resubmit.";
                            sendNotificationToTechnician(techUid, techName, message, ticketId);
                            Toast.makeText(SubmissionsActivity.this, "Ticket rejected, technician notified", Toast.LENGTH_SHORT).show();
                        });
                        break;
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void sendNotificationToTechnician(String techUid, String techName, String message, String ticketId) {
        if (techUid == null || techUid.isEmpty()) return;
        DatabaseReference notifRef = FirebaseDatabase.getInstance(DB_URL).getReference("Notifications").child(techUid);
        String id = notifRef.push().getKey();
        if (id != null) {
            // Sa SubmissionsActivity approve path:
            NotificationModel notification = new NotificationModel(
                    id,
                    message,
                    System.currentTimeMillis(),
                    "APPROVED",  // ← hindi "REJECTED"
                    ticketId,
                    "Admin"
            );
            notifRef.child(id).setValue(notification);
        }
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbRef != null && submissionsListener != null) {
            dbRef.removeEventListener(submissionsListener);
        }
    }
}