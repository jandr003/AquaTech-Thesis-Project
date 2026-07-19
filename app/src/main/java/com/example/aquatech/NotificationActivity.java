package com.example.aquatech;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NotificationHistoryAdapter adapter;
    private List<NotificationModel> notificationList;
    private DatabaseReference dbRef;
    private FirebaseAuth mAuth;

    private static final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        setupStatusBar();
        mAuth = FirebaseAuth.getInstance();

        recyclerView = findViewById(R.id.notificationsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificationList = new ArrayList<>();
        adapter = new NotificationHistoryAdapter(this, notificationList);
        recyclerView.setAdapter(adapter);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        if (mAuth.getCurrentUser() != null) {
            dbRef = FirebaseDatabase.getInstance(DB_URL).getReference("CustomerNotifications")
                    .child(mAuth.getCurrentUser().getUid());
            loadNotifications();
        }
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void loadNotifications() {
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                notificationList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    NotificationModel model = ds.getValue(NotificationModel.class);
                    if (model != null) {
                        notificationList.add(model);
                    }
                }
                Collections.sort(notificationList, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(NotificationActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Helper method to add a notification to Firebase.
     * Can be called from anywhere in the app.
     */
    public static void addNotification(String userId, String message, String type, String ticketId) {
        DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL).getReference("CustomerNotifications").child(userId);
        String id = ref.push().getKey();
        if (id != null) {
            NotificationModel notification = new NotificationModel(id, message, System.currentTimeMillis(), type, ticketId);
            ref.child(id).setValue(notification);
        }
    }
}