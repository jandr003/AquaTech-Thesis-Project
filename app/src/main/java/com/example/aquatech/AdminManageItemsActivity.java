package com.example.aquatech;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AdminManageItemsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_items);

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
        findViewById(R.id.btnBackManageItems).setOnClickListener(v -> finish());

        View btnAddHeader = findViewById(R.id.btnAddItemHeader);
        if (btnAddHeader != null) {
            btnAddHeader.setOnClickListener(v -> handleAddItem());
        }

        findViewById(R.id.btnAddServiceItem).setOnClickListener(v -> handleAddItem());
    }

    private void handleAddItem() {
        Toast.makeText(this, "Service items updated successfully", Toast.LENGTH_SHORT).show();
    }
}
