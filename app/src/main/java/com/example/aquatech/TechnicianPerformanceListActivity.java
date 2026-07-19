package com.example.aquatech;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TechnicianPerformanceListActivity extends AppCompatActivity {

    private RecyclerView rvTechs;
    private TechPerformanceAdapter adapter;
    private List<TechnicianModel> techList = new ArrayList<>();
    private List<TechnicianModel> filteredList = new ArrayList<>();
    private DatabaseReference usersRef, requestsRef;
    private ProgressBar progressBar;
    
    private DataSnapshot lastUserSnapshot, lastRequestSnapshot;
    
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private final String[] PREDEFINED_TECH_NAMES = {
            "John", "Jhonny Sinder", "Ricky Arcilla", "Ruel Trinidad", "Glenda Delantar",
            "Jerry Duena", "Jonnifer Iglesias", "Marlon Salvador", "Selvino Magora Jr."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tech_performance_list);

        setupStatusBar();
        initializeViews();
        setupFirebase();
    }

    private void initializeViews() {
        findViewById(R.id.btnBackPerfList).setOnClickListener(v -> finish());
        rvTechs = findViewById(R.id.rvTechPerformanceList);
        progressBar = findViewById(R.id.pbLoadingPerf);
        
        rvTechs.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TechPerformanceAdapter(filteredList);
        rvTechs.setAdapter(adapter);

        SearchView searchView = findViewById(R.id.searchViewPerf);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }
            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return true;
            }
        });
    }

    private void setupFirebase() {
        progressBar.setVisibility(View.VISIBLE);
        usersRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users");
        requestsRef = FirebaseDatabase.getInstance(DB_URL).getReference("ServiceRequests");
        
        usersRef.orderByChild("role").equalTo("Technician").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                lastUserSnapshot = snapshot;
                updateFullList();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { progressBar.setVisibility(View.GONE); }
        });

        requestsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                lastRequestSnapshot = snapshot;
                updateFullList();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateFullList() {
        if (lastRequestSnapshot == null) return;

        techList.clear();
        Map<String, TechnicianModel> dbTechs = new HashMap<>();
        
        if (lastUserSnapshot != null) {
            for (DataSnapshot ds : lastUserSnapshot.getChildren()) {
                TechnicianModel tech = ds.getValue(TechnicianModel.class);
                if (tech != null) {
                    if (tech.techId == null) tech.techId = ds.getKey();
                    String nameKey = tech.fullName != null ? tech.fullName.toLowerCase().trim() : "";
                    if (nameKey.isEmpty()) nameKey = tech.name != null ? tech.name.toLowerCase().trim() : "";
                    dbTechs.put(nameKey, tech);
                }
            }
        }

        Map<String, Float> totalRatings = new HashMap<>();
        Map<String, Integer> ratingCounts = new HashMap<>();
        Map<String, Integer> completedCounts = new HashMap<>();

        for (DataSnapshot ds : lastRequestSnapshot.getChildren()) {
            String status = ds.child("status").getValue(String.class);
            String tName = ds.child("assignedTechName").getValue(String.class);
            
            if (tName != null) {
                String key = tName.toLowerCase().trim();
                
                // 📍 Count COMPLETED tickets (approved)
                if ("Completed".equalsIgnoreCase(status)) {
                    completedCounts.put(key, completedCounts.getOrDefault(key, 0) + 1);
                    
                    Object ratingObj = ds.child("rating").getValue();
                    if (ratingObj instanceof Number) {
                        float r = ((Number) ratingObj).floatValue();
                        if (r > 0) {
                            totalRatings.put(key, totalRatings.getOrDefault(key, 0f) + r);
                            ratingCounts.put(key, ratingCounts.getOrDefault(key, 0) + 1);
                        }
                    }
                }
            }
        }

        for (String name : PREDEFINED_TECH_NAMES) {
            String nameKey = name.toLowerCase().trim();
            TechnicianModel tech = dbTechs.get(nameKey);
            
            if (tech == null) {
                tech = new TechnicianModel();
                tech.fullName = name;
                tech.techId = "asc-t-" + nameKey.replace(" ", "");
                tech.accountStatus = "offline";
            }

            // 📍 CALCULATION LOGIC: Scale to 7.0 Performance Rating
            // Quality (0-5 stars)
            float avgStars = ratingCounts.containsKey(nameKey) ? 
                            totalRatings.get(nameKey) / ratingCounts.get(nameKey) : 0;
            
            // Productivity (0-5 score, where 7 tasks = 5.0)
            int completed = completedCounts.getOrDefault(nameKey, 0);
            float productivity = Math.min((completed / 7.0f) * 5.0f, 5.0f);

            // Final Rating = (Quality + Productivity) * 0.7 => Max 10 * 0.7 = 7.0
            if (completed > 0) {
                float baseScore = (avgStars == 0) ? productivity : (avgStars + productivity) / 2.0f;
                // Scale 5.0 base to 7.0 max
                tech.rating = baseScore * 1.4f;
            } else {
                tech.rating = 0.0f;
            }

            techList.add(tech);
        }

        // 📍 SORTING: Highest performance at the top
        Collections.sort(techList, (t1, t2) -> Float.compare(t2.rating, t1.rating));

        filter("");
        progressBar.setVisibility(View.GONE);
    }

    private void filter(String query) {
        filteredList.clear();
        String lowerQuery = query.toLowerCase();
        for (TechnicianModel tech : techList) {
            String name = tech.fullName != null ? tech.fullName.toLowerCase() : "";
            if (name.contains(lowerQuery)) {
                filteredList.add(tech);
            }
        }
        adapter.updateList(filteredList);
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }
}
