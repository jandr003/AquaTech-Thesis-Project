package com.example.aquatech;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupChatInfoActivity extends AppCompatActivity {

    private TextView tvMembersCount;
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat_info);

        setupStatusBar();
        initializeViews();
        loadRealtimeMembersCount();
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initializeViews() {
        tvMembersCount = findViewById(R.id.tvMembersCount);

        View btnBack = findViewById(R.id.btnBackGroupInfo);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View btnMembers = findViewById(R.id.rowSeeMembers);
        if (btnMembers != null) {
            btnMembers.setOnClickListener(v -> showTeamMembersDialog());
        }

        View btnMute = findViewById(R.id.rowMuteChat);
        if (btnMute != null) {
            btnMute.setOnClickListener(v -> showMuteDialog());
        }

        View btnLeave = findViewById(R.id.rowLeaveChat);
        if (btnLeave != null) {
            btnLeave.setOnClickListener(v -> showLeaveChatDialog());
        }
    }

    private void showTeamMembersDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_technician_list);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        View btnClose = dialog.findViewById(R.id.btnCloseDialog);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        RecyclerView rvTechs = dialog.findViewById(R.id.rvDialogTechs);
        View pbLoading = dialog.findViewById(R.id.pbLoadingTechs);
        if (pbLoading != null) pbLoading.setVisibility(View.GONE);

        if (rvTechs != null) {
            rvTechs.setLayoutManager(new LinearLayoutManager(this));
            DatabaseReference techRef = FirebaseDatabase.getInstance(DB_URL).getReference("Technicians");
            techRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<Map<String, String>> techList = new ArrayList<>();
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String name = ds.child("fullName").getValue(String.class);
                        if (name == null) name = ds.child("name").getValue(String.class);
                        if (name == null) name = "Technician Member";

                        String uid = ds.getKey();
                        String techIdCode = ds.child("techId").getValue(String.class);
                        if (techIdCode == null) techIdCode = "ASC-T00" + (techList.size() + 1);

                        Map<String, String> map = new HashMap<>();
                        map.put("name", name);
                        map.put("uid", uid);
                        map.put("techId", techIdCode);
                        map.put("status", "Available");

                        techList.add(map);
                    }
                    DialogTechAdapter adapter = new DialogTechAdapter(techList, (name, uid) -> {
                        Toast.makeText(GroupChatInfoActivity.this, "Team Member: " + name, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                    rvTechs.setAdapter(adapter);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }

        dialog.show();
    }

    private void loadRealtimeMembersCount() {
        DatabaseReference techRef = FirebaseDatabase.getInstance(DB_URL).getReference("Technicians");
        techRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long count = snapshot.getChildrenCount();
                if (count == 0) count = 1;
                if (tvMembersCount != null) {
                    tvMembersCount.setText(count + " members");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showMuteDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_mute_chat);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        View btnCancel = dialog.findViewById(R.id.btnCancelMute);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        View btnConfirm = dialog.findViewById(R.id.btnConfirmMute);
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                Toast.makeText(this, "Team Chat Muted", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    private void showLeaveChatDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Leave Technicians Team Chat?")
                .setMessage("Are you sure you want to leave the team channel?")
                .setPositiveButton("Leave", (dialog, which) -> finish())
                .setNegativeButton("Cancel", null)
                .show();
    }
}
