package com.example.aquatech;

import android.app.AlertDialog;
import android.content.Intent;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
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
import java.util.Random;

public class ManageTechniciansActivity extends AppCompatActivity implements ManageTechniciansAdapter.OnTechActionListener {

    private RecyclerView rvManageTechs;
    private ManageTechniciansAdapter adapter;
    private List<TechnicianModel> allTechs = new ArrayList<>();
    private List<TechnicianModel> filteredList = new ArrayList<>();
    private Map<String, String> techToUidMap = new HashMap<>();
    private DatabaseReference techsRef, usersRef;
    private TabLayout tabLayout;
    private String currentFilter = "Active";
    private String searchQuery = "";

    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_technicians);

        techsRef = FirebaseDatabase.getInstance(DB_URL).getReference("Technicians");
        usersRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users");

        setupStatusBar();
        initializeViews();
        setupFirebase();
    }

    private void initializeViews() {
        findViewById(R.id.btnBackManage).setOnClickListener(v -> finish());
        
        rvManageTechs = findViewById(R.id.rvManageTechs);
        rvManageTechs.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ManageTechniciansAdapter(this, filteredList, this);
        rvManageTechs.setAdapter(adapter);

        tabLayout = findViewById(R.id.tabLayoutTechs);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentFilter = tab.getText().toString();
                applyFilters();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        SearchView searchView = findViewById(R.id.searchViewTechs);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }
            @Override
            public boolean onQueryTextChange(String newText) {
                searchQuery = newText.toLowerCase();
                applyFilters();
                    return true;
            }
        });

        View btnAddTech = findViewById(R.id.btnAddTech);
        if (btnAddTech != null) {
            btnAddTech.setOnClickListener(v -> startActivity(new Intent(this, AdminAddTechnicianActivity.class)));
        }
    }

    private void saveNewTechToDb(String uid, String name, String email) {
        String formattedId = "ASC-T" + (new Random().nextInt(900) + 100);
        Map<String, Object> data = new HashMap<>();
        data.put("userId", uid);
        data.put("techId", formattedId);
        data.put("fullName", name);
        data.put("email", email);
        data.put("role", "Technician");
        data.put("accountStatus", "active");
        data.put("userType", "Technician");

        Map<String, Object> updates = new HashMap<>();
        updates.put("/Users/" + uid, data);
        updates.put("/Technicians/" + uid, data);

        FirebaseDatabase.getInstance(DB_URL).getReference().updateChildren(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Technician account created!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupFirebase() {
        techsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allTechs.clear();
                techToUidMap.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    TechnicianModel tech = ds.getValue(TechnicianModel.class);
                    if (tech != null) {
                        String uid = ds.getKey();
                        techToUidMap.put(tech.email != null ? tech.email : uid, uid);
                        if (tech.techId == null || tech.techId.isEmpty() || tech.techId.length() > 15) {
                             tech.techId = uid; 
                        }
                        allTechs.add(tech);
                    }
                }
                applyFilters();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void applyFilters() {
        filteredList.clear();
        for (TechnicianModel tech : allTechs) {
            String name = tech.fullName != null ? tech.fullName.toLowerCase() : "";
            if (!name.contains(searchQuery)) continue;

            String status = "Pending";
            if ("disabled".equalsIgnoreCase(tech.accountStatus)) {
                status = "Disabled";
            } else if (tech.verification != null && "Verified".equalsIgnoreCase(tech.verification.status)) {
                status = "Active";
            } else if ("active".equalsIgnoreCase(tech.accountStatus)) {
                status = "Active";
            }

            if (status.equalsIgnoreCase(currentFilter)) {
                filteredList.add(tech);
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onViewVerification(TechnicianModel tech) {
        if (tech != null && tech.fullName != null) {
            Toast.makeText(this, "Technician: " + tech.fullName + " (Status: " + tech.accountStatus + ")", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeleteTech(TechnicianModel tech) {
        String uidToDelete = techToUidMap.get(tech.email);
        if (uidToDelete == null) uidToDelete = tech.techId;

        final String finalUid = uidToDelete;

        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to PERMANENTLY REMOVE " + tech.fullName + "?\n\nThis will delete them from the database and assignment lists.")
                .setPositiveButton("Delete Permanently", (dialog, which) -> {
                    DatabaseReference rootRef = FirebaseDatabase.getInstance(DB_URL).getReference();

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("/Users/" + finalUid, null);
                    updates.put("/Technicians/" + finalUid, null);
                    
                    rootRef.updateChildren(updates).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Technician deleted from system", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Failed to delete: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }
}
