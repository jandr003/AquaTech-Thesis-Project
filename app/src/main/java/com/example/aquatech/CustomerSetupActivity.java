package com.example.aquatech;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
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

public class CustomerSetupActivity extends AppCompatActivity {

    private CardView card1, card2, card3, card4;
    private TextView setUpAccountTitle;
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_setup);

        mAuth = FirebaseAuth.getInstance();
        setupStatusBar();
        initializeViews();
        fetchUserData();
        setupClickListeners();
    }

    private void initializeViews() {
        setUpAccountTitle = findViewById(R.id.setUpAccount);
        card1 = findViewById(R.id.whiteCardItem1);
        card2 = findViewById(R.id.whiteCardItem2);
        card3 = findViewById(R.id.whiteCardItem3);
        card4 = findViewById(R.id.whiteCardItem4);
    }

    private void fetchUserData() {
        String uid = mAuth.getUid();
        if (uid != null) {
            userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String name = snapshot.child("fullName").getValue(String.class);
                        if (name == null) name = snapshot.child("name").getValue(String.class);
                        
                        if (name != null && !name.isEmpty()) {
                            setUpAccountTitle.setText("HI " + name.toUpperCase() + ", SET UP YOUR ACCOUNT");
                        }
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    private void setupClickListeners() {
        card1.setOnClickListener(v -> selectCard(card1, "WL CUBE FIREWALL"));
        card2.setOnClickListener(v -> selectCard(card2, "SMART SLIM"));
        card3.setOnClickListener(v -> selectCard(card3, "COUNTER TOP WATER PURIFIER"));
        card4.setOnClickListener(v -> selectCard(card4, "STANDING WATER PURIFIER"));
    }

    private void selectCard(CardView selectedCard, String unitName) {
        // Reset colors
        card1.setCardBackgroundColor(Color.parseColor("#F8F8F8"));
        card2.setCardBackgroundColor(Color.parseColor("#F8F8F8"));
        card3.setCardBackgroundColor(Color.parseColor("#F8F8F8"));
        card4.setCardBackgroundColor(Color.parseColor("#F8F8F8"));

        // Highlight UI
        selectedCard.setCardBackgroundColor(Color.parseColor("#E3F2FD"));

        // FIREBASE LOGIC: Save the selected unit to the user's profile
        if (userRef != null) {
            userRef.child("selectedUnit").setValue(unitName)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Proceed to details after saving
                            Intent intent = new Intent(CustomerSetupActivity.this, UnitDetailsActivity.class);
                            intent.putExtra("UNIT_NAME", unitName);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(CustomerSetupActivity.this, "Failed to save selection. Try again.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            // Fallback in case userRef is null
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
