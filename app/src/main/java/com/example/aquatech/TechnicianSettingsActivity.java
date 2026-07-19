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
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseUserMetadata;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TechnicianSettingsActivity extends AppCompatActivity {

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
        
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
            notifRef = FirebaseDatabase.getInstance().getReference("Notifications").child(uid);
            
            // KONEK SA POPUP LISTENER
            setupRealtimePopupListener();
        }

        setupStatusBar();
        initializeViews();
    }

    private void setupRealtimePopupListener() {
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
            @Override public void onChildChanged(@NonNull DataSnapshot s, @Nullable String p) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot s) {}
            @Override public void onChildMoved(@NonNull DataSnapshot s, @Nullable String p) {}
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });

        // Sync flag para hindi mag-popup ang mga lumang notif
        notifRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) { isInitialLoad = false; }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void showItemNotificationPopup(NotificationModel notif) {
        // I-inflate ang mismong item_notification.xml mo
        View layout = getLayoutInflater().inflate(R.layout.item_notification, null);
        
        // Konek ang Views mula sa XML
        TextView tvMsg = layout.findViewById(R.id.tvNotifMessage);
        TextView tvTime = layout.findViewById(R.id.tvNotifTime);
        ImageView ivIcon = layout.findViewById(R.id.ivNotifIcon);
        
        // I-set ang data (may HTML support para sa bold text)
        if (tvMsg != null) tvMsg.setText(Html.fromHtml(notif.getMessage()));
        if (tvTime != null) tvTime.setText("Just now");
        if (ivIcon != null) ivIcon.setImageResource(getIconForType(notif.getType()));

        // I-display bilang Custom Toast sa taas (In-App Popup style)
        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP | Gravity.FILL_HORIZONTAL, 0, 100);
        toast.setView(layout);
        toast.show();
    }

    private int getIconForType(String type) {
        if (type == null) return R.drawable.maintenance_technician1;
        switch (type.toUpperCase()) {
            case "MESSAGE": return R.drawable.message_notification_1;
            case "REVISION": return R.drawable.alert_icon;
            case "APPROVED":
            case "ASSIGNED":
            default: return R.drawable.maintenance_technician1;
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
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Availability Toggle - Force ON
        SwitchMaterial swAvailability = findViewById(R.id.switchAvailability);
        if (swAvailability != null) {
            swAvailability.setChecked(true);
            swAvailability.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isChecked) {
                    swAvailability.setChecked(true);
                    Toast.makeText(this, "Status is active while logged in.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Navigation Links
        findViewById(R.id.btnTechAccount).setOnClickListener(v -> showTechAccountSecurityDialog());
        findViewById(R.id.btnTechDisplay).setOnClickListener(v -> showTechDisplayDialog());
        findViewById(R.id.btnTechPrivacy).setOnClickListener(v -> showTechPrivacyDialog());
        
        findViewById(R.id.btnTechNotification).setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationSettingsActivity.class);
            intent.putExtra("IS_TECH", true);
            startActivity(intent);
        });

        findViewById(R.id.btnTechHelp).setOnClickListener(v -> startActivity(new Intent(this, HelpCenterActivity.class)));
        findViewById(R.id.btnTechAbout).setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class)));
    }

    private void showTechDisplayDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_tech_display);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        View btnDone = dialog.findViewById(R.id.btnSaveTechDisplay);
        if (btnDone != null) btnDone.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showTechPrivacyDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_tech_privacy);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        View btnDone = dialog.findViewById(R.id.btnSaveTechPrivacy);
        if (btnDone != null) btnDone.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showTechAccountSecurityDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_tech_account_security);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvTechId = dialog.findViewById(R.id.tvTechUID);
        TextView tvStatus = dialog.findViewById(R.id.tvTechStatus);
        TextView tvPhone = dialog.findViewById(R.id.tvTechPhone);
        TextView tvSince = dialog.findViewById(R.id.tvTechSince);
        
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // DYNAMIC TECH ID
            if (tvTechId != null) {
                String email = user.getEmail() != null ? user.getEmail().toLowerCase() : "";
                tvTechId.setText(getTechIdFromEmail(email));
            }

            // REAL WORLD MEMBER SINCE
            if (tvSince != null) {
                FirebaseUserMetadata metadata = user.getMetadata();
                if (metadata != null) {
                    long creationTimestamp = metadata.getCreationTimestamp();
                    SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                    tvSince.setText(sdf.format(new Date(creationTimestamp)));
                }
            }

            // BACKEND SYNC
            if (userRef != null) {
                userRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String status = snapshot.child("verificationStatus").getValue(String.class);
                        if (tvStatus != null) {
                            if ("Verified".equalsIgnoreCase(status)) {
                                tvStatus.setText("Identity Verified");
                                tvStatus.setTextColor(Color.parseColor("#4CAF50"));
                            } else if ("Reviewing".equalsIgnoreCase(status)) {
                                tvStatus.setText("Pending Review");
                                tvStatus.setTextColor(Color.parseColor("#FFA000"));
                            } else {
                                tvStatus.setText("Unverified (Upload ID)");
                                tvStatus.setTextColor(Color.parseColor("#FF4B4B"));
                            }
                        }

                        String mobile = snapshot.child("mobileNumber").getValue(String.class);
                        if (tvPhone != null && mobile != null && !mobile.isEmpty()) {
                            tvPhone.setText(mobile);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
        }

        View identityLayout = dialog.findViewById(R.id.layoutTechIdentity);
        if (identityLayout != null) {
            identityLayout.setOnClickListener(v -> {
                dialog.dismiss();
                startActivity(new Intent(this, TechnicianVerificationActivity.class));
            });
        }

        View changePassLayout = dialog.findViewById(R.id.layoutTechChangePassword);
        if (changePassLayout != null) {
            changePassLayout.setOnClickListener(v -> {
                dialog.dismiss();
                startActivity(new Intent(this, ConfirmPasswordActivity.class));
            });
        }

        View btnDone = dialog.findViewById(R.id.btnDoneTechSecurity);
        if (btnDone != null) btnDone.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private String getTechIdFromEmail(String email) {
        if (email.contains("john@")) return "ASC-T001";
        if (email.contains("jhonny@")) return "ASC-T002";
        if (email.contains("ricky@")) return "ASC-T003";
        if (email.contains("ruel@")) return "ASC-T004";
        if (email.contains("glenda@")) return "ASC-T005";
        if (email.contains("jerry@")) return "ASC-T006";
        if (email.contains("jonnifer@")) return "ASC-T007";
        if (email.contains("marlon@")) return "ASC-T008";
        if (email.contains("selvino@")) return "ASC-T009";
        return "ASC-T100";
    }
}
