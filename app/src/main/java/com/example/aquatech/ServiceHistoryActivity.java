package com.example.aquatech;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ServiceHistoryActivity extends AppCompatActivity {

    private RecyclerView rvCalendar, rvServiceHistory;
    private TextView tvCurrentMonth, tvSelectedDateLabel;
    private ImageView btnBack;
    
    private CalendarAdapter calendarAdapter;
    private List<CalendarItem> calendarList;
    private Calendar currentCalendar = Calendar.getInstance();

    private DatabaseReference dbRef;
    private FirebaseAuth mAuth;
    private String currentUid;
    private List<HistoryItem> historyList;
    private HistoryAdapter historyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_history);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            currentUid = mAuth.getCurrentUser().getUid();
            dbRef = FirebaseDatabase.getInstance().getReference("ServiceRequests");
        }

        setupStatusBar();
        initializeViews();
        setupCalendar(currentCalendar);
        
        // Initial load for today's history
        String today = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(new Date());
        tvSelectedDateLabel.setText("Today, " + today);
        loadHistoryFromFirebase(today);
    }

    private void setupStatusBar() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        }
    }

    private void initializeViews() {
        rvCalendar = findViewById(R.id.rvCalendar);
        rvServiceHistory = findViewById(R.id.rvServiceHistory);
        tvCurrentMonth = findViewById(R.id.tvCurrentMonth);
        tvSelectedDateLabel = findViewById(R.id.tvSelectedDateLabel);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        findViewById(R.id.tvPrevMonth).setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            setupCalendar(currentCalendar);
        });

        findViewById(R.id.tvNextMonth).setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            setupCalendar(currentCalendar);
        });

        historyList = new ArrayList<>();
        historyAdapter = new HistoryAdapter(historyList);
        rvServiceHistory.setLayoutManager(new LinearLayoutManager(this));
        rvServiceHistory.setAdapter(historyAdapter);
    }

    private void setupCalendar(Calendar cal) {
        tvCurrentMonth.setText(new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.getTime()));
        
        calendarList = new ArrayList<>();
        Calendar tempCal = (Calendar) cal.clone();
        tempCal.set(Calendar.DAY_OF_MONTH, 1);
        
        int maxDay = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int i = 1; i <= maxDay; i++) {
            tempCal.set(Calendar.DAY_OF_MONTH, i);
            calendarList.add(new CalendarItem(tempCal.getTime(), i == cal.get(Calendar.DAY_OF_MONTH), "NONE"));
        }

        calendarAdapter = new CalendarAdapter(calendarList, (item, position) -> {
            for (CalendarItem ci : calendarList) ci.setSelected(false);
            item.setSelected(true);
            calendarAdapter.notifyDataSetChanged();
            
            String selectedDate = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(item.getDate());
            tvSelectedDateLabel.setText(selectedDate);
            
            loadHistoryFromFirebase(selectedDate);
        });

        rvCalendar.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvCalendar.setAdapter(calendarAdapter);
        rvCalendar.scrollToPosition(cal.get(Calendar.DAY_OF_MONTH) - 1);
    }

    private void loadHistoryFromFirebase(String dateFilter) {
        if (currentUid == null || dbRef == null) return;

        dbRef.orderByChild("userId").equalTo(currentUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                historyList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String dbDate = ds.child("date").getValue(String.class);
                    
                    if (dateFilter.equals(dbDate)) {
                        String ticketId = ds.child("ticketId").getValue(String.class);
                        String status = ds.child("status").getValue(String.class);
                        Double total = ds.child("totalAmount").getValue(Double.class);
                        
                        // BUILD SUMMARY BASED ON BLUEPRINT ITEMS
                        StringBuilder summaryBuilder = new StringBuilder();
                        if (hasQty(ds, "qty_cbc") || hasQty(ds, "qty_sediment")) summaryBuilder.append("Filter PM, ");
                        if (hasQty(ds, "qty_wayvalve")) summaryBuilder.append("Install Kit, ");
                        if (hasQty(ds, "qty_aquatal") || hasQty(ds, "qty_inline") || hasQty(ds, "qty_uvlamp")) summaryBuilder.append("Parts Replacement, ");
                        
                        String finalSummary = summaryBuilder.toString();
                        if (finalSummary.endsWith(", ")) {
                            finalSummary = finalSummary.substring(0, finalSummary.length() - 2);
                        }
                        if (finalSummary.isEmpty()) finalSummary = "General Service";

                        historyList.add(new HistoryItem(
                                ticketId != null ? "#" + ticketId : "#N/A",
                                dbDate,
                                finalSummary,
                                "₱ " + String.format(Locale.US, "%,.2f", total != null ? total : 0.0),
                                status != null ? status.toUpperCase() : "PENDING"
                        ));
                    }
                }
                historyAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ServiceHistoryActivity.this, "Error fetching history", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean hasQty(DataSnapshot ds, String key) {
        Integer q = ds.child(key).getValue(Integer.class);
        return q != null && q > 0;
    }
}
