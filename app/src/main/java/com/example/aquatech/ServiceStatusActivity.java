package com.example.aquatech;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class ServiceStatusActivity extends AppCompatActivity {

    private LinearLayout layoutArrived, layoutOngoing;
    private Button btnStatusAction;
    private TextView dot1, dot2, dot3, tvTicketId, locationMarker;
    private DotAnimationHelper dotHelper;
    private boolean isMaintenanceStarted = false;

    private DatabaseReference requestsRef;
    private ValueEventListener statusListener;
    private String ticketId;
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_status);

        ticketId = getIntent().getStringExtra("TICKET_ID");
        if (ticketId == null) {
            Toast.makeText(this, "Error: No ticket ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Point directly to this ticket
        requestsRef = FirebaseDatabase.getInstance(DB_URL).getReference("ServiceRequests").child(ticketId);

        initializeViews();
        setupStatusListener(); // 📍 Start real-time sync
    }

    private void initializeViews() {
        layoutArrived = findViewById(R.id.layoutArrived);
        layoutOngoing = findViewById(R.id.layoutOngoing);
        btnStatusAction = findViewById(R.id.btnStatusAction);
        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);
        dot3 = findViewById(R.id.dot3);
        tvTicketId = findViewById(R.id.tvTicketId);
        locationMarker = findViewById(R.id.LocationMarker);

        dotHelper = new DotAnimationHelper();

        if (tvTicketId != null) {
            tvTicketId.setText("Ticket #: " + ticketId);
        }

        btnStatusAction.setOnClickListener(v -> {
            if (!isMaintenanceStarted) {
                startMaintenance();
            } else {
                showConfirmationDialog();
            }
        });
    }

    private void setupStatusListener() {
        // 📍 Listening real-time to the status field
        statusListener = requestsRef.child("status").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String status = snapshot.getValue(String.class);
                if (status == null) return;

                if ("Arrived".equals(status)) {
                    showArrivedScreen();
                } else if ("Ongoing".equals(status)) {
                    showOngoingScreen();
                } else if ("Completed".equals(status) || "Submission".equals(status)) {
                    // Close the status activity if it's already done elsewhere
                    if (isMaintenanceStarted && "Submission".equals(status)) {
                        // handled by intent navigation in completeMaintenance()
                    } else {
                        finish();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("SYNC", "Status sync failed: " + error.getMessage());
            }
        });
    }

    private void showArrivedScreen() {
        isMaintenanceStarted = false;
        layoutArrived.setVisibility(View.VISIBLE);
        layoutOngoing.setVisibility(View.GONE);
        btnStatusAction.setText("START");
        if (locationMarker != null) locationMarker.setVisibility(View.VISIBLE);
    }

    private void showOngoingScreen() {
        isMaintenanceStarted = true;
        layoutArrived.setVisibility(View.GONE);
        layoutOngoing.setVisibility(View.VISIBLE);
        btnStatusAction.setText("NEXT");
        if (locationMarker != null) locationMarker.setVisibility(View.GONE);

        dotHelper.startPulseAnimation(dot1, dot2, dot3);
        ImageView maintenanceIcon = findViewById(R.id.MaintenanceIcon);
        if (maintenanceIcon != null) {
            dotHelper.startMaintenanceAnimation(maintenanceIcon);
        }
    }

    private void startMaintenance() {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Syncing with server...");
        pd.setCancelable(false);
        pd.show();

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "Ongoing");
        updates.put("maintenanceStartTime", ServerValue.TIMESTAMP);

        requestsRef.updateChildren(updates).addOnCompleteListener(task -> {
            pd.dismiss();
            if (task.isSuccessful()) {
                AdminDashboardActivity.addAdminLog("Technician started maintenance for ticket #" + ticketId);
                // Listener will trigger showOngoingScreen()
            } else {
                Toast.makeText(this, "Network Error: Failed to start service", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void completeMaintenance() {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Finalizing service...");
        pd.setCancelable(false);
        pd.show();

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "Submission");
        updates.put("maintenanceEndTime", ServerValue.TIMESTAMP);

        requestsRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (isFinishing() || isDestroyed()) {
                try { pd.dismiss(); } catch (Exception ignored) {}
                return;
            }
            pd.dismiss();
            if (task.isSuccessful()) {
                AdminDashboardActivity.addAdminLog("Technician finished maintenance for ticket #" + ticketId);

                // Immediate navigation to Dashboard for report submission
                Intent intent = new Intent(ServiceStatusActivity.this, TechnicianDashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);;
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Network Error: Failed to complete service", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showConfirmationDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_maintenance_complete);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        Button btnConfirm = dialog.findViewById(R.id.btnConfirmDialog);
        Button btnCancel = dialog.findViewById(R.id.btnCancelDialog);

        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                dialog.dismiss();
                completeMaintenance();
            });
        }
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 📍 Clean up listener to prevent ghost updates
        if (requestsRef != null && statusListener != null) {
            requestsRef.child("status").removeEventListener(statusListener);
        }
        if (dotHelper != null) dotHelper.stopAnimation();
    }
}