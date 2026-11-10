package com.example.aquatech;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

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

public class GroupChatNicknamesActivity extends AppCompatActivity {

    private RecyclerView rvNicknames;
    private GroupMemberAdapter adapter;
    private final List<Map<String, String>> techList = new ArrayList<>();
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat_nicknames);

        setupStatusBar();
        initializeViews();
        loadGroupNicknamesData();
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initializeViews() {
        View btnBack = findViewById(R.id.btnBackGroupNicknames);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        rvNicknames = findViewById(R.id.rvGroupNicknamesList);
        if (rvNicknames != null) {
            rvNicknames.setLayoutManager(new LinearLayoutManager(this));
            adapter = new GroupMemberAdapter(techList);
            rvNicknames.setAdapter(adapter);
        }
    }

    private void loadGroupNicknamesData() {
        DatabaseReference techRef = FirebaseDatabase.getInstance(DB_URL).getReference("Technicians");
        techRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                techList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String name = ds.child("fullName").getValue(String.class);
                    if (name == null) name = ds.child("name").getValue(String.class);
                    if (name == null) name = "Technician Member";

                    String nickname = ds.child("nickname").getValue(String.class);
                    String uid = ds.getKey();
                    String avatarUrl = ds.child("profileImageUrl").getValue(String.class);

                    Map<String, String> map = new HashMap<>();
                    map.put("name", name);
                    map.put("uid", uid);
                    map.put("techId", nickname != null ? "Nickname: " + nickname : "Set nickname");
                    map.put("status", "Available");
                    if (avatarUrl != null) map.put("avatarUrl", avatarUrl);

                    techList.add(map);
                }

                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showEditNicknameDialog(String uid, String currentName) {
        EditText etNickname = new EditText(this);
        etNickname.setHint("Type custom nickname...");
        etNickname.setPadding(40, 30, 40, 30);

        new AlertDialog.Builder(this)
                .setTitle("Edit Nickname for " + currentName)
                .setView(etNickname)
                .setPositiveButton("Set", (dialog, which) -> {
                    String newNick = etNickname.getText().toString().trim();
                    if (!newNick.isEmpty()) {
                        FirebaseDatabase.getInstance(DB_URL).getReference("Technicians").child(uid).child("nickname").setValue(newNick);
                        Toast.makeText(this, "Nickname updated!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
