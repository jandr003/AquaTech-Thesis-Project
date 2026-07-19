package com.example.aquatech;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
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

public class EditProfileActivity extends AppCompatActivity {

    private ImageView btnBack, ivProfilePic;
    private MaterialCardView cvProfileImage;
    private View btnChangePic;
    private EditText etUsername, etFullName, etNumber, etAddress;
    private MaterialButton btnUpdate;
    private List<MaterialCardView> colorCards = new ArrayList<>();

    private FirebaseAuth mAuth;
    private String currentUid;
    private boolean isTechnician = false;
    private int selectedAvatarResId = R.drawable.man_user_circle_icon;
    private String selectedColorHex = "#F0F7FF";

    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            currentUid = mAuth.getCurrentUser().getUid();
            loadUserData();
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
        }

        setupStatusBar();
        initializeViews();
        setupColorPicker();
        setupClickListeners();
    }

    private void loadUserData() {
        DatabaseReference userRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(currentUid);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String role = snapshot.child("role").getValue(String.class);
                    isTechnician = "Technician".equalsIgnoreCase(role);
                    
                    String username = snapshot.child("username").getValue(String.class);
                    String fullName = snapshot.child("fullName").getValue(String.class);
                    String mobile = snapshot.child("mobile").getValue(String.class);
                    String address = snapshot.child("address").getValue(String.class);
                    String bgColor = snapshot.child("profileBgColor").getValue(String.class);
                    Integer avatarId = snapshot.child("avatarResId").getValue(Integer.class);

                    if (username != null) etUsername.setText(username);
                    if (fullName != null) etFullName.setText(fullName);
                    if (mobile != null) etNumber.setText(mobile);
                    if (address != null) etAddress.setText(address);
                    
                    if (bgColor != null) {
                        selectedColorHex = bgColor;
                        cvProfileImage.setCardBackgroundColor(Color.parseColor(bgColor));
                    }
                    
                    if (avatarId != null && avatarId != 0) {
                        selectedAvatarResId = avatarId;
                        ivProfilePic.setImageResource(selectedAvatarResId);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void saveProfileToFirebase() {
        String username = etUsername.getText().toString().trim();
        String fullName = etFullName.getText().toString().trim();
        String mobile = etNumber.getText().toString().trim();
        String address = etAddress.getText().toString().trim();

        if (fullName.isEmpty() || mobile.isEmpty()) {
            Toast.makeText(this, "Name and mobile are required", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("username", username);
        updates.put("fullName", fullName);
        updates.put("mobile", mobile);
        updates.put("address", address);
        updates.put("profileBgColor", selectedColorHex);
        updates.put("avatarResId", selectedAvatarResId);

        DatabaseReference db = FirebaseDatabase.getInstance(DB_URL).getReference();
        
        // Update Users node
        db.child("Users").child(currentUid).updateChildren(updates);
        
        // If technician, also update Technicians node
        if (isTechnician) {
            db.child("Technicians").child(currentUid).updateChildren(updates);
        }

        Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        ivProfilePic = findViewById(R.id.ivEditProfilePic);
        cvProfileImage = findViewById(R.id.cvProfileImage);
        btnChangePic = findViewById(R.id.btnChangePic);
        etUsername = findViewById(R.id.etEditUsername);
        etFullName = findViewById(R.id.etEditFullName);
        etNumber = findViewById(R.id.etEditNumber);
        etAddress = findViewById(R.id.etEditAddress);
        btnUpdate = findViewById(R.id.btnUpdateProfile);
    }

    private void setupColorPicker() {
        int[] ids = {R.id.colorGray, R.id.colorBlue, R.id.colorPurple, R.id.colorGreen, R.id.colorOrange, R.id.colorYellow, R.id.colorCoral, R.id.colorCyan, R.id.colorPink, R.id.colorBlack};
        String[] colors = {"#CFD8DC", "#90CAF9", "#B39DDB", "#A5D6A7", "#FFCC80", "#FFF59D", "#FFAB91", "#80DEEA", "#F48FB1", "#546E7A"};
        for (int i = 0; i < ids.length; i++) {
            final MaterialCardView card = findViewById(ids[i]);
            final String colorHex = colors[i];
            if (card != null) {
                colorCards.add(card);
                card.setOnClickListener(v -> {
                    for (MaterialCardView c : colorCards) c.setStrokeWidth(0);
                    card.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#00E5FF")));
                    card.setStrokeWidth(6);
                    selectedColorHex = colorHex;
                    cvProfileImage.setCardBackgroundColor(Color.parseColor(colorHex));
                });
            }
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnChangePic.setOnClickListener(v -> showAvatarSelectionDialog());
        btnUpdate.setOnClickListener(v -> saveProfileToFirebase());
    }

    private void showAvatarSelectionDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_select_avatar);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        RecyclerView rvAvatars = dialog.findViewById(R.id.rvAvatars);
        List<AvatarModel> avatarList = isTechnician ? AvatarDataProvider.getTechnicianAvatars() : AvatarDataProvider.getCustomerAvatars();
        AvatarAdapter adapter = new AvatarAdapter(avatarList, avatar -> {
            selectedAvatarResId = avatar.getImageResId();
            ivProfilePic.setImageResource(selectedAvatarResId);
            dialog.dismiss();
        });
        rvAvatars.setLayoutManager(new GridLayoutManager(this, isTechnician ? 2 : 3));
        rvAvatars.setAdapter(adapter);
        dialog.findViewById(R.id.btnCancelAvatar).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}