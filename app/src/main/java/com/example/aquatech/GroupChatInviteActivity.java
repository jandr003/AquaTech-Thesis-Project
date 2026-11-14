package com.example.aquatech;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
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
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupChatInviteActivity extends AppCompatActivity {

    private RecyclerView rvInviteList;
    private EditText etSearch;
    private TextView tvUrl;
    private GroupMemberAdapter adapter;
    private final List<Map<String, String>> allTechs = new ArrayList<>();
    private final List<Map<String, String>> filteredTechs = new ArrayList<>();
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat_invite);

        setupStatusBar();
        initializeViews();
        loadTechniciansData();
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initializeViews() {
        View btnBack = findViewById(R.id.btnBackGroupInvite);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        String teamLink = "https://aquatech.smartguard.ph/join/team-chat-" + currentYear;

        tvUrl = findViewById(R.id.tvFullInviteUrl);
        if (tvUrl != null) {
            tvUrl.setText(teamLink);
        }

        View btnCopy = findViewById(R.id.btnCopyShareLink);
        if (btnCopy != null) {
            btnCopy.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Team Chat Link", teamLink);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                }
                Toast.makeText(this, "Invitation link copied to clipboard!", Toast.LENGTH_SHORT).show();
            });
        }

        rvInviteList = findViewById(R.id.rvInviteTechListFull);
        etSearch = findViewById(R.id.etSearchInviteTech);

        if (rvInviteList != null) {
            rvInviteList.setLayoutManager(new LinearLayoutManager(this));
            adapter = new GroupMemberAdapter(filteredTechs);
            rvInviteList.setAdapter(adapter);
        }

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterTechs(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void loadTechniciansData() {
        DatabaseReference techRef = FirebaseDatabase.getInstance(DB_URL).getReference("Technicians");
        techRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allTechs.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String name = ds.child("fullName").getValue(String.class);
                    if (name == null) {
                        name = ds.child("name").getValue(String.class);
                    }
                    if (name == null) {
                        name = "Technician Member";
                    }

                    String uid = ds.getKey();
                    String techIdCode = ds.child("techId").getValue(String.class);
                    if (techIdCode == null) {
                        techIdCode = "ID: ASC-T00" + (allTechs.size() + 1);
                    }

                    String avatarUrl = ds.child("profileImageUrl").getValue(String.class);

                    Map<String, String> map = new HashMap<>();
                    map.put("name", name);
                    map.put("uid", uid);
                    map.put("techId", techIdCode);
                    map.put("status", "Available");
                    if (avatarUrl != null) {
                        map.put("avatarUrl", avatarUrl);
                    }

                    allTechs.add(map);
                }

                filterTechs(etSearch != null ? etSearch.getText().toString() : "");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void filterTechs(String query) {
        filteredTechs.clear();
        String q = query != null ? query.toLowerCase().trim() : "";

        if (q.isEmpty()) {
            filteredTechs.addAll(allTechs);
        } else {
            for (Map<String, String> map : allTechs) {
                String name = map.get("name");
                String techId = map.get("techId");
                if ((name != null && name.toLowerCase().contains(q)) || (techId != null && techId.toLowerCase().contains(q))) {
                    filteredTechs.add(map);
                }
            }
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
}
