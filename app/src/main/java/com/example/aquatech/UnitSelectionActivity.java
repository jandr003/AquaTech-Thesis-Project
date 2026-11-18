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

    private static final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";
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
        if (mAuth == null) {
            return;
        }

        String uid = mAuth.getUid();
        if (uid == null) {
            return;
        }

        unitsRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(uid).child("units");
        unitsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<DataSnapshot> unitList = new ArrayList<>();
                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        if (ds != null) {
                            unitList.add(ds);
                        }
                    }
                    updateUI(unitList);
                } else {
                    fetchLegacyUnit(uid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchLegacyUnit(String uid) {
        if (uid == null) {
            return;
        }

        FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                if (s.exists() && s.hasChild("unitName")) {
                    updateUI_Legacy(s);
                } else {
                    if (card1 != null) {
                        card1.setVisibility(View.GONE);
                    }

                    if (card2 != null) {
                        card2.setVisibility(View.GONE);
                    }

                    if (tvNoUnits != null) {
                        tvNoUnits.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void updateUI(List<DataSnapshot> unitList) {
        if (tvNoUnits != null) {
            tvNoUnits.setVisibility(View.GONE);
        }

        if (unitList != null && unitList.size() >= 1) {
            if (card1 != null) {
                card1.setVisibility(View.VISIBLE);
                mapDataToCard(unitList.get(0), card1, R.id.customerUnit, R.id.CustomerUnitSerial, R.id.CustomerUnitPurchase, R.id.imgDispenser01);
            }
        } else if (card1 != null) {
            card1.setVisibility(View.GONE);
        }

        if (unitList != null && unitList.size() >= 2) {
            if (card2 != null) {
                card2.setVisibility(View.VISIBLE);
                mapDataToCard(unitList.get(1), card2, R.id.CustomerUnit01, R.id.CustomerUnitSerial01, R.id.CustomerUnitPurchase01, R.id.imgDispenser02);
            }

            if (cardAdd != null) {
                cardAdd.setVisibility(View.GONE);
            }
        } else {
            if (card2 != null) {
                card2.setVisibility(View.GONE);
            }

            if (cardAdd != null) {
                cardAdd.setVisibility(View.VISIBLE);
            }
        }
    }

    private void updateUI_Legacy(DataSnapshot ds) {
        if (ds == null) {
            return;
        }

        if (tvNoUnits != null) {
            tvNoUnits.setVisibility(View.GONE);
        }

        if (card1 != null) {
            card1.setVisibility(View.VISIBLE);
        }

        if (card2 != null) {
            card2.setVisibility(View.GONE);
        }

        String name = ds.child("unitName").getValue(String.class);
        String serial = ds.child("serialNo").getValue(String.class);
        String purchase = ds.child("purchaseType").getValue(String.class);
        String referenceNo = ds.child("referenceNo").getValue(String.class);

        if (card1 != null) {
            TextView tvName = card1.findViewById(R.id.customerUnit);
            if (tvName != null) {
                tvName.setText(name != null ? name : "Unknown Unit");
            }

            TextView tvSerial = card1.findViewById(R.id.CustomerUnitSerial);
            if (tvSerial != null) {
                tvSerial.setText(serial != null ? serial : "N/A");
            }

            TextView tvPurchase = card1.findViewById(R.id.CustomerUnitPurchase);
            if (tvPurchase != null) {
                tvPurchase.setText(purchase != null ? purchase : "N/A");
            }

            card1.setOnClickListener(v -> selectUnit(name, serial, referenceNo, R.drawable.titled_design1));
        }
    }

    private void mapDataToCard(DataSnapshot ds, CardView card, int nameId, int serialId, int purchaseId, int imgId) {
        if (ds == null || card == null) {
            return;
        }

        String name = ds.child("unitName").getValue(String.class);
        String serial = ds.child("serialNo").getValue(String.class);
        String purchase = ds.child("purchaseType").getValue(String.class);
        String referenceNo = ds.child("referenceNo").getValue(String.class);
        Integer imgRes = ds.child("unitImageRes").getValue(Integer.class);

        TextView tvName = card.findViewById(nameId);
        if (tvName != null) {
            tvName.setText(name != null ? name : "Unknown Unit");
        }

        TextView tvSerial = card.findViewById(serialId);
        if (tvSerial != null) {
            tvSerial.setText(serial != null ? serial : "N/A");
        }

        TextView tvPurchase = card.findViewById(purchaseId);
        if (tvPurchase != null) {
            tvPurchase.setText(purchase != null ? purchase : "N/A");
        }

        ImageView imgView = card.findViewById(imgId);
        if (imgView != null && imgRes != null) {
            imgView.setImageResource(imgRes);
        }

        card.setOnClickListener(v -> selectUnit(name, serial, referenceNo, imgRes != null ? imgRes : R.drawable.titled_design1));
    }

    private void selectUnit(String name, String serial, String refNo, int imgRes) {
        if (mAuth != null) {
            String uid = mAuth.getUid();
            if (uid != null) {
                DatabaseReference userRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(uid);
                userRef.child("unitName").setValue(name);
                userRef.child("serialNo").setValue(serial);
                userRef.child("referenceNo").setValue(refNo);
            }
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
