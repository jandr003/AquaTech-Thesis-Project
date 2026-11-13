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
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class AdminAddProductActivity extends AppCompatActivity {

    private static final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private ImageView ivPhotoPreview;
    private TextView tvPhotoHint;
    private EditText etProductName, etProductModelCode, etProductCategory, etProductDescription;

    private Uri selectedImageUri = null;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (ivPhotoPreview != null) {
                        ivPhotoPreview.setPadding(0, 0, 0, 0);
                        ivPhotoPreview.clearColorFilter();
                        ivPhotoPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        ivPhotoPreview.setImageURI(selectedImageUri);
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
        setContentView(R.layout.activity_admin_add_product);

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
        View btnBack = findViewById(R.id.btnBackAddProduct);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        ivPhotoPreview = findViewById(R.id.ivProductPhotoPreview);
        tvPhotoHint = findViewById(R.id.tvUploadProductHint);

        etProductName = findViewById(R.id.etProductName);
        etProductModelCode = findViewById(R.id.etProductModelCode);
        etProductCategory = findViewById(R.id.etProductCategory);
        etProductDescription = findViewById(R.id.etProductDescription);

        View cardPhoto = findViewById(R.id.cardUploadProductPhoto);
        if (cardPhoto != null) {
            cardPhoto.setOnClickListener(v -> openGalleryPicker());
        }

        View btnSave = findViewById(R.id.btnSaveNewProduct);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> handleSaveProduct());
        }
    }

    private void openGalleryPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void handleSaveProduct() {
        String name = "";
        if (etProductName != null && etProductName.getText() != null) {
            name = etProductName.getText().toString().trim();
        }

        String modelCode = "";
        if (etProductModelCode != null && etProductModelCode.getText() != null) {
            modelCode = etProductModelCode.getText().toString().trim();
        }

        String category = "";
        if (etProductCategory != null && etProductCategory.getText() != null) {
            category = etProductCategory.getText().toString().trim();
        }

        String description = "";
        if (etProductDescription != null && etProductDescription.getText() != null) {
            description = etProductDescription.getText().toString().trim();
        }

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Please enter product name", Toast.LENGTH_SHORT).show();
            return;
        }

        saveProductToDatabase(name, modelCode, category, description);
    }

    private void saveProductToDatabase(String name, String modelCode, String category, String description) {
        DatabaseReference productsRef = FirebaseDatabase.getInstance(DB_URL).getReference("Products");

        String productId = productsRef.push().getKey();
        if (productId == null) {
            productId = "PROD_" + System.currentTimeMillis();
        }

        Map<String, Object> productData = new HashMap<>();
        productData.put("productId", productId);
        productData.put("unitName", name);
        productData.put("itemName", name);
        productData.put("modelCode", modelCode != null ? modelCode : "");
        productData.put("category", (category != null && !category.isEmpty()) ? category : "Certified Water Dispenser");
        productData.put("description", description != null ? description : "");
        productData.put("status", "Active");
        productData.put("photoUri", selectedImageUri != null ? selectedImageUri.toString() : "");

        Map<String, Object> updates = new HashMap<>();
        updates.put("/Products/" + productId, productData);
        updates.put("/ServiceItems/" + productId, productData);

        FirebaseDatabase.getInstance(DB_URL).getReference().updateChildren(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "New Product added to catalog!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to save product. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
