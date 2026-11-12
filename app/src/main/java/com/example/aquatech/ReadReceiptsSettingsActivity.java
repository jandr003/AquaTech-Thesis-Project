package com.example.aquatech;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class ReadReceiptsSettingsActivity extends AppCompatActivity {

    private SwitchMaterial switchReadReceipts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_receipts_settings);

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
        View btnBack = findViewById(R.id.btnBackReadReceipts);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        switchReadReceipts = findViewById(R.id.switchReadReceipts);
        if (switchReadReceipts != null) {
            switchReadReceipts.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    Toast.makeText(this, "Read receipts enabled", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Read receipts disabled", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
