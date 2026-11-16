package com.example.aquatech;

import android.content.ContentValues;
import android.content.Intent;
import android.content.res.ColorStateList;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TechnicianPerformanceActivity extends AppCompatActivity {

    private ImageView ivAvatar, ivIDPreview, ivCertPreview;
    private TextView tvName, tvID, tvStatus, tvCompleted, tvOngoing, tvRating;
    private RecyclerView rvHistory;
    private MaterialButton btnLiveTrack;
    private String techId, techName;
    private DatabaseReference requestsRef, techRef;
    private List<HistoryItem> historyList;
    private HistoryAdapter historyAdapter;

    private boolean hasActiveRequest = false;
    private String activeTicketId = null;
    private String activeCustomerName = null;

    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_performance);

        setupStatusBar();
        initializeViews();

        techId = getIntent().getStringExtra("TECH_ID");
        techName = getIntent().getStringExtra("TECH_NAME");

        if (techId == null) {
            Toast.makeText(this, "Technician ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (techName == null) {
            techName = "Technician";
        }

        if (tvName != null) {
            tvName.setText(techName);
        }

        if (tvID != null) {
            tvID.setText("ID: " + techId);
        }

        setupRecyclerView();
        setupFirebase();
        loadTechnicianCredentials();
        setupButtons();

        View btnExport = findViewById(R.id.btnExportPdf);
        if (btnExport != null) {
            btnExport.setOnClickListener(v -> generatePerformanceReportPDF());
        }
    }

    private void initializeViews() {
        ivAvatar = findViewById(R.id.ivTechAvatarPerformance);
        ivIDPreview = findViewById(R.id.ivTechIDPreview);
        ivCertPreview = findViewById(R.id.ivTechCertPreview);
        tvName = findViewById(R.id.tvTechNamePerformance);
        tvID = findViewById(R.id.tvTechID);
        tvStatus = findViewById(R.id.tvTechStatusPerformance);
        tvCompleted = findViewById(R.id.tvCompletedCount);
        tvOngoing = findViewById(R.id.tvActiveCount);
        tvRating = findViewById(R.id.tvRatingStat);
        rvHistory = findViewById(R.id.rvTechHistory);
        btnLiveTrack = findViewById(R.id.btnLiveTrack);

        View btnBack = findViewById(R.id.btnBackPerformance);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void setupRecyclerView() {
        if (rvHistory != null) {
            rvHistory.setLayoutManager(new LinearLayoutManager(this));
            historyList = new ArrayList<>();
            historyAdapter = new HistoryAdapter(historyList);
            rvHistory.setAdapter(historyAdapter);
        }
    }

    private void setupFirebase() {
        requestsRef = FirebaseDatabase.getInstance(DB_URL).getReference("ServiceRequests");

        requestsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int completed = 0;
                int ongoing = 0;
                float totalRating = 0;
                int ratingCount = 0;

                if (historyList != null) {
                    historyList.clear();
                } else {
                    historyList = new ArrayList<>();
                }

                hasActiveRequest = false;
                activeTicketId = null;
                activeCustomerName = null;

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String assignedId = ds.child("assignedTechId").getValue(String.class);
                    String assignedName = ds.child("assignedTechName").getValue(String.class);

                    boolean isMatch = (assignedId != null && assignedId.equals(techId)) ||
                            (assignedName != null && assignedName.equalsIgnoreCase(techName));

                    if (isMatch) {
                        String status = ds.child("status").getValue(String.class);
                        if (status == null) {
                            status = "Pending";
                        }

                        String ticketId = ds.child("ticketId").getValue(String.class);
                        if (ticketId == null) {
                            ticketId = ds.getKey();
                        }

                        String customerName = ds.child("customerName").getValue(String.class);
                        if (customerName == null) {
                            customerName = "Customer";
                        }

                        String serviceType = getServiceTypeLabel(ds);
                        Long completionTime = ds.child("completedTimestamp").getValue(Long.class);
                        if (completionTime == null) {
                            completionTime = ds.child("maintenanceEndTime").getValue(Long.class);
                        }

                        String dateStr = "In Progress";
                        if ("Completed".equalsIgnoreCase(status) && completionTime != null) {
                            dateStr = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date(completionTime));
                        }

                        historyList.add(new HistoryItem(ticketId, customerName, serviceType, dateStr, status));

                        if ("Completed".equalsIgnoreCase(status)) {
                            completed++;
                            Float rating = ds.child("rating").getValue(Float.class);
                            if (rating != null && rating > 0) {
                                totalRating += rating;
                                ratingCount++;
                            }
                        } else if ("In Progress".equalsIgnoreCase(status) || "Ongoing".equalsIgnoreCase(status) || "Arrived".equalsIgnoreCase(status)) {
                            ongoing++;
                            hasActiveRequest = true;
                            activeTicketId = ticketId;
                            activeCustomerName = customerName;
                        }
                    }
                }

                if (historyList.size() > 10) {
                    historyList = historyList.subList(0, 10);
                }

                if (tvCompleted != null) {
                    tvCompleted.setText(String.valueOf(completed));
                }

                if (tvOngoing != null) {
                    tvOngoing.setText(String.valueOf(ongoing));
                }

                float avgRating = ratingCount > 0 ? totalRating / ratingCount : 0.0f;
                if (tvRating != null) {
                    tvRating.setText(String.format(Locale.US, "%.1f", avgRating));
                }

                if (historyAdapter != null) {
                    historyAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TechnicianPerformanceActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getServiceTypeLabel(DataSnapshot ds) {
        if (ds == null) {
            return "General Maintenance";
        }

        StringBuilder sb = new StringBuilder();

        if (hasQty(ds, "qty_cbc") || hasQty(ds, "qty_sediment") || hasQty(ds, "qty_smsf1") || hasQty(ds, "qty_smsf10")) {
            sb.append("Filter Preventive");
        }

        if (hasQty(ds, "qty_wayvalve")) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("Installation Kit");
        }

        if (hasQty(ds, "qty_aquatal") || hasQty(ds, "qty_inline") || hasQty(ds, "qty_uvlamp") ||
                hasQty(ds, "qty_touchpanel") || hasQty(ds, "qty_pbcboard")) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("Other Parts");
        }

        return sb.length() == 0 ? "General Maintenance" : sb.toString();
    }

    private boolean hasQty(DataSnapshot ds, String key) {
        if (ds == null || key == null) {
            return false;
        }

        Object val = ds.child(key).getValue();
        return val instanceof Number && ((Number) val).intValue() > 0;
    }

    private void loadTechnicianCredentials() {
        if (techId == null) {
            return;
        }

        techRef = FirebaseDatabase.getInstance(DB_URL).getReference("Technicians").child(techId);
        techRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String avatarUrl = snapshot.child("profileImageUrl").getValue(String.class);
                if (avatarUrl != null && ivAvatar != null) {
                    Glide.with(TechnicianPerformanceActivity.this).load(avatarUrl).circleCrop().into(ivAvatar);
                } else if (ivAvatar != null) {
                    ivAvatar.setImageResource(R.drawable.new_technician);
                }

                DataSnapshot verification = snapshot.child("verification");
                String verificationStatus = verification.child("status").getValue(String.class);

                View statusIndicator = findViewById(R.id.viewStatusIndicator);

                if ("Verified".equalsIgnoreCase(verificationStatus)) {
                    if (tvStatus != null) {
                        tvStatus.setText("Verified");
                        tvStatus.setTextColor(Color.parseColor("#4CAF50"));
                    }

                    if (statusIndicator != null) {
                        statusIndicator.setBackgroundTintList(
                                ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                    }

                    String idUrl = verification.child("idUrl").getValue(String.class);
                    String certUrl = verification.child("certUrl").getValue(String.class);

                    if (idUrl != null && ivIDPreview != null) {
                        Glide.with(TechnicianPerformanceActivity.this).load(idUrl).into(ivIDPreview);
                    }

                    if (certUrl != null && ivCertPreview != null) {
                        Glide.with(TechnicianPerformanceActivity.this).load(certUrl).into(ivCertPreview);
                    }
                } else {
                    if (tvStatus != null) {
                        tvStatus.setText(verificationStatus != null ? verificationStatus : "Not Verified");
                        tvStatus.setTextColor(Color.parseColor("#FF9800"));
                    }

                    if (statusIndicator != null) {
                        statusIndicator.setBackgroundTintList(
                                ColorStateList.valueOf(Color.parseColor("#FF9800")));
                    }

                    if (ivIDPreview != null) {
                        ivIDPreview.setImageResource(R.drawable.img_place_holder);
                    }

                    if (ivCertPreview != null) {
                        ivCertPreview.setImageResource(R.drawable.img_place_holder);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupButtons() {
        if (btnLiveTrack != null) {
            btnLiveTrack.setOnClickListener(v -> {
                if (hasActiveRequest && activeTicketId != null) {
                    Intent intent = new Intent(TechnicianPerformanceActivity.this, TechnicianTrackActivity.class);
                    intent.putExtra("TICKET_ID", activeTicketId);
                    intent.putExtra("CUSTOMER_NAME", activeCustomerName);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "No active request currently assigned to this technician.", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void generatePerformanceReportPDF() {
        PdfDocument document = new PdfDocument();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        int pageWidth = 595;
        int pageHeight = 842;
        int margin = 40;
        int y = 50;

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        titlePaint.setColor(Color.parseColor("#1E3A8A"));
        titlePaint.setTextSize(24f);
        canvas.drawText("TECHNICIAN PERFORMANCE REPORT", margin, y, titlePaint);
        y += 30;

        paint.setColor(Color.GRAY);
        paint.setTextSize(11f);
        String dateStr = new SimpleDateFormat("MMMM dd, yyyy hh:mm a", Locale.getDefault()).format(new Date());
        canvas.drawText("Generated on: " + dateStr, margin, y, paint);
        y += 30;

        paint.setColor(Color.parseColor("#F3F4F6"));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(margin, y, pageWidth - margin, y + 120, paint);
        paint.setColor(Color.BLACK);
        paint.setTextSize(14f);
        paint.setFakeBoldText(true);

        String techDisplayName = techName != null ? techName.toUpperCase() : "TECHNICIAN";
        canvas.drawText("TECHNICIAN: " + techDisplayName, margin + 15, y + 30, paint);
        paint.setTextSize(12f);
        paint.setFakeBoldText(false);

        String completedStr = tvCompleted != null ? tvCompleted.getText().toString() : "0";
        String ratingStr = tvRating != null ? tvRating.getText().toString() : "0.0";

        canvas.drawText("ID: " + (techId != null ? techId : "N/A"), margin + 15, y + 55, paint);
        canvas.drawText("Jobs Completed: " + completedStr, margin + 15, y + 80, paint);
        canvas.drawText("Avg Rating: " + ratingStr + " Stars", margin + 15, y + 105, paint);
        y += 140;

        paint.setFakeBoldText(true);
        paint.setTextSize(14f);
        canvas.drawText("RECENT SERVICE LOGS", margin, y, paint);
        y += 25;

        paint.setColor(Color.parseColor("#E5E7EB"));
        canvas.drawRect(margin, y, pageWidth - margin, y + 25, paint);
        paint.setColor(Color.BLACK);
        paint.setTextSize(10f);
        paint.setFakeBoldText(true);
        canvas.drawText("TICKET ID", margin + 10, y + 18, paint);
        canvas.drawText("CUSTOMER", margin + 120, y + 18, paint);
        canvas.drawText("SERVICE", margin + 250, y + 18, paint);
        canvas.drawText("DATE", margin + 400, y + 18, paint);
        y += 30;

        paint.setFakeBoldText(false);
        if (historyList != null) {
            int limit = Math.min(historyList.size(), 15);
            for (int i = 0; i < limit; i++) {
                HistoryItem item = historyList.get(i);
                if (item != null) {
                    canvas.drawText(item.ticketId != null ? item.ticketId : "", margin + 10, y, paint);
                    canvas.drawText(truncate(item.customerName, 15), margin + 120, y, paint);
                    canvas.drawText(truncate(item.serviceType, 12), margin + 250, y, paint);
                    canvas.drawText(item.date != null ? item.date : "", margin + 400, y, paint);
                    canvas.drawLine(margin, y + 5, pageWidth - margin, y + 5, paint);
                    y += 20;
                }
            }
        }

        document.finishPage(page);
        savePdfToDownloads(document);
    }

    private String truncate(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        return text.length() > maxLen ? text.substring(0, maxLen - 2) + ".." : text;
    }

    private void savePdfToDownloads(PdfDocument document) {
        if (document == null) {
            return;
        }

        String safeTechName = techName != null ? techName.replace(" ", "_") : "Technician";
        String fileName = "Performance_Report_" + safeTechName + "_" + System.currentTimeMillis() + ".pdf";

        try {
            OutputStream fos;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues cv = new ContentValues();
                cv.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                cv.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                cv.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                fos = uri != null ? getContentResolver().openOutputStream(uri) : null;
            } else {
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
                fos = new FileOutputStream(file);
            }

            if (fos != null) {
                document.writeTo(fos);
                fos.close();
            }

            document.close();
            Toast.makeText(this, "Performance report saved to Downloads!", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private static class HistoryItem {
        String ticketId, customerName, serviceType, date, status;

        HistoryItem(String ticketId, String customerName, String serviceType, String date, String status) {
            this.ticketId = ticketId;
            this.customerName = customerName;
            this.serviceType = serviceType;
            this.date = date;
            this.status = status;
        }
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private final List<HistoryItem> items;

        HistoryAdapter(List<HistoryItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_history_log, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (items == null) {
                return;
            }

            HistoryItem item = items.get(position);
            if (item == null) {
                return;
            }

            if (holder.tvTicketId != null) {
                holder.tvTicketId.setText(item.ticketId != null ? item.ticketId : "");
            }

            if (holder.tvCustomer != null) {
                holder.tvCustomer.setText(item.customerName != null ? item.customerName : "");
            }

            if (holder.tvService != null) {
                holder.tvService.setText(item.serviceType != null ? item.serviceType : "");
            }

            if (holder.tvDate != null) {
                holder.tvDate.setText(item.date != null ? item.date : "");
            }
        }

        @Override
        public int getItemCount() {
            if (items != null) {
                return items.size();
            }
            return 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTicketId, tvCustomer, tvService, tvDate;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTicketId = itemView.findViewById(R.id.historyTicketId);
                tvCustomer = itemView.findViewById(R.id.historyCustomer);
                tvService = itemView.findViewById(R.id.historyService);
                tvDate = itemView.findViewById(R.id.historyDate);
            }
        }
    }
}
