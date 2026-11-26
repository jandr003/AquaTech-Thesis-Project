package com.example.aquatech;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        setupStatusBar();

        View btnBack = findViewById(R.id.btnBackAbout);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        TextView tvCopyright = findViewById(R.id.tvCopyrightNotice);
        if (tvCopyright != null) {
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            tvCopyright.setText("© " + currentYear + " Aqua SmartGuard Corp. All Rights Reserved.");
        }
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }
}
