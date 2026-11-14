package com.example.aquatech;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerSetupActivity extends AppCompatActivity {

    private static final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private CardView card1, card2, card3, card4;
    private TextView setUpAccountTitle, tvPageIndicator;
    private Button btnPrevPage, btnNextPage;
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    private final List<Map<String, String>> allProductsList = new ArrayList<>();
    private static final int ITEMS_PER_PAGE = 5;
    private int currentPage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_setup);

        mAuth = FirebaseAuth.getInstance();
        setupStatusBar();
        initializeViews();
        fetchUserData();
        setupClickListeners();
        loadAllProductsFromFirebase();
    }

    private void initializeViews() {
        View btnBack = findViewById(R.id.btnBackSetup);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        setUpAccountTitle = findViewById(R.id.setUpAccount);
        card1 = findViewById(R.id.whiteCardItem1);
        card2 = findViewById(R.id.whiteCardItem2);
        card3 = findViewById(R.id.whiteCardItem3);
        card4 = findViewById(R.id.whiteCardItem4);

        btnPrevPage = findViewById(R.id.btnPrevPage);
        btnNextPage = findViewById(R.id.btnNextPage);
        tvPageIndicator = findViewById(R.id.tvPageIndicator);

        if (btnPrevPage != null) {
            btnPrevPage.setOnClickListener(v -> {
                if (currentPage > 0) {
                    currentPage--;
                    updatePaginationUI();
                }
            });
        }

        if (btnNextPage != null) {
            btnNextPage.setOnClickListener(v -> {
                int totalPages = getTotalPages();
                if (currentPage < totalPages - 1) {
                    currentPage++;
                    updatePaginationUI();
                }
            });
        }
    }

    private void fetchUserData() {
        if (mAuth == null) {
            return;
        }

        String uid = mAuth.getUid();
        if (uid != null) {
            userRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(uid);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String name = snapshot.child("fullName").getValue(String.class);
                        if (name == null) {
                            name = snapshot.child("name").getValue(String.class);
                        }

                        if (name != null && !name.isEmpty()) {
                            if (setUpAccountTitle != null) {
                                setUpAccountTitle.setText("HI " + name.toUpperCase() + ", SET UP YOUR ACCOUNT");
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    private void loadAllProductsFromFirebase() {
        DatabaseReference productsRef = FirebaseDatabase.getInstance(DB_URL).getReference("Products");
        productsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allProductsList.clear();

                addDefaultProduct("WL CUBE FIREWALL", "F-FXCU1-M-HCA-TT-0001", "Patented UVC Firewall™ Technology");
                addDefaultProduct("SMART SLIM", "S-FXCU1-M-HCA-AA-0002", "Sleek, Space-Saving Pure Hydration");
                addDefaultProduct("COUNTER TOP WATER PURIFIER", "CT-FXCU1-M-HCA-WT-0003", "Ideal for Kitchen & Home Countertops");
                addDefaultProduct("STANDING WATER PURIFIER", "ST-FXCU1-M-HCA-WT-0004", "High-Capacity Commercial & Office Purifier");

                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String name = ds.child("unitName").getValue(String.class);
                        if (name == null) {
                            name = ds.child("itemName").getValue(String.class);
                        }

                        String modelCode = ds.child("modelCode").getValue(String.class);
                        String description = ds.child("description").getValue(String.class);

                        if (name != null && !name.isEmpty() && !isAlreadyInList(name)) {
                            Map<String, String> map = new HashMap<>();
                            map.put("unitName", name);
                            map.put("modelCode", modelCode != null ? modelCode : "ASC-U00" + (allProductsList.size() + 1));
                            map.put("description", description != null && !description.isEmpty() ? description : "Certified Water Purifier Unit");
                            allProductsList.add(map);
                        }
                    }
                }

                updatePaginationUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private boolean isAlreadyInList(String name) {
        for (Map<String, String> m : allProductsList) {
            if (m.get("unitName") != null && m.get("unitName").equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private void addDefaultProduct(String name, String code, String desc) {
        Map<String, String> map = new HashMap<>();
        map.put("unitName", name);
        map.put("modelCode", code);
        map.put("description", desc);
        allProductsList.add(map);
    }

    private int getTotalPages() {
        if (allProductsList.isEmpty()) {
            return 1;
        }
        return (int) Math.ceil((double) allProductsList.size() / ITEMS_PER_PAGE);
    }

    private void updatePaginationUI() {
        int totalPages = getTotalPages();
        if (currentPage >= totalPages) {
            currentPage = totalPages - 1;
        }
        if (currentPage < 0) {
            currentPage = 0;
        }

        if (tvPageIndicator != null) {
            tvPageIndicator.setText("Page " + (currentPage + 1) + " of " + totalPages);
        }

        if (btnPrevPage != null) {
            btnPrevPage.setEnabled(currentPage > 0);
            btnPrevPage.setAlpha(currentPage > 0 ? 1.0f : 0.5f);
        }

        if (btnNextPage != null) {
            btnNextPage.setEnabled(currentPage < totalPages - 1);
            btnNextPage.setAlpha(currentPage < totalPages - 1 ? 1.0f : 0.5f);
        }
    }

    private void setupClickListeners() {
        if (card1 != null) {
            card1.setOnClickListener(v -> selectCard(card1, "WL CUBE FIREWALL"));
        }

        if (card2 != null) {
            card2.setOnClickListener(v -> selectCard(card2, "SMART SLIM"));
        }

        if (card3 != null) {
            card3.setOnClickListener(v -> selectCard(card3, "COUNTER TOP WATER PURIFIER"));
        }

        if (card4 != null) {
            card4.setOnClickListener(v -> selectCard(card4, "STANDING WATER PURIFIER"));
        }
    }

    private void selectCard(CardView selectedCard, String unitName) {
        if (card1 != null) {
            card1.setCardBackgroundColor(Color.parseColor("#F8F8F8"));
        }
        if (card2 != null) {
            card2.setCardBackgroundColor(Color.parseColor("#F8F8F8"));
        }
        if (card3 != null) {
            card3.setCardBackgroundColor(Color.parseColor("#F8F8F8"));
        }
        if (card4 != null) {
            card4.setCardBackgroundColor(Color.parseColor("#F8F8F8"));
        }

        if (selectedCard != null) {
            selectedCard.setCardBackgroundColor(Color.parseColor("#E3F2FD"));
        }

        if (userRef != null) {
            userRef.child("selectedUnit").setValue(unitName)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Intent intent = new Intent(CustomerSetupActivity.this, UnitDetailsActivity.class);
                            intent.putExtra("UNIT_NAME", unitName);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(CustomerSetupActivity.this, "Failed to save selection. Try again.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Intent intent = new Intent(CustomerSetupActivity.this, UnitDetailsActivity.class);
            intent.putExtra("UNIT_NAME", unitName);
            startActivity(intent);
            finish();
        }
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }
}
