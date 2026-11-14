package com.example.aquatech;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
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
    private TextView setUpAccountTitle, tvPageIndicator;
    private Button btnPrevPage, btnNextPage;
    private LinearLayout layoutDynamicCardsContainer;
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    private final List<Map<String, String>> allProductsList = new ArrayList<>();
    private static final int ITEMS_PER_PAGE = 5;
    private int currentPage = 0;
    private String selectedUnitName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_setup);

        mAuth = FirebaseAuth.getInstance();
        setupStatusBar();
        initializeViews();
        fetchUserData();
        loadAllProductsFromFirebase();
    }

    private void initializeViews() {
        View btnBack = findViewById(R.id.btnBackSetup);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        setUpAccountTitle = findViewById(R.id.setUpAccount);
        layoutDynamicCardsContainer = findViewById(R.id.layoutDynamicCardsContainer);

        btnPrevPage = findViewById(R.id.btnPrevPage);
        btnNextPage = findViewById(R.id.btnNextPage);
        tvPageIndicator = findViewById(R.id.tvPageIndicator);

        if (btnPrevPage != null) {
            btnPrevPage.setOnClickListener(v -> {
                if (currentPage > 0) {
                    currentPage--;
                    renderCurrentPageCards();
                }
            });
        }

        if (btnNextPage != null) {
            btnNextPage.setOnClickListener(v -> {
                int totalPages = getTotalPages();
                if (currentPage < totalPages - 1) {
                    currentPage++;
                    renderCurrentPageCards();
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

                addDefaultProduct("WL CUBE FIREWALL", "F-FXCU1-M-HCA-TT-0001", "Patented UVC Firewall™ Technology", "");
                addDefaultProduct("SMART SLIM", "S-FXCU1-M-HCA-AA-0002", "Sleek, Space-Saving Pure Hydration", "");
                addDefaultProduct("COUNTER TOP WATER PURIFIER", "CT-FXCU1-M-HCA-WT-0003", "Ideal for Kitchen & Home Countertops", "");
                addDefaultProduct("STANDING WATER PURIFIER", "ST-FXCU1-M-HCA-WT-0004", "High-Capacity Commercial & Office Purifier", "");

                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String name = ds.child("unitName").getValue(String.class);
                        if (name == null) {
                            name = ds.child("itemName").getValue(String.class);
                        }

                        String modelCode = ds.child("modelCode").getValue(String.class);
                        String description = ds.child("description").getValue(String.class);
                        String photoUri = ds.child("photoUri").getValue(String.class);

                        if (name != null && !name.isEmpty() && !isAlreadyInList(name)) {
                            Map<String, String> map = new HashMap<>();
                            map.put("unitName", name);
                            map.put("modelCode", modelCode != null && !modelCode.isEmpty() ? modelCode : "ASC-U00" + (allProductsList.size() + 1));
                            map.put("description", description != null && !description.isEmpty() ? description : "Certified Water Purifier Unit");
                            map.put("photoUri", photoUri != null ? photoUri : "");
                            allProductsList.add(map);
                        }
                    }
                }

                renderCurrentPageCards();
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

    private void addDefaultProduct(String name, String code, String desc, String photo) {
        Map<String, String> map = new HashMap<>();
        map.put("unitName", name);
        map.put("modelCode", code);
        map.put("description", desc);
        map.put("photoUri", photo);
        allProductsList.add(map);
    }

    private int getTotalPages() {
        if (allProductsList.isEmpty()) {
            return 1;
        }
        return (int) Math.ceil((double) allProductsList.size() / ITEMS_PER_PAGE);
    }

    private void renderCurrentPageCards() {
        if (layoutDynamicCardsContainer == null) {
            return;
        }
        layoutDynamicCardsContainer.removeAllViews();

        int totalPages = getTotalPages();
        if (currentPage >= totalPages) {
            currentPage = totalPages - 1;
        }
        if (currentPage < 0) {
            currentPage = 0;
        }

        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allProductsList.size());

        for (int i = startIndex; i < endIndex; i++) {
            Map<String, String> productMap = allProductsList.get(i);
            if (productMap != null) {
                View cardView = createProductCardView(productMap);
                if (cardView != null) {
                    layoutDynamicCardsContainer.addView(cardView);
                }
            }
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

    private View createProductCardView(Map<String, String> productMap) {
        String unitName = productMap.get("unitName");
        String modelCode = productMap.get("modelCode");
        String description = productMap.get("description");
        String photoUri = productMap.get("photoUri");

        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        int marginPx = dpToPx(4);
        cardParams.setMargins(marginPx, marginPx, marginPx, marginPx);
        card.setLayoutParams(cardParams);

        card.setRadius(dpToPx(20));
        card.setCardElevation(dpToPx(4));
        card.setUseCompatPadding(true);

        boolean isSelected = (unitName != null && unitName.equalsIgnoreCase(selectedUnitName));
        card.setCardBackgroundColor(Color.parseColor(isSelected ? "#E3F2FD" : "#FFFFFF"));

        RelativeLayout relativeLayout = new RelativeLayout(this);
        relativeLayout.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        ));
        relativeLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        FrameLayout frameLayout = new FrameLayout(this);
        int frameId = View.generateViewId();
        frameLayout.setId(frameId);
        RelativeLayout.LayoutParams frameParams = new RelativeLayout.LayoutParams(dpToPx(80), dpToPx(80));
        frameParams.addRule(RelativeLayout.ALIGN_PARENT_START);
        frameParams.addRule(RelativeLayout.CENTER_VERTICAL);
        frameLayout.setLayoutParams(frameParams);
        frameLayout.setBackgroundResource(R.drawable.bg_unit_backdrop);
        frameLayout.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));

        ImageView ivPhoto = new ImageView(this);
        ivPhoto.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        ivPhoto.setScaleType(ImageView.ScaleType.FIT_CENTER);

        int defaultImg = getImageForUnit(unitName);
        if (photoUri != null && !photoUri.isEmpty()) {
            Glide.with(this).load(Uri.parse(photoUri)).placeholder(defaultImg).into(ivPhoto);
        } else {
            ivPhoto.setImageResource(defaultImg);
        }
        frameLayout.addView(ivPhoto);

        ImageView ivArrow = new ImageView(this);
        int arrowId = View.generateViewId();
        ivArrow.setId(arrowId);
        RelativeLayout.LayoutParams arrowParams = new RelativeLayout.LayoutParams(dpToPx(20), dpToPx(20));
        arrowParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        arrowParams.addRule(RelativeLayout.CENTER_VERTICAL);
        ivArrow.setLayoutParams(arrowParams);
        ivArrow.setImageResource(R.drawable.arrow_forward);
        ivArrow.setColorFilter(Color.parseColor("#94A3B8"));

        LinearLayout textLayout = new LinearLayout(this);
        RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        textParams.addRule(RelativeLayout.END_OF, frameId);
        textParams.addRule(RelativeLayout.START_OF, arrowId);
        textParams.addRule(RelativeLayout.CENTER_VERTICAL);
        textParams.setMargins(dpToPx(16), 0, dpToPx(8), 0);
        textLayout.setLayoutParams(textParams);
        textLayout.setOrientation(LinearLayout.VERTICAL);

        TextView tvName = new TextView(this);
        tvName.setText(unitName != null ? unitName : "Water Purifier");
        tvName.setTextColor(Color.parseColor("#1E293B"));
        tvName.setTextSize(16f);
        tvName.setTypeface(null, Typeface.BOLD);

        TextView tvCode = new TextView(this);
        tvCode.setText(modelCode != null ? modelCode : "");
        tvCode.setTextColor(Color.parseColor("#2196F3"));
        tvCode.setTextSize(12f);
        tvCode.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams codeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        codeParams.setMargins(0, dpToPx(2), 0, 0);
        tvCode.setLayoutParams(codeParams);

        TextView tvDesc = new TextView(this);
        tvDesc.setText(description != null ? description : "");
        tvDesc.setTextColor(Color.parseColor("#64748B"));
        tvDesc.setTextSize(11.5f);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        descParams.setMargins(0, dpToPx(2), 0, 0);
        tvDesc.setLayoutParams(descParams);

        textLayout.addView(tvName);
        textLayout.addView(tvCode);
        textLayout.addView(tvDesc);

        relativeLayout.addView(frameLayout);
        relativeLayout.addView(textLayout);
        relativeLayout.addView(ivArrow);

        card.addView(relativeLayout);

        card.setOnClickListener(v -> {
            if (unitName != null) {
                selectedUnitName = unitName;
                card.setCardBackgroundColor(Color.parseColor("#E3F2FD"));
                saveUnitAndNavigate(unitName);
            }
        });

        return card;
    }

    private int getImageForUnit(String name) {
        if (name == null) {
            return R.drawable.titled_design1;
        }
        String upper = name.toUpperCase();
        if (upper.contains("SLIM")) {
            return R.drawable.titled_design2;
        }
        if (upper.contains("COUNTER")) {
            return R.drawable.titled_design3;
        }
        if (upper.contains("STANDING")) {
            return R.drawable.titled_design4;
        }
        return R.drawable.titled_design1;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void saveUnitAndNavigate(String unitName) {
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
