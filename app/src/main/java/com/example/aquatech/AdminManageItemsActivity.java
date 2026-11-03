package com.example.aquatech;

import android.app.AlertDialog;
import android.app.Dialog;
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
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm_delete_item);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvMsg = dialog.findViewById(R.id.tvDeleteDialogMessage);
        if (tvMsg != null) {
            tvMsg.setText("Are you sure you want to remove " + itemName + " from Step 3 requests?");
        }

        dialog.findViewById(R.id.btnCancelDeleteItem).setOnClickListener(v -> dialog.dismiss());

        dialog.findViewById(R.id.btnConfirmDeleteItem).setOnClickListener(v -> {
            Toast.makeText(this, itemName + " removed successfully", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }
}
