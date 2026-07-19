package com.example.aquatech;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("AquaTechPrefs", MODE_PRIVATE);
        mAuth = FirebaseAuth.getInstance();
        
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
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Account & Security Dialog
        setupClick(R.id.yourAccSec, R.id.arrowUp, this::showAccountSecurityDialog);
        
        // Settings Activities
        setupClick(R.id.yourChat, R.id.arrowUp06, () -> startActivity(new Intent(this, ChatSettingsActivity.class)));
        setupClick(R.id.yourNotification, R.id.arrowUp07, () -> startActivity(new Intent(this, NotificationSettingsActivity.class)));
        setupClick(R.id.yourValidID, R.id.arrowUp02, () -> startActivity(new Intent(this, ValidIDActivity.class)));
        
        // Preference Dialogs
        setupClick(R.id.yourLanguage, R.id.arrowUp03, this::showLanguageDialog);
        setupClick(R.id.yourDisplay, R.id.arrowUp04, this::showDisplayDialog);
        setupClick(R.id.yourPrivacySettings, R.id.arrowUp05, this::showPrivacyDialog);

        // Support & Info Links
        setupClick(R.id.yourHelp, R.id.arrowUp08, () -> startActivity(new Intent(this, HelpCenterActivity.class)));
        setupClick(R.id.yourTermsCondition, R.id.arrowUp09, () -> startActivity(new Intent(this, TermsAndConditionActivity.class)));
        setupClick(R.id.yourAccDelete, R.id.arrowUp10, () -> startActivity(new Intent(this, AccountDeletionActivity.class)));
        setupClick(R.id.youAboutInfo, R.id.arrowUp11, () -> startActivity(new Intent(this, AboutActivity.class)));
    }

    private void showAccountSecurityDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_account_security);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvUID = dialog.findViewById(R.id.tvUserUID);
        TextView tvPhone = dialog.findViewById(R.id.tvUserPhone);
        TextView tvMemberSince = dialog.findViewById(R.id.tvMemberSince);
        
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            String numericPart = uid.replaceAll("[^0-9]", "");
            if (numericPart.length() < 4) numericPart = (uid.hashCode() + "0000").substring(0, 4);
            tvUID.setText("ASC-C" + numericPart.substring(0, 4));
            
            String phone = user.getPhoneNumber();
            tvPhone.setText(phone != null && !phone.isEmpty() ? phone : "No phone linked");

            long creationTimestamp = user.getMetadata().getCreationTimestamp();
            if (creationTimestamp > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                tvMemberSince.setText(sdf.format(new Date(creationTimestamp)));
            }
        }

        // UPDATED: Now points to ConfirmPasswordActivity
        dialog.findViewById(R.id.layoutChangePasswordInSec).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, ConfirmPasswordActivity.class));
        });

        dialog.findViewById(R.id.btnDoneSecurity).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showDisplayDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_display);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        Slider sliderFont = dialog.findViewById(R.id.sliderFontSize);
        SwitchMaterial swSerial = dialog.findViewById(R.id.switchShowSerial);
        SwitchMaterial swService = dialog.findViewById(R.id.switchServiceDate);
        SwitchMaterial swDistance = dialog.findViewById(R.id.switchTechDistance);

        sliderFont.setValue(prefs.getFloat("display_font_size", 1.0f));
        swSerial.setChecked(prefs.getBoolean("display_show_serial", true));
        swService.setChecked(prefs.getBoolean("display_show_service_date", true));
        swDistance.setChecked(prefs.getBoolean("display_show_distance", true));

        dialog.findViewById(R.id.btnDoneDisplay).setOnClickListener(v -> {
            prefs.edit()
                .putFloat("display_font_size", sliderFont.getValue())
                .putBoolean("display_show_serial", swSerial.isChecked())
                .putBoolean("display_show_service_date", swService.isChecked())
                .putBoolean("display_show_distance", swDistance.isChecked())
                .apply();

            Toast.makeText(this, "Display settings saved!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showPrivacyDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_privacy);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        SwitchMaterial swAnalytics = dialog.findViewById(R.id.switchAnalytics);
        SwitchMaterial swMarketing = dialog.findViewById(R.id.switchMarketing);

        swAnalytics.setChecked(prefs.getBoolean("privacy_analytics", true));
        swMarketing.setChecked(prefs.getBoolean("privacy_marketing", false));

        dialog.findViewById(R.id.btnDonePrivacy).setOnClickListener(v -> {
            prefs.edit()
                .putBoolean("privacy_analytics", swAnalytics.isChecked())
                .putBoolean("privacy_marketing", swMarketing.isChecked())
                .apply();

            Toast.makeText(this, "Privacy settings saved!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showLanguageDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_language);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.findViewById(R.id.btnLangEnglish).setOnClickListener(v -> {
            prefs.edit().putString("app_language", "en").apply();
            dialog.dismiss();
        });

        dialog.findViewById(R.id.btnCancelLang).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void setupClick(int labelId, int arrowId, Runnable action) {
        View label = findViewById(labelId);
        View arrow = findViewById(arrowId);
        View.OnClickListener listener = v -> action.run();
        if (label != null) label.setOnClickListener(listener);
        if (arrow != null) arrow.setOnClickListener(listener);
    }

    private void showToast(String msg) {
        Toast.makeText(this, "Opening " + msg + "...", Toast.LENGTH_SHORT).show();
    }
}
