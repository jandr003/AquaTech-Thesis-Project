package com.example.aquatech;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminManageProductsActivity extends AppCompatActivity {

    private static final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private RecyclerView rvProducts;
    private CustomerSetupAdapter adapter;
    private final List<Map<String, String>> productsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_products);

        setupStatusBar();
        initializeViews();
        loadProductsFromFirebase();
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initializeViews() {
        View btnBack = findViewById(R.id.btnBackManageProducts);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View btnAddHeader = findViewById(R.id.btnAddProductHeader);
        if (btnAddHeader != null) {
            btnAddHeader.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminAddProductActivity.class);
                startActivity(intent);
            });
        }

        View btnAddBottom = findViewById(R.id.btnAddNewProductBottom);
        if (btnAddBottom != null) {
            btnAddBottom.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminAddProductActivity.class);
                startActivity(intent);
            });
        }

        rvProducts = findViewById(R.id.rvAdminProductsList);
        if (rvProducts != null) {
            rvProducts.setLayoutManager(new LinearLayoutManager(this));
            adapter = new CustomerSetupAdapter(productsList, (unitMap, position) -> {});
            rvProducts.setAdapter(adapter);
        }
    }

    private void loadProductsFromFirebase() {
        DatabaseReference productsRef = FirebaseDatabase.getInstance(DB_URL).getReference("Products");
        productsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                productsList.clear();
                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    addDefaultProduct("WL CUBE FIREWALL", "Certified Water Dispenser");
                    addDefaultProduct("SMART SLIM", "Space-Saving Pure Hydration");
                    addDefaultProduct("COUNTER TOP PURIFIER", "Ideal for Kitchen Countertops");
                    addDefaultProduct("STANDING PURIFIER", "High-Capacity Commercial Unit");
                } else {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String name = ds.child("unitName").getValue(String.class);
                        if (name == null) {
                            name = ds.child("itemName").getValue(String.class);
                        }
                        String category = ds.child("category").getValue(String.class);
                        String photoUri = ds.child("photoUri").getValue(String.class);

                        if (name != null && !name.isEmpty()) {
                            Map<String, String> map = new HashMap<>();
                            map.put("unitName", name);
                            map.put("category", category != null ? category : "Certified Dispenser Unit");
                            if (photoUri != null) {
                                map.put("photoUri", photoUri);
                            }
                            productsList.add(map);
                        }
                    }
                }

                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addDefaultProduct(String name, String category) {
        Map<String, String> map = new HashMap<>();
        map.put("unitName", name);
        map.put("category", category);
        productsList.add(map);
    }
}
