package com.example.aquatech;

import android.app.AlertDialog;
import android.content.Intent;
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
            btnAddHeader.setOnClickListener(v -> startActivity(new Intent(this, AdminAddServiceItemActivity.class)));
        }

        findViewById(R.id.btnAddServiceItem).setOnClickListener(v -> startActivity(new Intent(this, AdminAddServiceItemActivity.class)));

        bindDeleteButton(R.id.btnDeleteItem1, "Filter 0064-CBC");
        bindDeleteButton(R.id.btnDeleteItem2, "Filter 0055-SEDIMENT");
        bindDeleteButton(R.id.btnDeleteItem3, "SMSF 1µ CBC Filter");
        bindDeleteButton(R.id.btnDeleteItem4, "SMSF 10µ SED Filter");
        bindDeleteButton(R.id.btnDeleteItem5, "Inline Filter Assembly");
        bindDeleteButton(R.id.btnDeleteItem6, "Installation Kit (3-Way Valve)");
        bindDeleteButton(R.id.btnDeleteItem7, "Aquatal Dispenser Parts");
        bindDeleteButton(R.id.btnDeleteItem8, "UV Lamp Assembly");
        bindDeleteButton(R.id.btnDeleteItem9, "Touch Panel Board");
        bindDeleteButton(R.id.btnDeleteItem10, "PBC / PCB Main Control Board");
    }

    private void bindDeleteButton(int resId, String name) {
        View btn = findViewById(resId);
        if (btn != null) {
            btn.setOnClickListener(v -> showRemoveConfirmationDialog(name));
        }
    }

    public void showRemoveConfirmationDialog(String itemName) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Service Item")
                .setMessage("Are you sure you want to disable or remove " + itemName + " from Step 3 requests?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    Toast.makeText(this, itemName + " removed from Step 3", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
