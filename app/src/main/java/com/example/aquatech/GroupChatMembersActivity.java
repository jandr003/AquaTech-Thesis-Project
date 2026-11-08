package com.example.aquatech;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

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

public class GroupChatMembersActivity extends AppCompatActivity {

    private RecyclerView rvGroupMembers;
    private TextView tvFullMembersCount;
    private GroupMemberAdapter adapter;
    private final List<Map<String, String>> techList = new ArrayList<>();
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat_members);

        setupStatusBar();
        initializeViews();
        loadGroupMembersData();
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initializeViews() {
        View btnBack = findViewById(R.id.btnBackGroupMembers);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        tvFullMembersCount = findViewById(R.id.tvFullMembersCount);
        rvGroupMembers = findViewById(R.id.rvGroupMembersFull);

        if (rvGroupMembers != null) {
            rvGroupMembers.setLayoutManager(new LinearLayoutManager(this));
            adapter = new GroupMemberAdapter(techList);
            rvGroupMembers.setAdapter(adapter);
        }
    }

    private void loadGroupMembersData() {
        DatabaseReference techRef = FirebaseDatabase.getInstance(DB_URL).getReference("Technicians");
        techRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                techList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String name = ds.child("fullName").getValue(String.class);
                    if (name == null) name = ds.child("name").getValue(String.class);
                    if (name == null) name = "Technician Member";

                    String uid = ds.getKey();
                    String techIdCode = ds.child("techId").getValue(String.class);
                    if (techIdCode == null) techIdCode = "ID: ASC-T00" + (techList.size() + 1);

                    String avatarUrl = ds.child("profileImageUrl").getValue(String.class);

                    Map<String, String> map = new HashMap<>();
                    map.put("name", name);
                    map.put("uid", uid);
                    map.put("techId", techIdCode);
                    map.put("status", "Available");
                    if (avatarUrl != null) map.put("avatarUrl", avatarUrl);

                    techList.add(map);
                }

                if (tvFullMembersCount != null) {
                    tvFullMembersCount.setText(techList.size() + " Active Team Members");
                }
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
