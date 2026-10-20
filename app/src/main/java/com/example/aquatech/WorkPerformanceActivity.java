package com.example.aquatech;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class WorkPerformanceActivity extends AppCompatActivity {

    private TextView tvTechName, tvTotalTasks, tvPerfMessage;
    private TextView tvMonth1Name, tvMonth1Tasks, tvMonth2Name, tvMonth2Tasks, tvMonth3Name, tvMonth3Tasks;
    private ProgressBar pbMonth1, pbMonth2, pbMonth3;
    private CircularProgressIndicator pbTotalChart;

    private DatabaseReference requestsRef;
    private FirebaseAuth mAuth;
    private String currentTechUid;

    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_work_performance);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentTechUid = mAuth.getCurrentUser().getUid();
        requestsRef = FirebaseDatabase.getInstance(DB_URL).getReference("ServiceRequests");

        setupStatusBar();
        initializeViews();
        fetchPerformanceData();
        fetchTechIdentity();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView techBottomNav = findViewById(R.id.techBottomNav);
        if (techBottomNav != null) {
            techBottomNav.setSelectedItemId(R.id.nav_tech_performance);
        }
    }

    private void initializeViews() {
        tvTechName = findViewById(R.id.tvTechName);
        tvTotalTasks = findViewById(R.id.tvTotalTasks);
        tvPerfMessage = findViewById(R.id.tvPerfMessage);
        pbTotalChart = findViewById(R.id.pbTotalChart);

        tvMonth1Name = findViewById(R.id.tvMonth1Name);
        tvMonth1Tasks = findViewById(R.id.tvMonth1Tasks);
        pbMonth1 = findViewById(R.id.pbMonth1);

        tvMonth2Name = findViewById(R.id.tvMonth2Name);
        tvMonth2Tasks = findViewById(R.id.tvMonth2Tasks);
        pbMonth2 = findViewById(R.id.pbMonth2);

        tvMonth3Name = findViewById(R.id.tvMonth3Name);
        tvMonth3Tasks = findViewById(R.id.tvMonth3Tasks);
        pbMonth3 = findViewById(R.id.pbMonth3);

        setupBottomNav();

        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        Calendar cal = Calendar.getInstance();

        tvMonth1Name.setText(monthFormat.format(cal.getTime()).toUpperCase());
        cal.add(Calendar.MONTH, -1);
        tvMonth2Name.setText(monthFormat.format(cal.getTime()).toUpperCase());
        cal.add(Calendar.MONTH, -1);
        tvMonth3Name.setText(monthFormat.format(cal.getTime()).toUpperCase());
    }

    private void fetchTechIdentity() {
        if (mAuth.getCurrentUser() == null) return;
        String userEmail = mAuth.getCurrentUser().getEmail();
        TechnicianIdentityHelper.fetchTechnicianIdentity(DB_URL, currentTechUid, userEmail, (uid, resolvedName) -> {
            if (tvTechName != null && resolvedName != null && !resolvedName.trim().isEmpty()) {
                tvTechName.setText(resolvedName);
            }
        });
    }

    private void fetchPerformanceData() {
        requestsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int totalCount = 0;
                int m1Count = 0, m2Count = 0, m3Count = 0;

                Calendar cal = Calendar.getInstance();
                int currentMonth = cal.get(Calendar.MONTH);
                int currentYear = cal.get(Calendar.YEAR);

                cal.add(Calendar.MONTH, -1);
                int lastMonth = cal.get(Calendar.MONTH);
                int lastYear = cal.get(Calendar.YEAR);

                cal.add(Calendar.MONTH, -1);
                int twoMonthsAgo = cal.get(Calendar.MONTH);
                int twoYearsAgo = cal.get(Calendar.YEAR);

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String assignedTechId = ds.child("assignedTechId").getValue(String.class);
                    String status = ds.child("status").getValue(String.class);

                    if (assignedTechId != null && assignedTechId.equals(currentTechUid) &&
                            "Completed".equalsIgnoreCase(status)) {

                        totalCount++;

                        Long timestamp = ds.child("completedTimestamp").getValue(Long.class);
                        if (timestamp == null) {
                            timestamp = ds.child("maintenanceEndTime").getValue(Long.class);
                        }
                        if (timestamp == null) {
                            timestamp = ds.child("timestamp").getValue(Long.class);
                        }

                        if (timestamp != null) {
                            Calendar taskCal = Calendar.getInstance();
                            taskCal.setTimeInMillis(timestamp);
                            int tMonth = taskCal.get(Calendar.MONTH);
                            int tYear = taskCal.get(Calendar.YEAR);

                            if (tMonth == currentMonth && tYear == currentYear) {
                                m1Count++;
                            } else if (tMonth == lastMonth && tYear == lastYear) {
                                m2Count++;
                            } else if (tMonth == twoMonthsAgo && tYear == twoYearsAgo) {
                                m3Count++;
                            }
                        }
                    }
                }

                updateUI(totalCount, m1Count, m2Count, m3Count);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(WorkPerformanceActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(int total, int m1, int m2, int m3) {
        tvTotalTasks.setText(String.valueOf(m1));
        if (pbTotalChart != null) {
            pbTotalChart.setMax(30);
            pbTotalChart.setProgress(Math.min(m1, 30));
        }

        tvMonth1Tasks.setText(m1 + " tasks");
        tvMonth2Tasks.setText(m2 + " tasks");
        tvMonth3Tasks.setText(m3 + " tasks");

        int targetTasksPerMonth = 30;
        pbMonth1.setMax(targetTasksPerMonth);
        pbMonth1.setProgress(Math.min(m1, targetTasksPerMonth));

        pbMonth2.setMax(targetTasksPerMonth);
        pbMonth2.setProgress(Math.min(m2, targetTasksPerMonth));

        pbMonth3.setMax(targetTasksPerMonth);
        pbMonth3.setProgress(Math.min(m3, targetTasksPerMonth));

        if (m1 == 0) {
            tvPerfMessage.setText("LET'S GET STARTED THIS MONTH!");
        } else if (m1 <= 5) {
            tvPerfMessage.setText("GOOD START! KEEP PUSHING!");
        } else if (m1 <= 15) {
            tvPerfMessage.setText("EXCELLENT WORK! YOU'RE ON FIRE!");
        } else {
            tvPerfMessage.setText("LEGENDARY PERFORMANCE! GREAT JOB!");
        }
    }

    private void setupBottomNav() {
        BottomNavigationView techBottomNav = findViewById(R.id.techBottomNav);
        if (techBottomNav != null) {
            techBottomNav.setSelectedItemId(R.id.nav_tech_performance);
            techBottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_tech_dashboard) {
                    Intent intent = new Intent(this, TechnicianDashboardActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (id == R.id.nav_tech_messages) {
                    startActivity(new Intent(this, CustomerChatListActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_tech_profile) {
                    startActivity(new Intent(this, EditProfileActivity.class));
                    finish();
                    return true;
                }
                return true;
            });
        }
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }
}