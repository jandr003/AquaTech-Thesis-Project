package com.example.aquatech;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MonthlyAnalyticsActivity extends AppCompatActivity {

    private TextView tvTotalJobs, tvCompletionRate, tvAvgResponse, tvResolutionTime, tvCsatScore, tvRateTrend;
    private View barW1, barW2, barW3, barW4;
    private RecyclerView rvMonthlyLog;
    private SearchView searchAuditLog;
    private DatabaseReference dbRef;

    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private List<RequestLogModel> logList = new ArrayList<>();
    private List<RequestLogModel> filteredList = new ArrayList<>();
    private RequestLogAdapter logAdapter;

    private int totalCount = 0, completedCount = 0, inProgressCount = 0, pendingCount = 0;
    private long totalResponseTime = 0, totalResolutionTime = 0;
    private int responseCount = 0, resolutionCount = 0;
    private float totalRating = 0;
    private int ratingCount = 0;
    private int[] weeklyCounts = new int[4];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monthly_analytics);

        setupStatusBar();
        initializeViews();
        setupFirebase();
        setupSearch();
    }

    private void initializeViews() {
        tvTotalJobs = findViewById(R.id.tvTotalMonthlyJobsSnapshot);
        tvCompletionRate = findViewById(R.id.tvCompletionRate);
        tvAvgResponse = findViewById(R.id.tvAvgResponse);
        tvResolutionTime = findViewById(R.id.tvResolutionTime);
        tvCsatScore = findViewById(R.id.tvCsatScore);
        tvRateTrend = findViewById(R.id.tvRateTrend);
        searchAuditLog = findViewById(R.id.searchAuditLog);

        barW1 = findViewById(R.id.barW1);
        barW2 = findViewById(R.id.barW2);
        barW3 = findViewById(R.id.barW3);
        barW4 = findViewById(R.id.barW4);

        rvMonthlyLog = findViewById(R.id.rvMonthlyDetailedLog);
        rvMonthlyLog.setLayoutManager(new LinearLayoutManager(this));
        logAdapter = new RequestLogAdapter(filteredList);
        rvMonthlyLog.setAdapter(logAdapter);

        findViewById(R.id.btnBackAnalytics).setOnClickListener(v -> finish());
        findViewById(R.id.btnExportMonthlyPdf).setOnClickListener(v -> generateMonthlyReportPDF());
    }

    private void setupFirebase() {
        dbRef = FirebaseDatabase.getInstance(DB_URL).getReference("ServiceRequests");

        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                resetStats();

                Calendar now = Calendar.getInstance();
                int currentMonth = now.get(Calendar.MONTH);
                int currentYear = now.get(Calendar.YEAR);

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String ticketId = ds.child("ticketId").getValue(String.class);
                    if (ticketId == null) ticketId = ds.getKey();

                    String customerName = ds.child("customerName").getValue(String.class);
                    if (customerName == null) customerName = "Unknown";

                    String customerId = ds.child("customerId").getValue(String.class);
                    if (customerId == null) {
                        String userId = ds.child("userId").getValue(String.class);
                        if (userId != null && userId.length() > 5) {
                            customerId = "CUST-" + userId.substring(0, 5);
                        } else {
                            customerId = "CUST-001";
                        }
                    }

                    String status = ds.child("status").getValue(String.class);
                    if (status == null) status = "Pending";

                    String techName = ds.child("assignedTechName").getValue(String.class);
                    if (techName == null) techName = "Unassigned";

                    String techId = ds.child("assignedTechId").getValue(String.class);
                    if (techId == null) techId = "";

                    Long timestamp = ds.child("timestamp").getValue(Long.class);
                    Long assignedTime = ds.child("assignedTimestamp").getValue(Long.class);
                    Long completedTime = ds.child("completedTimestamp").getValue(Long.class);
                    if (completedTime == null) completedTime = ds.child("maintenanceEndTime").getValue(Long.class);

                    Float rating = ds.child("rating").getValue(Float.class);

                    String serviceType = getServiceTypeLabel(ds);

                    boolean isCurrentMonth = false;
                    if (timestamp != null) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(timestamp);
                        isCurrentMonth = (cal.get(Calendar.MONTH) == currentMonth &&
                                cal.get(Calendar.YEAR) == currentYear);
                    }

                    RequestLogModel item = new RequestLogModel(
                            ticketId,
                            customerName,
                            customerId,
                            techName,
                            techId,
                            status,
                            serviceType,
                            timestamp,
                            assignedTime,
                            completedTime,
                            rating
                    );

                    logList.add(item);

                    if (isCurrentMonth) {
                        totalCount++;

                        if (timestamp != null) {
                            int week = getWeekOfMonth(timestamp);
                            if (week >= 1 && week <= 4) weeklyCounts[week - 1]++;
                        }

                        if ("Completed".equalsIgnoreCase(status)) {
                            completedCount++;
                            if (rating != null && rating > 0) {
                                totalRating += rating;
                                ratingCount++;
                            }
                            if (assignedTime != null && completedTime != null && completedTime > assignedTime) {
                                totalResolutionTime += (completedTime - assignedTime) / (1000 * 60);
                                resolutionCount++;
                            }
                        } else if ("In Progress".equalsIgnoreCase(status) || "Ongoing".equalsIgnoreCase(status) || "Arrived".equalsIgnoreCase(status)) {
                            inProgressCount++;
                        } else {
                            pendingCount++;
                        }

                        if (timestamp != null && assignedTime != null && assignedTime > timestamp) {
                            totalResponseTime += (assignedTime - timestamp) / (1000 * 60);
                            responseCount++;
                        }
                    }
                }

                filterList("");
                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MonthlyAnalyticsActivity.this, "Firebase error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getServiceTypeLabel(DataSnapshot ds) {
        StringBuilder sb = new StringBuilder();
        if (hasQty(ds, "qty_cbc") || hasQty(ds, "qty_sediment") || hasQty(ds, "qty_smsf1") || hasQty(ds, "qty_smsf10"))
            sb.append("Filter Preventive");
        if (hasQty(ds, "qty_wayvalve")) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("Installation Kit");
        }
        if (hasQty(ds, "qty_aquatal") || hasQty(ds, "qty_inline") || hasQty(ds, "qty_uvlamp") ||
                hasQty(ds, "qty_touchpanel") || hasQty(ds, "qty_pbcboard")) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("Other Parts");
        }
        return sb.length() == 0 ? "General Maintenance" : sb.toString();
    }

    private boolean hasQty(DataSnapshot ds, String key) {
        Object val = ds.child(key).getValue();
        return val instanceof Number && ((Number) val).intValue() > 0;
    }

    private void setupSearch() {
        searchAuditLog.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterList(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterList(newText);
                return true;
            }
        });
    }

    private void filterList(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(logList);
        } else {
            String q = query.toLowerCase();
            for (RequestLogModel item : logList) {
                if (item.getTicketId().toLowerCase().contains(q) ||
                        (item.getCustomerName() != null && item.getCustomerName().toLowerCase().contains(q)) ||
                        item.getStatus().toLowerCase().contains(q)) {
                    filteredList.add(item);
                }
            }
        }
        logAdapter.notifyDataSetChanged();
    }

    private void updateUI() {
        tvTotalJobs.setText(String.valueOf(totalCount));
        if (totalCount > 0) {
            int rate = (completedCount * 100) / totalCount;
            tvCompletionRate.setText(rate + "%");
            tvRateTrend.setText(rate >= 80 ? "↑ High" : (rate >= 50 ? "→ Stable" : "↓ Low"));
        } else {
            tvCompletionRate.setText("0%");
            tvRateTrend.setText("No Data");
        }

        long avgResponse = responseCount > 0 ? totalResponseTime / responseCount : 0;
        tvAvgResponse.setText(avgResponse + " mins");

        long avgResolution = resolutionCount > 0 ? totalResolutionTime / resolutionCount : 0;
        tvResolutionTime.setText(avgResolution + " mins");

        tvCsatScore.setText("⭐ " + (ratingCount > 0 ? String.format(Locale.US, "%.1f", totalRating / ratingCount) : "0.0") +
                (ratingCount > 0 ? " (" + ratingCount + ")" : ""));

        updateBars();
    }

    private void updateBars() {
        int max = 1;
        for (int c : weeklyCounts) if (c > max) max = c;
        setBarHeight(barW1, weeklyCounts[0], max);
        setBarHeight(barW2, weeklyCounts[1], max);
        setBarHeight(barW3, weeklyCounts[2], max);
        setBarHeight(barW4, weeklyCounts[3], max);
    }

    private void setBarHeight(View bar, int count, int max) {
        if (bar == null) return;
        ViewGroup.LayoutParams params = bar.getLayoutParams();
        params.height = Math.max(20, (count * 300) / (max == 0 ? 1 : max));
        bar.setLayoutParams(params);
    }

    private int getWeekOfMonth(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        if (day <= 7) return 1;
        if (day <= 14) return 2;
        if (day <= 21) return 3;
        return 4;
    }

    private void generateMonthlyReportPDF() {
        PdfDocument document = new PdfDocument();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        int pageWidth = 595, pageHeight = 842, margin = 40, y = 50;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // Header
        titlePaint.setColor(Color.parseColor("#1E3A8A"));
        titlePaint.setTextSize(24f);
        canvas.drawText("AQUATECH MONTHLY ANALYTICS", margin, y, titlePaint);
        y += 30;

        paint.setColor(Color.GRAY);
        paint.setTextSize(11f);
        String dateStr = new SimpleDateFormat("MMMM dd, yyyy hh:mm a", Locale.getDefault()).format(new Date());
        canvas.drawText("Generated on: " + dateStr, margin, y, paint);
        y += 30;

        // Summary
        paint.setColor(Color.parseColor("#F3F4F6"));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(margin, y, pageWidth - margin, y + 120, paint);
        paint.setColor(Color.BLACK);
        paint.setTextSize(14f);
        paint.setFakeBoldText(true);
        canvas.drawText("MONTHLY PERFORMANCE SUMMARY", margin + 15, y + 25, paint);
        paint.setFakeBoldText(false);
        paint.setTextSize(12f);
        canvas.drawText("Total Requests: " + totalCount, margin + 25, y + 50, paint);
        canvas.drawText("Completed: " + completedCount, margin + 25, y + 75, paint);
        canvas.drawText("In Progress: " + inProgressCount, margin + 200, y + 50, paint);
        canvas.drawText("Pending: " + pendingCount, margin + 200, y + 75, paint);
        canvas.drawText("Success Rate: " + tvCompletionRate.getText(), margin + 350, y + 50, paint);
        canvas.drawText("Avg Rating: " + tvCsatScore.getText(), margin + 350, y + 75, paint);
        y += 140;

        // Weekly trends
        paint.setFakeBoldText(true);
        paint.setTextSize(14f);
        canvas.drawText("WEEKLY GROWTH TRENDS", margin, y, paint);
        y += 30;

        int barWidth = 80, barSpacing = 40, graphBottom = y + 150, maxHeight = 120;
        int maxWeek = 1;
        for (int c : weeklyCounts) if (c > maxWeek) maxWeek = c;
        String[] weeks = {"Week 1", "Week 2", "Week 3", "Week 4"};
        for (int i = 0; i < 4; i++) {
            int barX = margin + i * (barWidth + barSpacing);
            int barHeight = (int) ((float) weeklyCounts[i] / maxWeek * maxHeight);
            paint.setColor(Color.parseColor("#4B91C6"));
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(barX, graphBottom - barHeight, barX + barWidth, graphBottom, paint);
            paint.setColor(Color.BLACK);
            paint.setTextSize(10f);
            canvas.drawText(String.valueOf(weeklyCounts[i]), barX + barWidth / 2 - 10, graphBottom - barHeight - 5, paint);
            canvas.drawText(weeks[i], barX, graphBottom + 20, paint);
        }
        y = graphBottom + 60;

        // Request log table
        paint.setFakeBoldText(true);
        paint.setTextSize(14f);
        canvas.drawText("RECENT REQUEST LOG", margin, y, paint);
        y += 25;

        // Table header
        paint.setColor(Color.parseColor("#E5E7EB"));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(margin, y, pageWidth - margin, y + 25, paint);
        paint.setColor(Color.BLACK);
        paint.setTextSize(10f);
        paint.setFakeBoldText(true);
        canvas.drawText("TICKET ID", margin + 10, y + 18, paint);
        canvas.drawText("CUSTOMER ID", margin + 150, y + 18, paint);
        canvas.drawText("STATUS", margin + 350, y + 18, paint);
        y += 30;

        paint.setFakeBoldText(false);
        int limit = Math.min(filteredList.size(), 15);
        for (int i = 0; i < limit; i++) {
            RequestLogModel item = filteredList.get(i);
            canvas.drawText(item.getTicketId(), margin + 10, y, paint);
            String customerDisplay = item.getCustomerId() != null ? item.getCustomerId() : item.getCustomerName();
            canvas.drawText(truncate(customerDisplay, 15), margin + 150, y, paint);
            if ("Completed".equalsIgnoreCase(item.getStatus())) {
                paint.setColor(Color.parseColor("#4CAF50"));
            } else if ("In Progress".equalsIgnoreCase(item.getStatus())) {
                paint.setColor(Color.parseColor("#FF9800"));
            } else {
                paint.setColor(Color.parseColor("#9E9E9E"));
            }
            canvas.drawText(item.getStatus(), margin + 350, y, paint);
            paint.setColor(Color.BLACK);
            y += 20;
        }

        document.finishPage(page);
        saveAndOpenPdf(document);
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen - 2) + ".." : text;
    }

    private void saveAndOpenPdf(PdfDocument document) {
        String fileName = "AquaTech_Monthly_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".pdf";
        File pdfFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);

        try {
            // 1. Write to internal storage first (never close document yet)
            FileOutputStream fos = new FileOutputStream(pdfFile);
            document.writeTo(fos);
            fos.close();

            // 2. Save to Downloads by copying the file (document is still open for now)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues cv = new ContentValues();
                cv.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                cv.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                cv.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AquaTech");
                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                if (uri != null) {
                    OutputStream publicFos = getContentResolver().openOutputStream(uri);
                    FileInputStream fis = new FileInputStream(pdfFile);
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        publicFos.write(buffer, 0, length);
                    }
                    fis.close();
                    publicFos.close();
                    Toast.makeText(this, "PDF saved to Downloads/AquaTech folder", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Could not save to Downloads, but file is saved internally", Toast.LENGTH_LONG).show();
                }
            } else {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    File publicFile = new File(downloadsDir, fileName);
                    FileOutputStream publicFos = new FileOutputStream(publicFile);
                    FileInputStream fis = new FileInputStream(pdfFile);
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        publicFos.write(buffer, 0, length);
                    }
                    fis.close();
                    publicFos.close();
                    Toast.makeText(this, "PDF saved to Downloads folder", Toast.LENGTH_LONG).show();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
                    Toast.makeText(this, "Storage permission needed to save to Downloads", Toast.LENGTH_SHORT).show();
                }
            }

            // 3. Close the document after all writes are done
            document.close();

            // 4. Open the PDF from internal storage
            Uri pdfUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", pdfFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(pdfUri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Open PDF"));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "PDF Export Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void resetStats() {
        totalCount = 0; completedCount = 0; inProgressCount = 0; pendingCount = 0;
        totalResponseTime = 0; totalResolutionTime = 0;
        responseCount = 0; resolutionCount = 0;
        totalRating = 0; ratingCount = 0;
        logList.clear();
        for (int i = 0; i < 4; i++) weeklyCounts[i] = 0;
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }
}