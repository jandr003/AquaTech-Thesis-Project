package com.example.aquatech;

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

        View btnInvite = findViewById(R.id.btnActionInvite);
        if (btnInvite != null) {
            btnInvite.setOnClickListener(v -> {
                Intent intent = new Intent(this, GroupChatInviteActivity.class);
                startActivity(intent);
            });
        }

        View btnMembers = findViewById(R.id.rowSeeMembers);
        if (btnMembers != null) {
            btnMembers.setOnClickListener(v -> {
                Intent intent = new Intent(this, GroupChatMembersActivity.class);
                startActivity(intent);
            });
        }

        View btnNicknames = findViewById(R.id.btnActionNicknames);
        if (btnNicknames != null) {
            btnNicknames.setOnClickListener(v -> {
                Intent intent = new Intent(this, GroupChatNicknamesActivity.class);
                startActivity(intent);
            });
        }

        View btnSearch = findViewById(R.id.btnActionSearch);
        if (btnSearch != null) {
            btnSearch.setOnClickListener(v -> {
                Intent intent = new Intent(this, GroupChatSearchActivity.class);
                startActivity(intent);
            });
        }

        View btnCustomise = findViewById(R.id.btnActionCustomise);
        if (btnCustomise != null) {
            btnCustomise.setOnClickListener(v -> {
                Intent intent = new Intent(this, GroupChatCustomiseActivity.class);
                startActivity(intent);
            });
        }

        View btnInviteLink = findViewById(R.id.rowInviteLink);
        if (btnInviteLink != null) {
            btnInviteLink.setOnClickListener(v -> showInviteLinkDialog());
        }

        View btnMedia = findViewById(R.id.rowViewMedia);
        if (btnMedia != null) {
            btnMedia.setOnClickListener(v -> {
                Intent intent = new Intent(this, GroupSharedMediaActivity.class);
                startActivity(intent);
            });
        }

        View btnPinned = findViewById(R.id.rowPinnedMessages);
        if (btnPinned != null) {
            btnPinned.setOnClickListener(v -> {
                Intent intent = new Intent(this, PinnedMessagesActivity.class);
                startActivity(intent);
            });
        }

        View btnMute = findViewById(R.id.rowMuteChat);
        if (btnMute != null) {
            btnMute.setOnClickListener(v -> showMuteDialog());
        }

        View btnSounds = findViewById(R.id.rowNotificationsSounds);
        if (btnSounds != null) {
            btnSounds.setOnClickListener(v -> {
                Intent intent = new Intent(this, NotificationSoundsSettingsActivity.class);
                startActivity(intent);
            });
        }

        View btnReadReceipts = findViewById(R.id.rowReadReceipts);
        if (btnReadReceipts != null) {
            btnReadReceipts.setOnClickListener(v -> {
                Intent intent = new Intent(this, ReadReceiptsSettingsActivity.class);
                startActivity(intent);
            });
        }

        View btnTyping = findViewById(R.id.rowTypingIndicator);
        if (btnTyping != null) {
            btnTyping.setOnClickListener(v -> {
                Intent intent = new Intent(this, TypingIndicatorSettingsActivity.class);
                startActivity(intent);
            });
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
