package com.example.aquatech;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class TechnicianDetailActivity extends AppCompatActivity {

    private ImageView ivTechDetailAvatar, ivTechCert;
    private TextView tvTechDetailName, tvTechDetailRole, tvTechDetailExperience, tvTechDetailRating, tvTechDetailJobs, tvTechDetailPhone, tvTechDetailStatus;
    private RecyclerView rvSharedMedia;
    private TextView tvEmptySharedMedia;
    private SharedMediaAdapter sharedMediaAdapter;
    private List<ChatMessage> sharedMediaList;
    private DatabaseReference dbRef;
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_detail);

        setupStatusBar();
        initializeViews();
        loadTechnicianData();
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initializeViews() {
        findViewById(R.id.btnBackTechDetail).setOnClickListener(v -> finish());

        ivTechDetailAvatar = findViewById(R.id.ivTechDetailAvatar);
        ivTechCert = findViewById(R.id.ivTechCert);

        tvTechDetailName = findViewById(R.id.tvTechDetailName);
        tvTechDetailRole = findViewById(R.id.tvTechDetailRole);
        tvTechDetailExperience = findViewById(R.id.tvTechDetailExperience);
        tvTechDetailRating = findViewById(R.id.tvTechDetailRating);
        tvTechDetailJobs = findViewById(R.id.tvTechDetailJobs);
        tvTechDetailPhone = findViewById(R.id.tvTechDetailPhone);
        tvTechDetailStatus = findViewById(R.id.tvTechDetailStatus);

        String intentName = getIntent().getStringExtra("TECH_NAME");
        if (intentName != null && !intentName.isEmpty()) {
            tvTechDetailName.setText(intentName);
        }
    }

    private void loadTechnicianData() {
        String techId = getIntent().getStringExtra("TECH_ID");
        if (techId == null || techId.isEmpty()) return;

        dbRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(techId);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("fullName").getValue(String.class);
                    if (name == null) name = snapshot.child("name").getValue(String.class);
                    if (name != null) tvTechDetailName.setText(name);

                    String phone = snapshot.child("phone").getValue(String.class);
                    if (phone == null) phone = snapshot.child("phoneNumber").getValue(String.class);
                    if (phone != null) tvTechDetailPhone.setText(phone);

                    String avatarUrl = snapshot.child("profileImageUrl").getValue(String.class);
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        Glide.with(TechnicianDetailActivity.this).load(avatarUrl).into(ivTechDetailAvatar);
                    }

                    String certUrl = snapshot.child("verification").child("certUrl").getValue(String.class);
                    if (certUrl != null && !certUrl.isEmpty()) {
                        Glide.with(TechnicianDetailActivity.this).load(certUrl).into(ivTechCert);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        setupSharedMedia();
    }

    private void setupSharedMedia() {
        rvSharedMedia = findViewById(R.id.rvSharedMedia);
        tvEmptySharedMedia = findViewById(R.id.tvEmptySharedMedia);

        if (rvSharedMedia != null) {
            sharedMediaList = new ArrayList<>();
            sharedMediaAdapter = new SharedMediaAdapter(sharedMediaList);
            rvSharedMedia.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            rvSharedMedia.setAdapter(sharedMediaAdapter);

            loadSharedMediaData();
        }
    }

    private void loadSharedMediaData() {
        String techId = getIntent().getStringExtra("TECH_ID");
        String myUid = FirebaseAuth.getInstance().getUid();
        if (techId == null || myUid == null) return;

        String chatId1 = techId + "_" + myUid;
        DatabaseReference chatRef = FirebaseDatabase.getInstance(DB_URL).getReference("UserChats").child(chatId1).child("messages");

        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                sharedMediaList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ChatMessage msg = ds.getValue(ChatMessage.class);
                    if (msg != null && (msg.getFileUri() != null || msg.getFileUrl() != null)) {
                        sharedMediaList.add(msg);
                    }
                }
                if (tvEmptySharedMedia != null) {
                    tvEmptySharedMedia.setVisibility(sharedMediaList.isEmpty() ? View.VISIBLE : View.GONE);
                }
                if (rvSharedMedia != null) {
                    rvSharedMedia.setVisibility(sharedMediaList.isEmpty() ? View.GONE : View.VISIBLE);
                }
                sharedMediaAdapter.notifyDataSetChanged();
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}