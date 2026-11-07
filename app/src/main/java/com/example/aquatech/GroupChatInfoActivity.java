package com.example.aquatech;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class GroupChatInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat_info);

        setupStatusBar();
        initializeViews();
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initializeViews() {
        findViewById(R.id.btnBackGroupInfo).setOnClickListener(v -> finish());

        View btnMembers = findViewById(R.id.rowSeeMembers);
        if (btnMembers != null) {
            btnMembers.setOnClickListener(v -> Toast.makeText(this, "12 Active Technicians in Team Chat", Toast.LENGTH_SHORT).show());
        }

        View btnMute = findViewById(R.id.rowMuteChat);
        if (btnMute != null) {
            btnMute.setOnClickListener(v -> Toast.makeText(this, "Notifications Muted for Team Chat", Toast.LENGTH_SHORT).show());
        }

        View btnLeave = findViewById(R.id.rowLeaveChat);
        if (btnLeave != null) {
            btnLeave.setOnClickListener(v -> showLeaveChatDialog());
        }
    }

    private void showLeaveChatDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Leave Technicians Team Chat?")
                .setMessage("Are you sure you want to leave the team channel?")
                .setPositiveButton("Leave", (dialog, which) -> finish())
                .setNegativeButton("Cancel", null)
                .show();
    }
}
