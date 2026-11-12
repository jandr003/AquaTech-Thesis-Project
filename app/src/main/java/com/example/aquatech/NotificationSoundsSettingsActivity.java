package com.example.aquatech;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class NotificationSoundsSettingsActivity extends AppCompatActivity {

    private SwitchMaterial switchMainMaster;
    private TextView tvMainSwitchLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_sounds_settings);

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
        View btnBack = findViewById(R.id.btnBackSoundsSettings);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        switchMainMaster = findViewById(R.id.switchMainMaster);
        tvMainSwitchLabel = findViewById(R.id.tvMainSwitchLabel);

        if (switchMainMaster != null) {
            switchMainMaster.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (tvMainSwitchLabel != null) {
                    if (isChecked) {
                        tvMainSwitchLabel.setText("On");
                    } else {
                        tvMainSwitchLabel.setText("Off");
                    }
                }
                if (isChecked) {
                    Toast.makeText(this, "Notifications On", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Notifications Off", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
