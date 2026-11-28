package com.example.aquatech;

import android.content.Intent;
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
import java.util.Locale;

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
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initializeViews() {
        View btnBack = findViewById(R.id.btnBackTechDetail);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                finish();
            });
        }

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
            if (tvTechDetailName != null) {
                tvTechDetailName.setText(intentName);
            }
        }
    }

    private void loadTechnicianData() {
        String techId = getIntent().getStringExtra("TECH_ID");
        if (techId == null || techId.isEmpty()) {
            return;
        }

        dbRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(techId);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (snapshot.exists()) {
                    String name = snapshot.child("fullName").getValue(String.class);
                    if (name == null || name.isEmpty()) {
                        name = snapshot.child("name").getValue(String.class);
                    }

                    if (name != null && !name.isEmpty() && tvTechDetailName != null) {
                        tvTechDetailName.setText(name);
                    }

                    String phone = snapshot.child("phone").getValue(String.class);
                    if (phone == null || phone.isEmpty()) {
                        phone = snapshot.child("phoneNumber").getValue(String.class);
                    }

                    if (phone != null && !phone.isEmpty() && tvTechDetailPhone != null) {
                        tvTechDetailPhone.setText(phone);
                    }

                    String experience = snapshot.child("experienceYears").getValue(String.class);
                    if (experience == null || experience.isEmpty()) {
                        experience = snapshot.child("experience").getValue(String.class);
                    }

                    if (tvTechDetailExperience != null) {
                        if (experience != null && !experience.isEmpty()) {
                            tvTechDetailExperience.setText(experience + " Experience with AquaTech");
                        } else {
                            tvTechDetailExperience.setText("3+ Years Experience with AquaTech");
                        }
                    }

                    String role = snapshot.child("role").getValue(String.class);
                    if (tvTechDetailRole != null && role != null && !role.isEmpty()) {
                        tvTechDetailRole.setText(role);
                    }

                    Double ratingVal = snapshot.child("rating").getValue(Double.class);
                    if (tvTechDetailRating != null) {
                        if (ratingVal != null && ratingVal > 0) {
                            tvTechDetailRating.setText(String.format(Locale.US, "⭐ %.1f / 5.0", ratingVal));
                        } else {
                            tvTechDetailRating.setText("⭐ 4.9 / 5.0");
                        }
                    }

                    Long jobsCount = snapshot.child("completedJobs").getValue(Long.class);
                    if (tvTechDetailJobs != null) {
                        if (jobsCount != null && jobsCount > 0) {
                            tvTechDetailJobs.setText(jobsCount + "+ Jobs");
                        } else {
                            tvTechDetailJobs.setText("120+ Jobs");
                        }
                    }

                    int[] techAvatars = {
                            R.drawable.technician_avatar1,
                            R.drawable.technician_avatar2,
                            R.drawable.technician_avatar3,
                            R.drawable.technician_avatar4,
                            R.drawable.technician_avatar5,
                            R.drawable.technician_avatar6,
                            R.drawable.technician_avatar7,
                            R.drawable.technician_avatar8
                    };
                    int defaultAvatar = techAvatars[Math.abs(techId.hashCode()) % techAvatars.length];

                    String avatarUrl = snapshot.child("profileImageUrl").getValue(String.class);
                    if (avatarUrl != null && !avatarUrl.isEmpty() && ivTechDetailAvatar != null) {
                        Glide.with(TechnicianDetailActivity.this).load(avatarUrl).placeholder(defaultAvatar).into(ivTechDetailAvatar);
                    } else if (ivTechDetailAvatar != null) {
                        ivTechDetailAvatar.setImageResource(defaultAvatar);
                    }

                    String certUrl = snapshot.child("verification").child("certUrl").getValue(String.class);
                    if (certUrl != null && !certUrl.isEmpty() && ivTechCert != null) {
                        Glide.with(TechnicianDetailActivity.this).load(certUrl).into(ivTechCert);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        setupSharedMedia();
    }

    private void setupSharedMedia() {
        rvSharedMedia = findViewById(R.id.rvSharedMedia);
        tvEmptySharedMedia = findViewById(R.id.tvEmptySharedMedia);

        View chipMedia = findViewById(R.id.chipTechMedia);
        View chipFiles = findViewById(R.id.chipTechFiles);
        View chipLinks = findViewById(R.id.chipTechLinks);

        View.OnClickListener openFullSharedMedia = v -> {
            String techId = getIntent().getStringExtra("TECH_ID");
            String myUid = FirebaseAuth.getInstance().getUid();
            String chatId = (techId != null && myUid != null) ? techId + "_" + myUid : null;

            Intent intent = new Intent(TechnicianDetailActivity.this, GroupSharedMediaActivity.class);
            if (chatId != null) {
                intent.putExtra("CHAT_ID", chatId);
            }
            startActivity(intent);
        };

        if (chipMedia != null) {
            chipMedia.setOnClickListener(openFullSharedMedia);
        }
        if (chipFiles != null) {
            chipFiles.setOnClickListener(openFullSharedMedia);
        }
        if (chipLinks != null) {
            chipLinks.setOnClickListener(openFullSharedMedia);
        }

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
        if (techId == null || myUid == null) {
            return;
        }

        String chatId1 = techId + "_" + myUid;
        DatabaseReference chatRef = FirebaseDatabase.getInstance(DB_URL).getReference("UserChats").child(chatId1).child("messages");

        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (sharedMediaList != null) {
                    sharedMediaList.clear();
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        ChatMessage msg = ds.getValue(ChatMessage.class);
                        if (msg != null && (msg.getFileUri() != null || msg.getFileUrl() != null)) {
                            sharedMediaList.add(msg);
                        }
                    }
                }

                if (tvEmptySharedMedia != null && sharedMediaList != null) {
                    tvEmptySharedMedia.setVisibility(sharedMediaList.isEmpty() ? View.VISIBLE : View.GONE);
                }

                if (rvSharedMedia != null && sharedMediaList != null) {
                    rvSharedMedia.setVisibility(sharedMediaList.isEmpty() ? View.GONE : View.VISIBLE);
                }

                if (sharedMediaAdapter != null) {
                    sharedMediaAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }
}
