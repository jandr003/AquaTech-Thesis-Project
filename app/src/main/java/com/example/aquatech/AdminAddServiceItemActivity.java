package com.example.aquatech;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class AdminAddServiceItemActivity extends AppCompatActivity {

    private static final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private ImageView ivImagePreview;
    private TextView tvPhotoHint;
    private EditText etItemName, etItemCode, etItemPrice;
    private ChipGroup chipGroupCategory;
    private Chip chipFilter, chipParts;

    private Uri selectedImageUri = null;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (ivImagePreview != null) {
                        ivImagePreview.setPadding(0, 0, 0, 0);
                        ivImagePreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        ivImagePreview.setImageURI(selectedImageUri);
                    }
                    if (tvPhotoHint != null) {
                        tvPhotoHint.setText("Photo attached!");
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_service_item);

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
        findViewById(R.id.btnBackAddServiceItem).setOnClickListener(v -> finish());

        ivImagePreview = findViewById(R.id.ivItemImagePreview);
        tvPhotoHint = findViewById(R.id.tvUploadPhotoHint);

        etItemName = findViewById(R.id.etItemName);
        etItemCode = findViewById(R.id.etItemCode);
        etItemPrice = findViewById(R.id.etItemPrice);

        chipGroupCategory = findViewById(R.id.chipGroupItemCategory);
        chipFilter = findViewById(R.id.chipCategoryFilter);
        chipParts = findViewById(R.id.chipCategoryParts);

        if (chipGroupCategory != null) {
            chipGroupCategory.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.chipCategoryParts) {
                    if (chipParts != null) chipParts.setTextColor(Color.parseColor("#2196F3"));
                    if (chipFilter != null) chipFilter.setTextColor(Color.parseColor("#64748B"));
                } else {
                    if (chipFilter != null) chipFilter.setTextColor(Color.parseColor("#2196F3"));
                    if (chipParts != null) chipParts.setTextColor(Color.parseColor("#64748B"));
                }
            });
        }

        View cardPhoto = findViewById(R.id.cardUploadItemPhoto);
        if (cardPhoto != null) {
            cardPhoto.setOnClickListener(v -> openGalleryPicker());
        }

        findViewById(R.id.btnSubmitSaveItem).setOnClickListener(v -> handleSaveServiceItem());
    }

    private void openGalleryPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void handleSaveServiceItem() {
        String name = etItemName.getText() != null ? etItemName.getText().toString().trim() : "";
        String code = etItemCode.getText() != null ? etItemCode.getText().toString().trim() : "";
        String priceStr = etItemPrice.getText() != null ? etItemPrice.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Please enter item / part name", Toast.LENGTH_SHORT).show();
            return;
        }

        String category = "Replacement Filter";
        if (chipGroupCategory != null && chipGroupCategory.getCheckedChipId() == R.id.chipCategoryParts) {
            category = "Parts & Installation Kit";
        }

        saveItemToDatabase(name, code, priceStr, category);
    }

    private void saveItemToDatabase(String name, String code, String priceStr, String category) {
        DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL).getReference("ServiceItems");
        String itemId = ref.push().getKey();
        if (itemId == null) itemId = "ITEM_" + System.currentTimeMillis();

        Map<String, Object> data = new HashMap<>();
        data.put("itemId", itemId);
        data.put("itemName", name);
        data.put("itemCode", code != null ? code : "");
        data.put("category", category);
        data.put("price", priceStr != null ? priceStr : "0");
        data.put("status", "Active");
        data.put("photoUri", selectedImageUri != null ? selectedImageUri.toString() : "");

        ref.child(itemId).setValue(data).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Service item saved successfully!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to save item", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
