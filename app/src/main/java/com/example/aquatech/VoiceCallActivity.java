package com.example.aquatech;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

public class VoiceCallActivity extends AppCompatActivity {

    private TextView tvCallTimer, tvStatusLabel, tvTechNameDisplay, tvRoleLabel;
    private ImageView callAvatar, muteIcon, speakerIcon;
    private boolean isMuted = false, isSpeakerOn = false, isTimerRunning = false;
    private long syncedStartTime = 0L, serverTimeOffset = 0L;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private DatabaseReference callRef;
    private String techId, customerId, customerName;
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (syncedStartTime <= 0) {
                timerHandler.postDelayed(this, 500);
                return;
            }

            long currentTimeOnServer = System.currentTimeMillis() + serverTimeOffset;
            long millis = currentTimeOnServer - syncedStartTime;
            if (millis < 0) {
                millis = 0;
            }

            int seconds = (int) (millis / 1000);
            if (tvCallTimer != null) {
                tvCallTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60));
            }

            timerHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_call);

        techId = getIntent().getStringExtra("TECH_ID");
        customerId = getIntent().getStringExtra("CUSTOMER_ID");
        customerName = getIntent().getStringExtra("NAME");

        if (techId == null && FirebaseAuth.getInstance().getCurrentUser() != null) {
            techId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        FirebaseDatabase.getInstance(DB_URL).getReference(".info/serverTimeOffset")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot s) {
                        Long offset = s.getValue(Long.class);
                        if (offset != null) {
                            serverTimeOffset = offset;
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError e) {}
                });

        setupStatusBar();
        initViews();
        loadCustomerName();
        setupListeners();
        setupCallListener();
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initViews() {
        tvCallTimer = findViewById(R.id.tvCallTimer);
        tvStatusLabel = findViewById(R.id.ConnectedCall);
        callAvatar = findViewById(R.id.callAvatar);
        tvTechNameDisplay = findViewById(R.id.callTechName);
        tvRoleLabel = findViewById(R.id.tvAquaTechnician);
        muteIcon = findViewById(R.id.mutedMicroPhone);
        speakerIcon = findViewById(R.id.btnSpeakerIcon);

        if (tvCallTimer != null) {
            tvCallTimer.setText("00:00");
        }

        if (tvStatusLabel != null) {
            tvStatusLabel.setText("CALLING...");
        }

        if (muteIcon != null) {
            muteIcon.setColorFilter(isMuted ? Color.RED : Color.parseColor("#4D4D4D"));
        }

        if (tvRoleLabel != null) {
            tvRoleLabel.setText("Customer");
        }

        if (callAvatar != null) {
            Glide.with(this).load(R.drawable.man_user_circle_icon).circleCrop().into(callAvatar);
        }

        if (tvTechNameDisplay != null) {
            tvTechNameDisplay.setText(customerName != null ? customerName : "Connecting...");
        }
    }

    private void loadCustomerName() {
        if (customerName != null && !customerName.isEmpty()) {
            return;
        }

        if (customerId == null) {
            return;
        }

        FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(customerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot s) {
                        String name = s.child("fullName").getValue(String.class);
                        if (name == null) {
                            name = s.child("name").getValue(String.class);
                        }

                        if (name != null && tvTechNameDisplay != null) {
                            tvTechNameDisplay.setText(name);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void setupCallListener() {
        if (techId == null || customerId == null) {
            return;
        }

        String chatId = techId + "_" + customerId;
        callRef = FirebaseDatabase.getInstance(DB_URL).getReference("UserChats").child(chatId);
        callRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                if (!s.exists()) {
                    return;
                }

                String status = s.child("callStatus").getValue(String.class);
                if ("active".equalsIgnoreCase(status)) {
                    if (tvStatusLabel != null) {
                        tvStatusLabel.setText("CONNECTED");
                    }

                    Long start = s.child("callStartTime").getValue(Long.class);
                    if (start != null) {
                        syncedStartTime = start;
                        if (!isTimerRunning) {
                            isTimerRunning = true;
                            timerHandler.post(timerRunnable);
                        }
                    }
                } else if ("ended".equalsIgnoreCase(status)) {
                    finishCall();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void setupListeners() {
        View btnEndCallWrapper = findViewById(R.id.btnEndCallWrapper);
        if (btnEndCallWrapper != null) {
            btnEndCallWrapper.setOnClickListener(v -> {
                if (callRef != null) {
                    callRef.child("callStatus").setValue("ended");
                }
                finishCall();
            });
        }

        View btnMicrophoneWrapper = findViewById(R.id.btnMicrophoneWrapper);
        if (btnMicrophoneWrapper != null) {
            btnMicrophoneWrapper.setOnClickListener(v -> {
                isMuted = !isMuted;
                if (muteIcon != null) {
                    muteIcon.setColorFilter(isMuted ? Color.RED : Color.parseColor("#4D4D4D"));
                }
            });
        }

        View btnSpeakerWrapper = findViewById(R.id.btnSpeakerWrapper);
        if (btnSpeakerWrapper != null) {
            btnSpeakerWrapper.setOnClickListener(v -> {
                isSpeakerOn = !isSpeakerOn;
                if (speakerIcon != null) {
                    speakerIcon.setColorFilter(isSpeakerOn ? Color.parseColor("#37AAFD") : Color.parseColor("#4D4D4D"));
                }
            });
        }
    }

    private void finishCall() {
        timerHandler.removeCallbacks(timerRunnable);
        Toast.makeText(this, "Call Ended", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(timerRunnable);
    }
}
