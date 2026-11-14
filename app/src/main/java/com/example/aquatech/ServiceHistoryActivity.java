package com.example.aquatech;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
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

    private static final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private RecyclerView rvCalendar, rvServiceHistory;
    private TextView tvCurrentMonth, tvSelectedDateLabel;
    private ImageView btnBack;

    private CalendarAdapter calendarAdapter;
    private List<CalendarItem> calendarList;
    private final Calendar currentCalendar = Calendar.getInstance();

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
            dbRef = FirebaseDatabase.getInstance(DB_URL).getReference("ServiceRequests");
        }

        setupStatusBar();
        initializeViews();
        setupCalendar(currentCalendar);

        String today = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(new Date());
        if (tvSelectedDateLabel != null) {
            tvSelectedDateLabel.setText("Today, " + today);
        }
        loadHistoryFromFirebase(today);
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initializeViews() {
        rvCalendar = findViewById(R.id.rvCalendar);
        rvServiceHistory = findViewById(R.id.rvServiceHistory);
        tvCurrentMonth = findViewById(R.id.tvCurrentMonth);
        tvSelectedDateLabel = findViewById(R.id.tvSelectedDateLabel);
        btnBack = findViewById(R.id.btnBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View btnPrev = findViewById(R.id.tvPrevMonth);
        if (btnPrev != null) {
            btnPrev.setOnClickListener(v -> {
                currentCalendar.add(Calendar.MONTH, -1);
                setupCalendar(currentCalendar);
            });
        }

        View btnNext = findViewById(R.id.tvNextMonth);
        if (btnNext != null) {
            btnNext.setOnClickListener(v -> {
                currentCalendar.add(Calendar.MONTH, 1);
                setupCalendar(currentCalendar);
            });
        }

        historyList = new ArrayList<>();
        historyAdapter = new HistoryAdapter(historyList);
        if (rvServiceHistory != null) {
            rvServiceHistory.setLayoutManager(new LinearLayoutManager(this));
            rvServiceHistory.setAdapter(historyAdapter);
        }
    }

    private void setupCalendar(Calendar cal) {
        if (cal == null) {
            return;
        }

        if (tvCurrentMonth != null) {
            tvCurrentMonth.setText(new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.getTime()));
        }

        calendarList = new ArrayList<>();
        Calendar tempCal = (Calendar) cal.clone();
        tempCal.set(Calendar.DAY_OF_MONTH, 1);

        int maxDay = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int i = 1; i <= maxDay; i++) {
            tempCal.set(Calendar.DAY_OF_MONTH, i);
            calendarList.add(new CalendarItem(tempCal.getTime(), i == cal.get(Calendar.DAY_OF_MONTH), "NONE"));
        }

        calendarAdapter = new CalendarAdapter(calendarList, (item, position) -> {
            if (calendarList != null) {
                for (CalendarItem ci : calendarList) {
                    if (ci != null) {
                        ci.setSelected(false);
                    }
                }
            }

            if (item != null) {
                item.setSelected(true);
            }

            if (calendarAdapter != null) {
                calendarAdapter.notifyDataSetChanged();
            }

            if (item != null) {
                String selectedDate = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(item.getDate());
                if (tvSelectedDateLabel != null) {
                    tvSelectedDateLabel.setText(selectedDate);
                }
                loadHistoryFromFirebase(selectedDate);
            }
        });

        if (rvCalendar != null) {
            rvCalendar.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            rvCalendar.setAdapter(calendarAdapter);
            rvCalendar.scrollToPosition(cal.get(Calendar.DAY_OF_MONTH) - 1);
        }
    }

    private void loadHistoryFromFirebase(String dateFilter) {
        if (currentUid == null || dbRef == null || dateFilter == null) {
            return;
        }

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

                        StringBuilder summaryBuilder = new StringBuilder();
                        if (hasQty(ds, "qty_cbc") || hasQty(ds, "qty_sediment")) {
                            summaryBuilder.append("Filter PM, ");
                        }

                        if (hasQty(ds, "qty_wayvalve")) {
                            summaryBuilder.append("Install Kit, ");
                        }

                        if (hasQty(ds, "qty_aquatal") || hasQty(ds, "qty_inline") || hasQty(ds, "qty_uvlamp")) {
                            summaryBuilder.append("Parts Replacement, ");
                        }

                        String finalSummary = summaryBuilder.toString();
                        if (finalSummary.endsWith(", ")) {
                            finalSummary = finalSummary.substring(0, finalSummary.length() - 2);
                        }

                        if (finalSummary.isEmpty()) {
                            finalSummary = "General Service";
                        }

                        historyList.add(new HistoryItem(
                                ticketId != null ? "#" + ticketId : "#N/A",
                                dbDate,
                                finalSummary,
                                "₱ " + String.format(Locale.US, "%,.2f", total != null ? total : 0.0),
                                status != null ? status.toUpperCase() : "PENDING"
                        ));
                    }
                }

                if (historyAdapter != null) {
                    historyAdapter.notifyDataSetChanged();
                }
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
