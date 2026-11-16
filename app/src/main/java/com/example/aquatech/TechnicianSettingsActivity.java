package com.example.aquatech;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class TechnicianSettingsActivity extends AppCompatActivity {

    private static final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private SharedPreferences prefs;
    private FirebaseAuth mAuth;
    private DatabaseReference userRef, notifRef;
    private boolean isInitialLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_settings);

        prefs = getSharedPreferences("AquaTechPrefs_Tech", MODE_PRIVATE);
        mAuth = FirebaseAuth.getInstance();

        FirebaseUser user = mAuth != null ? mAuth.getCurrentUser() : null;
        if (user != null) {
            String uid = user.getUid();
            userRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(uid);
            notifRef = FirebaseDatabase.getInstance(DB_URL).getReference("Notifications").child(uid);

            setupRealtimePopupListener();
        }

        setupStatusBar();
        initializeViews();
    }

    private void setupRealtimePopupListener() {
        if (notifRef == null) {
            return;
        }

        notifRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (!isInitialLoad) {
                    NotificationModel notif = snapshot.getValue(NotificationModel.class);
                    if (notif != null) {
                        showItemNotificationPopup(notif);
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot s, @Nullable String p) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot s) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot s, @Nullable String p) {}

            @Override
            public void onCancelled(@NonNull DatabaseError e) {}
        });

        notifRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                isInitialLoad = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void showItemNotificationPopup(NotificationModel notif) {
        if (notif == null) {
            return;
        }

        View layout = getLayoutInflater().inflate(R.layout.item_notification, null);
        if (layout == null) {
            return;
        }

        TextView tvMsg = layout.findViewById(R.id.tvNotifMessage);
        TextView tvTime = layout.findViewById(R.id.tvNotifTime);
        ImageView ivIcon = layout.findViewById(R.id.ivNotifIcon);

        if (tvMsg != null && notif.getMessage() != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                tvMsg.setText(Html.fromHtml(notif.getMessage(), Html.FROM_HTML_MODE_LEGACY));
            } else {
                tvMsg.setText(Html.fromHtml(notif.getMessage()));
            }
        }

        if (tvTime != null) {
            tvTime.setText("Just now");
        }

        if (ivIcon != null) {
            ivIcon.setImageResource(getIconForType(notif.getType()));
        }

        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP | Gravity.FILL_HORIZONTAL, 0, 100);
        toast.setView(layout);
        toast.show();
    }

    private int getIconForType(String type) {
        if (type == null) {
            return R.drawable.maintenance_technician1;
        }

        switch (type.toUpperCase()) {
            case "MESSAGE":
                return R.drawable.message_notification_1;
            case "REVISION":
                return R.drawable.alert_icon;
            case "APPROVED":
            case "ASSIGNED":
            default:
                return R.drawable.maintenance_technician1;
        }
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initializeViews() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        SwitchMaterial swAvailability = findViewById(R.id.switchAvailability);
        if (swAvailability != null) {
            swAvailability.setChecked(true);
            swAvailability.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (userRef != null) {
                    userRef.child("isAvailable").setValue(isChecked);
                    userRef.child("dutyStatus").setValue(isChecked ? "Online / Available" : "Off-Duty");
                }
                Toast.makeText(this, isChecked ? "Status: Online & Available" : "Status: Off-Duty / On Break", Toast.LENGTH_SHORT).show();
            });
        }

        View btnDisplay = findViewById(R.id.btnTechDisplay);
        if (btnDisplay != null) {
            btnDisplay.setOnClickListener(v -> startActivity(new Intent(this, TechnicianDisplayActivity.class)));
        }

        View btnPrivacy = findViewById(R.id.btnTechPrivacy);
        if (btnPrivacy != null) {
            btnPrivacy.setOnClickListener(v -> startActivity(new Intent(this, TechnicianPrivacyActivity.class)));
        }

        View btnNotif = findViewById(R.id.btnTechNotification);
        if (btnNotif != null) {
            btnNotif.setOnClickListener(v -> {
                Intent intent = new Intent(this, NotificationSettingsActivity.class);
                intent.putExtra("IS_TECH", true);
                startActivity(intent);
            });
        }

        View btnHelp = findViewById(R.id.btnTechHelp);
        if (btnHelp != null) {
            btnHelp.setOnClickListener(v -> startActivity(new Intent(this, TechnicianHelpCenterActivity.class)));
        }

        View btnAbout = findViewById(R.id.btnTechAbout);
        if (btnAbout != null) {
            btnAbout.setOnClickListener(v -> startActivity(new Intent(this, TechnicianAboutActivity.class)));
        }
    }
}
