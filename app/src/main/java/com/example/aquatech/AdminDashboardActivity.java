package com.example.aquatech;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvCompletedCount, tvInProgressCount, tvSubmissionCount, tvOverdueCount, tvAdminName;
    private DatabaseReference dbRef;
    private FirebaseAuth mAuth;

    private static final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);
        mAuth = FirebaseAuth.getInstance();
        
        setupStatusBar();
        initializeViews();
        setAdminIdentity();
        setupFirebase();
    }

    private void initializeViews() {
        tvCompletedCount = findViewById(R.id.tvCompletedCount);
        tvInProgressCount = findViewById(R.id.tvInProgressCount);
        tvSubmissionCount = findViewById(R.id.tvSubmissionCount);
        tvOverdueCount = findViewById(R.id.tvOverdueCount);
        tvAdminName = findViewById(R.id.tvAdminName);

        // Stats Cards (Original Click Listeners)
        findViewById(R.id.cardInProgress).setOnClickListener(v ->
                startActivity(new Intent(this, TrackTechniciansActivity.class))
        );
        findViewById(R.id.cardOpen).setOnClickListener(v -> startActivity(new Intent(this, OpenRequestsActivity.class)));
        findViewById(R.id.cardCompleted).setOnClickListener(v -> startActivity(new Intent(this, CompletedRequestsActivity.class)));
        findViewById(R.id.cardSubmissions).setOnClickListener(v -> startActivity(new Intent(this, SubmissionsActivity.class)));
        
        // Header
        findViewById(R.id.adminAvatar).setOnClickListener(v -> showAdminProfileDialog());

        // Quick Panels Listeners
        findViewById(R.id.cardActionTechPerf).setOnClickListener(v -> {
            startActivity(new Intent(this, TechnicianPerformanceListActivity.class));
        });

        findViewById(R.id.cardActionLogs).setOnClickListener(v -> 
                startActivity(new Intent(this, AdminLogsActivity.class))
        );
        
        findViewById(R.id.cardActionManageTechs).setOnClickListener(v ->
                startActivity(new Intent(this, ManageTechniciansActivity.class))
        );
    }

    private void setupFirebase() {
        dbRef = FirebaseDatabase.getInstance(DB_URL).getReference("ServiceRequests");
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;
                int completed = 0, inProgress = 0, openCount = 0, submission = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String status = ds.child("status").getValue(String.class);
                    String assignedTechName = ds.child("assignedTechName").getValue(String.class);


                    if (status != null) {
                        String s = status.trim().toLowerCase();
                        if (s.equals("completed")) completed++;
                        else if (s.equals("submission") || s.equals("submitted")) submission++; // ✅ Handle both internal and display status
                        else if (s.equals("in progress") || s.equals("ongoing") || s.equals("arrived")) inProgress++;
                        else if (s.equals("open") || s.equals("assigned")) {
                            if (assignedTechName != null && !assignedTechName.isEmpty()) inProgress++;
                            else openCount++;
                        }
                    }
                }
                if (tvCompletedCount != null) tvCompletedCount.setText(String.valueOf(completed));
                if (tvInProgressCount != null) tvInProgressCount.setText(String.valueOf(inProgress));
                if (tvOverdueCount != null) tvOverdueCount.setText(String.valueOf(openCount));
                if (tvSubmissionCount != null) tvSubmissionCount.setText(String.valueOf(submission));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public static void addAdminLog(String message) {
        try {
            DatabaseReference logRef = FirebaseDatabase.getInstance(DB_URL).getReference("AdminLogs").push();
            Map<String, Object> log = new HashMap<>();
            log.put("message", message);
            log.put("timestamp", ServerValue.TIMESTAMP);
            logRef.setValue(log);
        } catch (Exception e) {
            android.util.Log.e("LOG_ERROR", "Failed to add log: " + e.getMessage());
        }
    }

    private void showAdminProfileDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_admin_profile);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        
        TextView dialogEmail = dialog.findViewById(R.id.dialogEmail);
        TextView dialogName = dialog.findViewById(R.id.dialogName);
        String email = getAdminEmail();
        dialogEmail.setText(email);
        
        if (email.equals("management@aquasmartguard.ph")) dialogName.setText("Glenn Jean");
        else if (email.equals("admin@aquasmartguard.ph")) dialogName.setText("John Andrew");
        
        // Removed btnAdminLogs and btnManageTechs as they were removed from the XML layout

        View btnDialogSettings = dialog.findViewById(R.id.btnDialogSettings);
        if (btnDialogSettings != null) {
            btnDialogSettings.setOnClickListener(v -> {
                startActivity(new Intent(this, AdminSettingsActivity.class));
                dialog.dismiss();
            });
        }

        dialog.findViewById(R.id.btnMonthlyAnalytics).setOnClickListener(v -> { startActivity(new Intent(this, MonthlyAnalyticsActivity.class)); dialog.dismiss(); });
        dialog.findViewById(R.id.btnLogout).setOnClickListener(v -> { mAuth.signOut(); startActivity(new Intent(this, SplashActivity.class)); finishAffinity(); });
        dialog.findViewById(R.id.btnCloseProfileDialog).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private String getAdminEmail() {
        if (getIntent() != null && getIntent().hasExtra("USER_EMAIL")) return getIntent().getStringExtra("USER_EMAIL").toLowerCase().trim();
        FirebaseUser user = mAuth.getCurrentUser();
        return (user != null && user.getEmail() != null) ? user.getEmail().toLowerCase().trim() : "";
    }

    private void setAdminIdentity() {
        String email = getAdminEmail();
        if (email.equals("management@aquasmartguard.ph")) tvAdminName.setText("Glenn Jean");
        else if (email.equals("admin@aquasmartguard.ph")) tvAdminName.setText("John Andrew");
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }
}
