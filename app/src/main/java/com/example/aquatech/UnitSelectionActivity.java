package com.example.aquatech;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
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
import java.util.List;

public class UnitSelectionActivity extends AppCompatActivity {

    private CardView card1, card2, cardAdd;
    private TextView tvNoUnits;
    private FirebaseAuth mAuth;
    private DatabaseReference unitsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unit_selection);

        mAuth = FirebaseAuth.getInstance();

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        card1 = findViewById(R.id.blueCard01);
        card2 = findViewById(R.id.blueCard02);
        cardAdd = findViewById(R.id.blueCard03);
        tvNoUnits = findViewById(R.id.tvNoUnits);

        setupFirebaseListener();

        if (cardAdd != null) {
            cardAdd.setOnClickListener(v -> {
                Intent intent = new Intent(UnitSelectionActivity.this, CustomerSetupActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setupFirebaseListener() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        // 📍 Listening to all units saved under the user
        unitsRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("units");
        unitsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<DataSnapshot> unitList = new ArrayList<>();
                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        unitList.add(ds);
                    }
                    updateUI(unitList);
                } else {
                    // Fallback to legacy/root if units folder is empty
                    fetchLegacyUnit(uid);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchLegacyUnit(String uid) {
        FirebaseDatabase.getInstance().getReference("Users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                if (s.exists() && s.hasChild("unitName")) {
                    List<DataSnapshot> legacyList = new ArrayList<>();
                    // We treat the root unit as one item list if units folder is empty
                    updateUI_Legacy(s);
                } else {
                    card1.setVisibility(View.GONE);
                    card2.setVisibility(View.GONE);
                    tvNoUnits.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void updateUI(List<DataSnapshot> unitList) {
        tvNoUnits.setVisibility(View.GONE);

        if (unitList.size() >= 1) {
            card1.setVisibility(View.VISIBLE);
            mapDataToCard(unitList.get(0), card1, R.id.customerUnit, R.id.CustomerUnitSerial, R.id.CustomerUnitPurchase, R.id.imgDispenser01);
        } else {
            card1.setVisibility(View.GONE);
        }

        if (unitList.size() >= 2) {
            card2.setVisibility(View.VISIBLE);
            mapDataToCard(unitList.get(1), card2, R.id.CustomerUnit01, R.id.CustomerUnitSerial01, R.id.CustomerUnitPurchase01, R.id.imgDispenser02);
            cardAdd.setVisibility(View.GONE); // Limit to 2 units
        } else {
            card2.setVisibility(View.GONE);
            cardAdd.setVisibility(View.VISIBLE);
        }
    }

    private void updateUI_Legacy(DataSnapshot ds) {
        tvNoUnits.setVisibility(View.GONE);
        card1.setVisibility(View.VISIBLE);
        card2.setVisibility(View.GONE);
        
        String name = ds.child("unitName").getValue(String.class);
        String serial = ds.child("serialNo").getValue(String.class);
        String purchase = ds.child("purchaseType").getValue(String.class);
        String referenceNo = ds.child("referenceNo").getValue(String.class);
        
        ((TextView) card1.findViewById(R.id.customerUnit)).setText(name != null ? name : "Unknown Unit");
        ((TextView) card1.findViewById(R.id.CustomerUnitSerial)).setText(serial != null ? serial : "N/A");
        ((TextView) card1.findViewById(R.id.CustomerUnitPurchase)).setText(purchase != null ? purchase : "N/A");
        
        card1.setOnClickListener(v -> selectUnit(name, serial, referenceNo, R.drawable.titled_design1));
    }

    private void mapDataToCard(DataSnapshot ds, CardView card, int nameId, int serialId, int purchaseId, int imgId) {
        String name = ds.child("unitName").getValue(String.class);
        String serial = ds.child("serialNo").getValue(String.class);
        String purchase = ds.child("purchaseType").getValue(String.class);
        String referenceNo = ds.child("referenceNo").getValue(String.class);
        Integer imgRes = ds.child("unitImageRes").getValue(Integer.class);

        ((TextView) card.findViewById(nameId)).setText(name != null ? name : "Unknown Unit");
        ((TextView) card.findViewById(serialId)).setText(serial != null ? serial : "N/A");
        ((TextView) card.findViewById(purchaseId)).setText(purchase != null ? purchase : "N/A");

        if (imgRes != null) {
            ((ImageView) card.findViewById(imgId)).setImageResource(imgRes);
        }

        card.setOnClickListener(v -> selectUnit(name, serial, referenceNo, imgRes != null ? imgRes : R.drawable.titled_design1));
    }

    private void selectUnit(String name, String serial, String refNo, int imgRes) {
        String uid = mAuth.getUid();
        if (uid != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
            userRef.child("unitName").setValue(name);
            userRef.child("serialNo").setValue(serial);
            userRef.child("referenceNo").setValue(refNo);
        }

        Intent intent = new Intent();
        intent.putExtra("unit_name", name);
        intent.putExtra("serial_no", serial);
        intent.putExtra("reference_no", refNo);
        intent.putExtra("image_res", imgRes);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
