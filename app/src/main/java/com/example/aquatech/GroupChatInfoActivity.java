package com.example.aquatech;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

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
        View btnBack = findViewById(R.id.btnBackGroupInfo);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View btnMembers = findViewById(R.id.rowSeeMembers);
        if (btnMembers != null) {
            btnMembers.setOnClickListener(v -> {
                startActivity(new Intent(this, GroupChatMembersActivity.class));
            });
        }

        View btnInviteLink = findViewById(R.id.rowInviteLink);
        if (btnInviteLink != null) {
            btnInviteLink.setOnClickListener(v -> showInviteLinkDialog());
        }

        View btnMedia = findViewById(R.id.rowViewMedia);
        if (btnMedia != null) {
            btnMedia.setOnClickListener(v -> startActivity(new Intent(this, GroupSharedMediaActivity.class)));
        }

        View btnPinned = findViewById(R.id.rowPinnedMessages);
        if (btnPinned != null) {
            btnPinned.setOnClickListener(v -> startActivity(new Intent(this, PinnedMessagesActivity.class)));
        }

        View btnMute = findViewById(R.id.rowMuteChat);
        if (btnMute != null) {
            btnMute.setOnClickListener(v -> showMuteDialog());
        }

        View btnSounds = findViewById(R.id.rowNotificationsSounds);
        if (btnSounds != null) {
            btnSounds.setOnClickListener(v -> startActivity(new Intent(this, NotificationSoundsSettingsActivity.class)));
        }

        View btnReadReceipts = findViewById(R.id.rowReadReceipts);
        if (btnReadReceipts != null) {
            btnReadReceipts.setOnClickListener(v -> startActivity(new Intent(this, ReadReceiptsSettingsActivity.class)));
        }
    }

    private void showInviteLinkDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_invitation_link);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        String teamLink = "https://aquatech.smartguard.ph/join/team-chat-" + currentYear;

        TextView tvLink = dialog.findViewById(R.id.tvInviteLinkUrl);
        if (tvLink != null) {
            tvLink.setText(teamLink);
        }

        View btnClose = dialog.findViewById(R.id.btnCloseInviteDialog);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        View btnCopy = dialog.findViewById(R.id.btnCopyInviteLink);
        if (btnCopy != null) {
            btnCopy.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Team Chat Link", teamLink);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                }
                Toast.makeText(this, "Invitation link copied to clipboard!", Toast.LENGTH_SHORT).show();
            });
        }

        dialog.show();
    }

    private void showMuteDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_mute_chat);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        View btnCancel = dialog.findViewById(R.id.btnCancelMute);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        View btnConfirm = dialog.findViewById(R.id.btnConfirmMute);
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                Toast.makeText(this, "Team Chat Muted", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        }

        dialog.show();
    }
}
