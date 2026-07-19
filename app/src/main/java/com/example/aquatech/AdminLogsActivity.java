package com.example.aquatech;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
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
import java.util.Collections;
import java.util.List;

public class AdminLogsActivity extends AppCompatActivity {

    private RecyclerView rvAdminLogs;
    private AdminLogAdapter logAdapter;
    private List<AdminLogModel> logList = new ArrayList<>();
    private DatabaseReference logsRef;
    private static final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_logs);

        setupStatusBar();
        initializeViews();
        setupFirebase();
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initializeViews() {
        findViewById(R.id.btnBackLogs).setOnClickListener(v -> finish());
        rvAdminLogs = findViewById(R.id.rvAdminLogs);
        rvAdminLogs.setLayoutManager(new LinearLayoutManager(this));
        logAdapter = new AdminLogAdapter(logList);
        rvAdminLogs.setAdapter(logAdapter);
    }

    private void setupFirebase() {
        logsRef = FirebaseDatabase.getInstance(DB_URL).getReference("AdminLogs");
        logsRef.orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                logList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    AdminLogModel log = ds.getValue(AdminLogModel.class);
                    if (log != null) logList.add(log);
                }
                Collections.reverse(logList);
                logAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
