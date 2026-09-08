package com.example.aquatech;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TechnicianNotificationActivity extends AppCompatActivity {

    private static final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private ImageView btnBack, btnSettings;
    private RecyclerView rvToday, rvEarlier;
    private TechnicianNotificationAdapter todayAdapter, earlierAdapter;
    private List<NotificationModel> todayList, earlierList;
    private LinearLayout noNotifLayout;
    private TextView chipAll, chipReminders, chipPayment, chipBooking;

    private DatabaseReference dbRef;
    private FirebaseAuth mAuth;
    private boolean isInitialLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_notification);

        mAuth = FirebaseAuth.getInstance();
        
        setupStatusBar();
        initializeViews();

        if (mAuth.getCurrentUser() != null) {
            String currentUid = mAuth.getCurrentUser().getUid();
            dbRef = FirebaseDatabase.getInstance(DB_URL).getReference("Notifications").child(currentUid);
            connectToFirebase();
        } else {
            updateEmptyState(true);
        }
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        btnSettings = findViewById(R.id.btnSettings);
        rvToday = findViewById(R.id.rvTodayNotifications);
        rvEarlier = findViewById(R.id.rvEarlierNotifications);
        noNotifLayout = findViewById(R.id.noNotifLayout);

        chipAll = findViewById(R.id.chipAll);
        chipReminders = findViewById(R.id.chipReminders);
        chipPayment = findViewById(R.id.chipPayment);
        chipBooking = findViewById(R.id.chipBooking);

        btnBack.setOnClickListener(v -> finish());
        btnSettings.setOnClickListener(v -> {
            Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show();
        });

        rvToday.setLayoutManager(new LinearLayoutManager(this));
        todayList = new ArrayList<>();
        todayAdapter = new TechnicianNotificationAdapter(todayList);
        rvToday.setAdapter(todayAdapter);

        rvEarlier.setLayoutManager(new LinearLayoutManager(this));
        earlierList = new ArrayList<>();
        earlierAdapter = new TechnicianNotificationAdapter(earlierList);
        rvEarlier.setAdapter(earlierAdapter);

        setupFilterClickListeners();
    }

    private void setupFilterClickListeners() {
        View.OnClickListener filterListener = v -> {
            chipAll.setSelected(false);
            chipReminders.setSelected(false);
            chipPayment.setSelected(false);
            chipBooking.setSelected(false);
            v.setSelected(true);
            
            // Logic for actual filtering can be added here
            Toast.makeText(this, "Filtering...", Toast.LENGTH_SHORT).show();
        };

        chipAll.setOnClickListener(filterListener);
        chipReminders.setOnClickListener(filterListener);
        chipPayment.setOnClickListener(filterListener);
        chipBooking.setOnClickListener(filterListener);
    }

    private void connectToFirebase() {
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<NotificationModel> allNotifs = new ArrayList<>();
                for (DataSnapshot data : snapshot.getChildren()) {
                    NotificationModel notif = data.getValue(NotificationModel.class);
                    if (notif != null) {
                        allNotifs.add(notif);
                    }
                }

                Collections.reverse(allNotifs);

                if (allNotifs.isEmpty()) {
                    updateEmptyState(true);
                } else {
                    updateEmptyState(false);
                    distributeToSections(allNotifs);
                }

                isInitialLoad = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TechnicianNotificationActivity.this, "Sync Error!", Toast.LENGTH_SHORT).show();
            }
        });

        dbRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (!isInitialLoad) {
                    NotificationModel newNotif = snapshot.getValue(NotificationModel.class);
                    if (newNotif != null) {
                        String msg = Html.fromHtml(newNotif.getMessage()).toString();
                        Toast.makeText(TechnicianNotificationActivity.this, "🔔 NEW UPDATE: " + msg, Toast.LENGTH_LONG).show();
                    }
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot s, @Nullable String p) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot s) {}
            @Override public void onChildMoved(@NonNull DataSnapshot s, @Nullable String p) {}
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void distributeToSections(List<NotificationModel> all) {
        todayList.clear();
        earlierList.clear();

        long now = System.currentTimeMillis();
        long oneDayMillis = 24 * 60 * 60 * 1000;

        for (NotificationModel notif : all) {
            if (now - notif.getTimestamp() < oneDayMillis) {
                todayList.add(notif);
            } else {
                earlierList.add(notif);
            }
        }

        todayAdapter.notifyDataSetChanged();
        earlierAdapter.notifyDataSetChanged();

        findViewById(R.id.tvLabelToday).setVisibility(todayList.isEmpty() ? View.GONE : View.VISIBLE);
        findViewById(R.id.tvLabelEarlier).setVisibility(earlierList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            noNotifLayout.setVisibility(View.VISIBLE);
            findViewById(R.id.rvTodayNotifications).setVisibility(View.GONE);
            findViewById(R.id.rvEarlierNotifications).setVisibility(View.GONE);
            findViewById(R.id.tvLabelToday).setVisibility(View.GONE);
            findViewById(R.id.tvLabelEarlier).setVisibility(View.GONE);
        } else {
            noNotifLayout.setVisibility(View.GONE);
            findViewById(R.id.rvTodayNotifications).setVisibility(View.VISIBLE);
            findViewById(R.id.rvEarlierNotifications).setVisibility(View.VISIBLE);
        }
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            window.setStatusBarColor(Color.WHITE);
        }
    }
}
