package com.example.aquatech;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.OutputStream;

public class ServiceReceiptActivity extends AppCompatActivity {

    private ImageView btnBackReceipt, ivReceiptTechSignature, btnDownloadReceipt;
    private TextView tvTicketId, tvReceiptCustomerName, tvReceiptAddress, 
                     tvReceiptServiceType, tvReceiptSchedule, tvReceiptRequestDate,
                     tvReceiptUnitName, tvReceiptTechName, tvReceiptTechRole, 
                     tvReceiptStatus, tvReceiptTotalAmount;
    private CardView receiptCard;
    private NestedScrollView receiptScrollView;
    private LinearLayout containerReceiptItems, technicianInfoLayout;
    
    private DataSnapshot currentSnapshot;
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_receipt);

        setupStatusBar();
        initializeViews();
        setupClickListeners();
        loadReceiptDataFromFirebase();
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initializeViews() {
        btnBackReceipt = findViewById(R.id.btnBackReceipt);
        btnDownloadReceipt = findViewById(R.id.btnDownloadReceipt);
        tvTicketId = findViewById(R.id.tvTicketId);
        tvReceiptCustomerName = findViewById(R.id.tvReceiptCustomerName);
        tvReceiptAddress = findViewById(R.id.tvReceiptAddress);
        tvReceiptServiceType = findViewById(R.id.tvReceiptServiceType);
        tvReceiptSchedule = findViewById(R.id.tvReceiptSchedule);
        tvReceiptRequestDate = findViewById(R.id.tvReceiptRequestDate);
        tvReceiptUnitName = findViewById(R.id.tvReceiptUnitName);
        tvReceiptTechName = findViewById(R.id.tvReceiptTechName);
        tvReceiptTechRole = findViewById(R.id.tvReceiptTechRole);
        tvReceiptStatus = findViewById(R.id.tvReceiptStatus);
        tvReceiptTotalAmount = findViewById(R.id.tvReceiptTotalAmount);
        ivReceiptTechSignature = findViewById(R.id.ivReceiptTechSignature);
        receiptCard = findViewById(R.id.receiptCard);
        receiptScrollView = findViewById(R.id.receiptScrollView);
        containerReceiptItems = findViewById(R.id.containerReceiptItems);
        technicianInfoLayout = findViewById(R.id.technicianInfoLayout);
    }

    private void setupClickListeners() {
        btnBackReceipt.setOnClickListener(v -> returnToDashboard());
        btnDownloadReceipt.setOnClickListener(v -> saveReceiptAsPdf());
    }

    private void saveReceiptAsPdf() {
        if (currentSnapshot == null) {
            Toast.makeText(this, "Please wait for receipt to load fully", Toast.LENGTH_SHORT).show();
            return;
        }

        View pdfView = LayoutInflater.from(this).inflate(R.layout.layout_receipt_pdf_template, null);
        
        ((TextView)pdfView.findViewById(R.id.pdfCustomerName)).setText(tvReceiptCustomerName.getText());
        ((TextView)pdfView.findViewById(R.id.pdfAddress)).setText(tvReceiptAddress.getText());
        ((TextView)pdfView.findViewById(R.id.pdfTicketId)).setText(tvTicketId.getText());
        ((TextView)pdfView.findViewById(R.id.pdfServiceType)).setText(tvReceiptServiceType.getText());
        ((TextView)pdfView.findViewById(R.id.pdfSchedule)).setText(tvReceiptSchedule.getText());
        ((TextView)pdfView.findViewById(R.id.pdfDate)).setText(tvReceiptRequestDate.getText());
        ((TextView)pdfView.findViewById(R.id.pdfUnit)).setText(tvReceiptUnitName.getText());
        ((TextView)pdfView.findViewById(R.id.pdfTotalAmount)).setText(tvReceiptTotalAmount.getText());
        ((TextView)pdfView.findViewById(R.id.pdfStatus)).setText(tvReceiptStatus.getText());

        LinearLayout pdfOrdersContainer = pdfView.findViewById(R.id.pdfOrdersContainer);
        addPdfOrder(currentSnapshot, pdfOrdersContainer, "qty_wayvalve", "Installation Kit (3-Way Valve)", 350);
        addPdfOrder(currentSnapshot, pdfOrdersContainer, "qty_cbc", "Filter 0064-CBC", 2000);
        addPdfOrder(currentSnapshot, pdfOrdersContainer, "qty_sediment", "Filter 0055-SEDIMENT", 1000);
        addPdfOrder(currentSnapshot, pdfOrdersContainer, "qty_aquatal", "Aquatal Replacement", 2000);
        addPdfOrder(currentSnapshot, pdfOrdersContainer, "qty_inline", "Inline Filter", 3000);
        addPdfOrder(currentSnapshot, pdfOrdersContainer, "qty_uvlamp", "UV Lamp", 1500);
        addPdfOrder(currentSnapshot, pdfOrdersContainer, "qty_touchpanel", "Touch Panel", 750);
        addPdfOrder(currentSnapshot, pdfOrdersContainer, "qty_pbcboard", "PBC Board", 3000);
        addPdfOrder(currentSnapshot, pdfOrdersContainer, "qty_smsf1", "SMSF 1µ CBC", 2000);
        addPdfOrder(currentSnapshot, pdfOrdersContainer, "qty_smsf10", "SMSF 10µ SED", 1000);

        pdfView.measure(View.MeasureSpec.makeMeasureSpec(450, View.MeasureSpec.EXACTLY), 
                       View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        pdfView.layout(0, 0, pdfView.getMeasuredWidth(), pdfView.getMeasuredHeight());

        try {
            Bitmap bitmap = Bitmap.createBitmap(pdfView.getMeasuredWidth(), pdfView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.WHITE);
            pdfView.draw(canvas);

            PdfDocument document = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            page.getCanvas().drawBitmap(bitmap, 0, 0, new Paint());
            document.finishPage(page);

            String tIdText = tvTicketId.getText().toString().replace("#", "").trim();
            String fileName = "AquaTech_Official_Receipt_" + tIdText + ".pdf";

            OutputStream fos;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AquaTech");
                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                fos = getContentResolver().openOutputStream(uri);
            } else {
                java.io.File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                java.io.File folder = new java.io.File(downloadDir, "AquaTech");
                if (!folder.exists()) folder.mkdirs();
                java.io.File file = new java.io.File(folder, fileName);
                fos = new java.io.FileOutputStream(file);
            }

            document.writeTo(fos);
            document.close();
            if (fos != null) fos.close();

            Toast.makeText(this, "Professional Receipt saved to Downloads/AquaTech", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this, "Error generating PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void addPdfOrder(DataSnapshot snapshot, LinearLayout container, String key, String name, int price) {
        Object val = snapshot.child(key).getValue();
        int q = 0;
        if (val instanceof Number) q = ((Number) val).intValue();
        
        if (q > 0) {
            View row = getLayoutInflater().inflate(R.layout.item_pdf_billing_row, container, false);
            ((TextView)row.findViewById(R.id.pdfItemName)).setText(name + " x" + q);
            ((TextView)row.findViewById(R.id.pdfItemPrice)).setText("₱ " + String.format("%,d", q * price) + ".00");
            container.addView(row);
        }
    }

    private void loadReceiptDataFromFirebase() {
        String ticketId = getIntent().getStringExtra("TICKET_ID");
        if (ticketId == null) return;
        tvTicketId.setText("#" + ticketId);

        DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL).getReference("ServiceRequests").child(ticketId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentSnapshot = snapshot; 
                    tvReceiptCustomerName.setText(snapshot.child("customerName").getValue(String.class));
                    tvReceiptAddress.setText(snapshot.child("address").getValue(String.class));
                    tvReceiptRequestDate.setText(snapshot.child("date").getValue(String.class));
                    
                    String unitModel = snapshot.child("unitName").getValue(String.class);
                    if (unitModel == null) unitModel = snapshot.child("unitModel").getValue(String.class);
                    if (unitModel != null) {
                        String unitCode = unitModel.toUpperCase().contains("CUBE") ? "F-FXCU1-M-HCA-TT" :
                                         unitModel.toUpperCase().contains("SLIM") ? "S-FXCU1-M-HCA-AA" :
                                         unitModel.toUpperCase().contains("COUNTER") ? "CT-FXCU1-M-HCA-WT" : "ST-FXCU1-M-HCA-WT";
                        String suffix = ticketId.length() >= 3 ? ticketId.substring(ticketId.length() - 3) : "000";
                        tvReceiptUnitName.setText(unitCode + "-T1-" + suffix);
                    }

                    String startTime = snapshot.child("startTime").getValue(String.class);
                    String endTime = snapshot.child("endTime").getValue(String.class);
                    if (startTime != null && endTime != null) {
                        tvReceiptSchedule.setText(startTime.toUpperCase() + " TO " + endTime.toUpperCase());
                    }

                    String serviceType = snapshot.child("purchaseType").getValue(String.class);
                    if (serviceType != null) {
                        tvReceiptServiceType.setText(serviceType);
                    }

                    String status = snapshot.child("status").getValue(String.class);
                    if (status == null) status = "OPEN";
                    updateStatusUI(status);

                    // 🛠️ FIX: Show Tech Info if a technician is assigned, regardless of specific status string
                    String techId = snapshot.child("assignedTechId").getValue(String.class);
                    if (techId == null || techId.isEmpty()) techId = snapshot.child("technicianId").getValue(String.class);

                    if (techId != null && !techId.isEmpty()) {
                        if (technicianInfoLayout != null) technicianInfoLayout.setVisibility(View.VISIBLE);
                        
                        String techName = snapshot.child("assignedTechName").getValue(String.class);
                        if (techName == null || techName.isEmpty()) techName = snapshot.child("technicianName").getValue(String.class);
                        tvReceiptTechName.setText(techName != null ? techName : "Aqua Technician");
                        
                        // 🛠️ FIX: Correctly load signature resource from string name
                        String sigName = snapshot.child("technicianSignature").getValue(String.class);
                        if (sigName != null && !sigName.isEmpty()) {
                            int resId = getResources().getIdentifier(sigName, "drawable", getPackageName());
                            if (resId != 0) {
                                ivReceiptTechSignature.setImageResource(resId);
                            } else {
                                ivReceiptTechSignature.setImageResource(R.drawable.signature1_png);
                            }
                        } else {
                            ivReceiptTechSignature.setImageResource(R.drawable.signature1_png);
                        }
                    } else {
                        if (technicianInfoLayout != null) technicianInfoLayout.setVisibility(View.GONE);
                    }

                    containerReceiptItems.removeAllViews();
                    addOrder(snapshot, "qty_wayvalve", "Installation Kit (3-Way Valve)", 350);
                    addOrder(snapshot, "qty_cbc", "Filter 0064-CBC", 2000);
                    addOrder(snapshot, "qty_sediment", "Filter 0055-SEDIMENT", 1000);
                    addOrder(snapshot, "qty_aquatal", "Aquatal Replacement", 2000);
                    addOrder(snapshot, "qty_inline", "Inline Filter", 3000);
                    addOrder(snapshot, "qty_uvlamp", "UV Lamp", 1500);
                    addOrder(snapshot, "qty_touchpanel", "Touch Panel", 750);
                    addOrder(snapshot, "qty_pbcboard", "PBC Board", 3000);
                    addOrder(snapshot, "qty_smsf1", "SMSF 1µ CBC", 2000);
                    addOrder(snapshot, "qty_smsf10", "SMSF 10µ SED", 1000);

                    Double total = snapshot.child("totalAmount").getValue(Double.class);
                    tvReceiptTotalAmount.setText("₱ " + String.format("%,.2f", total != null ? total : 0.0));
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateStatusUI(String status) {
        tvReceiptStatus.setText(status.toUpperCase());
        if (status.equalsIgnoreCase("COMPLETED") || status.equalsIgnoreCase("APPROVED") || status.equalsIgnoreCase("ACCEPTED") || status.equalsIgnoreCase("In Progress") || status.equalsIgnoreCase("Submission")) {
            tvReceiptStatus.setBackground(ContextCompat.getDrawable(this, R.drawable.status_completed_green));
        } else if (status.equalsIgnoreCase("CANCELLED")) {
            tvReceiptStatus.setBackground(ContextCompat.getDrawable(this, R.drawable.status_cancelled_red));
        } else {
            tvReceiptStatus.setBackground(ContextCompat.getDrawable(this, R.drawable.status_open_blue));
        }
    }

    private void addOrder(DataSnapshot snapshot, String key, String name, int price) {
        Object val = snapshot.child(key).getValue();
        int q = 0;
        if (val instanceof Number) q = ((Number) val).intValue();

        if (q > 0) {
            View row = getLayoutInflater().inflate(R.layout.item_billing_row, containerReceiptItems, false);
            ((TextView)row.findViewById(R.id.tvItemName)).setText(name + " x" + q);
            ((TextView)row.findViewById(R.id.tvItemPrice)).setText("₱ " + String.format("%,d", q * price) + ".00");
            containerReceiptItems.addView(row);
        }
    }

    @Override
    public void onBackPressed() {
        returnToDashboard();
    }

    private void returnToDashboard() {
        Intent intent = new Intent(this, CustomerDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
