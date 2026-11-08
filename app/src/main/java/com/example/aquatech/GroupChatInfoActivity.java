package com.example.aquatech;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
            btnMute.setOnClickListener(v -> showMuteDialog());
        }

        View btnLeave = findViewById(R.id.rowLeaveChat);
        if (btnLeave != null) {
            btnLeave.setOnClickListener(v -> showLeaveChatDialog());
        }
    }

    private void showMuteDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_mute_chat);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.findViewById(R.id.btnCancelMute).setOnClickListener(v -> dialog.dismiss());

        dialog.findViewById(R.id.btnConfirmMute).setOnClickListener(v -> {
            Toast.makeText(this, "Team Chat Muted", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
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
