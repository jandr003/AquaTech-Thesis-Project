package com.example.aquatech;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class TermsAndConditionActivity extends AppCompatActivity {

    private CheckBox cbAgree;
    private MaterialButton btnAccept;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms);

        setupStatusBar();

        cbAgree = findViewById(R.id.cbAgreeTerms);
        btnAccept = findViewById(R.id.btnAcceptTerms);

        // Back Button
        findViewById(R.id.btnBackTerms).setOnClickListener(v -> finish());

        // Checkbox Logic
        cbAgree.setOnCheckedChangeListener((buttonView, isChecked) -> {
            btnAccept.setEnabled(isChecked);
            btnAccept.setAlpha(isChecked ? 1.0f : 0.5f);
        });

        // Accept Button
        btnAccept.setOnClickListener(v -> finish());
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }
}
