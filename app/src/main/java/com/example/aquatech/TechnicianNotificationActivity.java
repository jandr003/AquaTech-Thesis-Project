package com.example.aquatech;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

    private ImageView btnBack;
    private RecyclerView rvNew, rvOlder;
    private NotificationAdapter newAdapter, olderAdapter;
    private List<NotificationModel> newList, olderList;
    private View notifScroll;
    private LinearLayout noNotifLayout;

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
            dbRef = FirebaseDatabase.getInstance().getReference("Notifications").child(currentUid);
            connectToFirebase();
        } else {
            updateEmptyState(true);
        }
    }

    private void connectToFirebase() {
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<NotificationModel> allNotifs = new ArrayList<>();
                for (DataSnapshot data : snapshot.getChildren()) {
                    NotificationModel notif = data.getValue(NotificationModel.class);
                    if (notif != null) {
                        notif.setIconResId(getIconForType(notif.getType()));
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
        newList.clear();
        olderList.clear();

        // Top 3 as New, the rest go to Older
        for (int i = 0; i < all.size(); i++) {
            if (i < 3) newList.add(all.get(i));
            else olderList.add(all.get(i));
        }

        newAdapter.notifyDataSetChanged();
        olderAdapter.notifyDataSetChanged();

        // Hide labels if lists are empty
        findViewById(R.id.tvLabelNew).setVisibility(newList.isEmpty() ? View.GONE : View.VISIBLE);
        findViewById(R.id.tvLabelOlder).setVisibility(olderList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            noNotifLayout.setVisibility(View.VISIBLE);
            notifScroll.setVisibility(View.GONE);
        } else {
            noNotifLayout.setVisibility(View.GONE);
            notifScroll.setVisibility(View.VISIBLE);
        }
    }

    private int getIconForType(String type) {
        if (type == null) return R.drawable.maintenance_technician1;
        switch (type.toUpperCase()) {
            case "MESSAGE": return R.drawable.message_notification_1;
            case "REVISION": return R.drawable.alert_icon;
            case "APPROVED": return R.drawable.maintenance_technician1;
            case "ASSIGNED":
            default: return R.drawable.maintenance_technician1;
        }
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        rvNew = findViewById(R.id.rvNewNotifications);
        rvOlder = findViewById(R.id.rvOlderNotifications);
        notifScroll = findViewById(R.id.notifScroll);
        noNotifLayout = findViewById(R.id.noNotifLayout);

        btnBack.setOnClickListener(v -> finish());
        
        rvNew.setLayoutManager(new LinearLayoutManager(this));
        newList = new ArrayList<>();
        newAdapter = new NotificationAdapter(newList);
        rvNew.setAdapter(newAdapter);

        rvOlder.setLayoutManager(new LinearLayoutManager(this));
        olderList = new ArrayList<>();
        olderAdapter = new NotificationAdapter(olderList);
        rvOlder.setAdapter(olderAdapter);
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }
}
