package com.example.aquatech;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
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

import java.util.HashMap;
import java.util.Map;

public class IncomingCallActivity extends AppCompatActivity {

    private TextView tvCallerName, tvCallerRole;
    private ImageView ivCallerAvatar;
    private View btnAnswerCall, btnDeclineCall, btnQuickMessage, btnRemindMe;

    private String callerName, callerRole, callerId, myUid, ticketId;
    private boolean isTech = false;
    private DatabaseReference callRef;
    private ValueEventListener callStatusListener;
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Turn screen on and show over lock screen
        setupLockscreenAndStatusBar();

        setContentView(R.layout.activity_incoming_call);

        parseIntentData();
        initializeViews();
        setupListeners();
        setupCallStatusMonitor();
    }

    private void setupLockscreenAndStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            );
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void parseIntentData() {
        Intent intent = getIntent();
        if (intent == null) return;

        callerName = intent.getStringExtra("CALLER_NAME");
        callerRole = intent.getStringExtra("CALLER_ROLE");
        callerId = intent.getStringExtra("CALLER_ID");
        myUid = intent.getStringExtra("MY_UID");
        ticketId = intent.getStringExtra("TICKET_ID");
        isTech = intent.getBooleanExtra("IS_TECH", false);

        if (callerName == null) callerName = "Service Partner";
        if (callerRole == null) callerRole = isTech ? "Customer" : "AquaTech Technician";
    }

    private void initializeViews() {
        tvCallerName = findViewById(R.id.tvCallerName);
        tvCallerRole = findViewById(R.id.tvCallerRole);
        ivCallerAvatar = findViewById(R.id.ivCallerAvatar);

        btnAnswerCall = findViewById(R.id.btnAnswerCall);
        btnDeclineCall = findViewById(R.id.btnDeclineCall);
        btnQuickMessage = findViewById(R.id.btnQuickMessage);
        btnRemindMe = findViewById(R.id.btnRemindMe);

        if (tvCallerName != null) tvCallerName.setText(callerName);
        if (tvCallerRole != null) tvCallerRole.setText(callerRole);

        int defaultIcon = "Customer".equalsIgnoreCase(callerRole) ? R.drawable.man_customer_icon : R.drawable.new_technician;
        if (ivCallerAvatar != null) {
            Glide.with(this).load(defaultIcon).circleCrop().into(ivCallerAvatar);
        }
    }

    private void setupListeners() {
        btnAnswerCall.setOnClickListener(v -> answerCall());
        btnDeclineCall.setOnClickListener(v -> declineCall());

        btnQuickMessage.setOnClickListener(v -> {
            declineCall();
            Toast.makeText(this, "Quick message sent", Toast.LENGTH_SHORT).show();
        });

        btnRemindMe.setOnClickListener(v -> {
            declineCall();
            Toast.makeText(this, "Call reminder set for 15 mins", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupCallStatusMonitor() {
        if (callerId == null || myUid == null) return;
        String chatId = isTech ? (myUid + "_" + callerId) : (callerId + "_" + myUid);
        callRef = FirebaseDatabase.getInstance(DB_URL).getReference("UserChats").child(chatId);

        callStatusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String status = snapshot.child("callStatus").getValue(String.class);
                    if ("ended".equalsIgnoreCase(status)) {
                        finish();
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        callRef.addValueEventListener(callStatusListener);
    }

    private void answerCall() {
        if (callRef != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("callStatus", "active");
            updates.put("callStartTime", ServerValue.TIMESTAMP);
            callRef.updateChildren(updates);
        }

        Intent callIntent;
        if (isTech) {
            callIntent = new Intent(this, VoiceCallActivity.class);
            callIntent.putExtra("TECH_ID", myUid);
            callIntent.putExtra("CUSTOMER_ID", callerId);
            callIntent.putExtra("NAME", callerName);
        } else {
            callIntent = new Intent(this, CustomerVoiceCallActivity.class);
            callIntent.putExtra("TECH_ID", callerId);
            callIntent.putExtra("CUSTOMER_ID", myUid);
            callIntent.putExtra("NAME", callerName);
        }
        startActivity(callIntent);
        finish();
    }

    private void declineCall() {
        if (callRef != null) {
            callRef.child("callStatus").setValue("ended");
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (callRef != null && callStatusListener != null) {
            callRef.removeEventListener(callStatusListener);
        }
    }
}