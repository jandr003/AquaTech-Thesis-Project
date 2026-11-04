package com.example.aquatech;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdminManageItemsActivity extends AppCompatActivity {

    private static final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private LinearLayout layoutAddedItemsContainer;
    private View tvHeaderAddedItems, cardAddedItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_items);

        setupStatusBar();
        initializeViews();
        setupFirebaseItemListener();
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

        layoutAddedItemsContainer = findViewById(R.id.layoutAddedItemsContainer);
        tvHeaderAddedItems = findViewById(R.id.tvHeaderAddedItems);
        cardAddedItems = findViewById(R.id.cardAddedItems);

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

    private void setupFirebaseItemListener() {
        DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL).getReference("ServiceItems");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (layoutAddedItemsContainer != null) {
                    layoutAddedItemsContainer.removeAllViews();
                    if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                        if (tvHeaderAddedItems != null) tvHeaderAddedItems.setVisibility(View.VISIBLE);
                        if (cardAddedItems != null) cardAddedItems.setVisibility(View.VISIBLE);

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String itemId = ds.getKey();
                            String itemName = ds.child("itemName").getValue(String.class);
                            String category = ds.child("category").getValue(String.class);
                            String photoUri = ds.child("photoUri").getValue(String.class);

                            addDynamicItemRow(itemId, itemName, category, photoUri);
                        }
                    } else {
                        if (tvHeaderAddedItems != null) tvHeaderAddedItems.setVisibility(View.GONE);
                        if (cardAddedItems != null) cardAddedItems.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void addDynamicItemRow(String itemId, String itemName, String category, String photoUri) {
        if (layoutAddedItemsContainer == null || itemName == null) return;

        if (layoutAddedItemsContainer.getChildCount() > 0) {
            View divider = new View(this);
            LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1);
            divParams.setMargins(0, 16, 0, 16);
            divider.setLayoutParams(divParams);
            divider.setBackgroundColor(Color.parseColor("#F1F5F9"));
            layoutAddedItemsContainer.addView(divider);
        }

        RelativeLayout row = new RelativeLayout(this);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setPadding(0, 8, 0, 8);

        // Image Card
        MaterialCardView cardImg = new MaterialCardView(this);
        int cardId = View.generateViewId();
        cardImg.setId(cardId);
        RelativeLayout.LayoutParams cardParams = new RelativeLayout.LayoutParams(
                dpToPx(48), dpToPx(48));
        cardParams.addRule(RelativeLayout.ALIGN_PARENT_START);
        cardParams.addRule(RelativeLayout.CENTER_VERTICAL);
        cardImg.setLayoutParams(cardParams);
        cardImg.setCardBackgroundColor(Color.parseColor("#F8FAFC"));
        cardImg.setRadius(dpToPx(12));
        cardImg.setCardElevation(0);
        cardImg.setStrokeColor(Color.parseColor("#E2E8F0"));
        cardImg.setStrokeWidth(dpToPx(1));

        ImageView ivPhoto = new ImageView(this);
        ivPhoto.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        ivPhoto.setScaleType(ImageView.ScaleType.FIT_CENTER);
        ivPhoto.setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6));

        if (photoUri != null && !photoUri.isEmpty()) {
            Glide.with(this).load(Uri.parse(photoUri)).placeholder(R.drawable.img_place_holder).into(ivPhoto);
        } else {
            ivPhoto.setImageResource(R.drawable.img_place_holder);
        }
        cardImg.addView(ivPhoto);

        // Right Action Container (Status + Delete Icon)
        LinearLayout actionContainer = new LinearLayout(this);
        int actionId = View.generateViewId();
        actionContainer.setId(actionId);
        RelativeLayout.LayoutParams actionParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        actionParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        actionParams.addRule(RelativeLayout.CENTER_VERTICAL);
        actionContainer.setLayoutParams(actionParams);
        actionContainer.setOrientation(LinearLayout.HORIZONTAL);
        actionContainer.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvStatus = new TextView(this);
        tvStatus.setText("Active");
        tvStatus.setTextColor(Color.parseColor("#059669"));
        tvStatus.setTextSize(12.5f);
        tvStatus.setTypeface(null, Typeface.BOLD);

        ImageView btnDelete = new ImageView(this);
        LinearLayout.LayoutParams btnDelParams = new LinearLayout.LayoutParams(dpToPx(26), dpToPx(26));
        btnDelParams.setMargins(dpToPx(12), 0, 0, 0);
        btnDelete.setLayoutParams(btnDelParams);
        btnDelete.setImageResource(R.drawable.ic_trash_delete);
        btnDelete.setColorFilter(Color.parseColor("#EF4444"));
        btnDelete.setPadding(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
        btnDelete.setClickable(true);
        btnDelete.setFocusable(true);

        btnDelete.setOnClickListener(v -> showDynamicRemoveConfirmationDialog(itemId, itemName));

        actionContainer.addView(tvStatus);
        actionContainer.addView(btnDelete);

        // Center Info Container
        LinearLayout infoContainer = new LinearLayout(this);
        RelativeLayout.LayoutParams infoParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        infoParams.addRule(RelativeLayout.END_OF, cardId);
        infoParams.addRule(RelativeLayout.START_OF, actionId);
        infoParams.addRule(RelativeLayout.CENTER_VERTICAL);
        infoParams.setMargins(dpToPx(14), 0, dpToPx(8), 0);
        infoContainer.setLayoutParams(infoParams);
        infoContainer.setOrientation(LinearLayout.VERTICAL);

        TextView tvName = new TextView(this);
        tvName.setText(itemName);
        tvName.setTextColor(Color.parseColor("#1E293B"));
        tvName.setTextSize(14f);
        tvName.setTypeface(null, Typeface.BOLD);

        TextView tvSub = new TextView(this);
        tvSub.setText(category != null ? category : "Service Item");
        tvSub.setTextColor(Color.parseColor("#64748B"));
        tvSub.setTextSize(11.5f);

        infoContainer.addView(tvName);
        infoContainer.addView(tvSub);

        row.addView(cardImg);
        row.addView(infoContainer);
        row.addView(actionContainer);

        layoutAddedItemsContainer.addView(row);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void showDynamicRemoveConfirmationDialog(String itemId, String itemName) {
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
            if (itemId != null) {
                FirebaseDatabase.getInstance(DB_URL).getReference("ServiceItems").child(itemId).removeValue()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(this, itemName + " removed successfully!", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
            dialog.dismiss();
        });

        dialog.show();
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
