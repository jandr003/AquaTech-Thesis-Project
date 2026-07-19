package com.example.aquatech;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

public class CustomerVoiceCallActivity extends AppCompatActivity {

    private TextView tvCallTimer, tvName, tvStatus, tvRoleLabel;
    private ImageView callAvatar, ivMuteIcon, ivSpeakerIcon;
    private FrameLayout btnMute, btnSpeaker, btnEndCall;
    private boolean isMuted = false, isSpeakerOn = false, isTimerRunning = false;
    private long syncedStartTime = 0L, serverTimeOffset = 0L;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private DatabaseReference callRef;
    private ValueEventListener callListener;
    private String techId, customerId;
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (syncedStartTime <= 0) {
                timerHandler.postDelayed(this, 500);
                return;
            }
            long currentTimeOnServer = System.currentTimeMillis() + serverTimeOffset;
            long millis = currentTimeOnServer - syncedStartTime;
            if (millis < 0) millis = 0;
            int seconds = (int) (millis / 1000);
            tvCallTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60));
            timerHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_voice_call);

        techId = getIntent().getStringExtra("TECH_ID");
        customerId = getIntent().getStringExtra("CUSTOMER_ID");

        FirebaseDatabase.getInstance(DB_URL).getReference(".info/serverTimeOffset")
                .addValueEventListener(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot s) {
                        Long offset = s.getValue(Long.class);
                        if (offset != null) serverTimeOffset = offset;
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });

        setupStatusBar();
        initViews();
        setupListeners();
        setupCallListener();
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initViews() {
        tvCallTimer = findViewById(R.id.tvCallTimer);
        tvName = findViewById(R.id.callCustomerName);
        tvStatus = findViewById(R.id.inCall01);
        callAvatar = findViewById(R.id.callAvatar);
        tvRoleLabel = findViewById(R.id.tvAquaCustomer); 
        
        btnMute = findViewById(R.id.btnMicrophoneWrapper);
        btnSpeaker = findViewById(R.id.btnSpeakerWrapper);
        btnEndCall = findViewById(R.id.btnEndCallWrapper);
        ivMuteIcon = findViewById(R.id.mutedMicroPhone);
        ivSpeakerIcon = findViewById(R.id.btnSpeakerIcon);

        if (tvCallTimer != null) tvCallTimer.setText("00:00");
        if (tvStatus != null) tvStatus.setText("CALLING...");

        String role = getIntent().getStringExtra("ROLE");
        if (role == null) role = "Technician"; 
        if (tvRoleLabel != null) tvRoleLabel.setText(role);

        String name = getIntent().getStringExtra("NAME");
        if (name != null && tvName != null) {
            tvName.setText(name);
        }

        String avatarUrl = getIntent().getStringExtra("AVATAR_URL");
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(this).load(avatarUrl).circleCrop().into(callAvatar);
        } else {
            int defaultIcon = "Customer".equalsIgnoreCase(role) ? R.drawable.man_customer_icon : R.drawable.new_technician;
            Glide.with(this).load(defaultIcon).circleCrop().into(callAvatar);
        }
    }

    private void setupCallListener() {
        if (techId == null || customerId == null) return;
        String chatId = techId + "_" + customerId;
        callRef = FirebaseDatabase.getInstance(DB_URL).getReference("UserChats").child(chatId);
        
        callListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                if (!s.exists()) return;
                String status = s.child("callStatus").getValue(String.class);
                Log.d("CALL_STATUS", "Status updated: " + status);

                if ("active".equalsIgnoreCase(status)) {
                    if (tvStatus != null) tvStatus.setText("CONNECTED");
                    Long start = s.child("callStartTime").getValue(Long.class);
                    if (start != null) {
                        syncedStartTime = start;
                        if (!isTimerRunning) {
                            isTimerRunning = true;
                            timerHandler.post(timerRunnable);
                        }
                    }
                } else if ("ended".equalsIgnoreCase(status) || status == null) {
                    finishCall();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        };
        callRef.addValueEventListener(callListener);
    }

    private void finishCall() {
        timerHandler.removeCallbacks(timerRunnable);
        if (callRef != null && callListener != null) {
            callRef.removeEventListener(callListener);
        }
        if (!isFinishing()) {
            Toast.makeText(this, "Call Ended", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupListeners() {
        if (btnEndCall != null) {
            btnEndCall.setOnClickListener(v -> {
                if (callRef != null) {
                    // Update status to ended to notify the other party
                    callRef.child("callStatus").setValue("ended");
                    callRef.child("callStartTime").removeValue();
                }
                finishCall();
            });
        }
        if (btnMute != null) {
            btnMute.setOnClickListener(v -> {
                isMuted = !isMuted;
                if (ivMuteIcon != null) ivMuteIcon.setColorFilter(isMuted ? Color.RED : Color.parseColor("#4D4D4D"));
            });
        }
        if (btnSpeaker != null) {
            btnSpeaker.setOnClickListener(v -> {
                isSpeakerOn = !isSpeakerOn;
                if (ivSpeakerIcon != null) ivSpeakerIcon.setColorFilter(isSpeakerOn ? Color.parseColor("#37AAFD") : Color.parseColor("#4D4D4D"));
            });
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(timerRunnable);
        if (callRef != null && callListener != null) {
            callRef.removeEventListener(callListener);
        }
    }
}
