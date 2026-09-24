package com.example.aquatech;

import android.content.ContentValues;
import android.content.Intent;
import android.content.res.ColorStateList;
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
import androidx.core.widget.TextViewCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ServiceReceiptActivity extends AppCompatActivity {

    private ImageView btnBackReceipt;
    private TextView tvTicketId, tvReceiptCustomerName, tvReceiptAddress, 
                     tvReceiptServiceType, tvReceiptSchedule, tvReceiptRequestDate,
                     tvReceiptUnitName, tvReceiptStatus, tvReceiptTotalAmount, 
                     tvReceiptPaymentMethod, tvReceiptBankRef;
    private LinearLayout containerReceiptItems, layoutReceiptBankRef;
    
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
        tvTicketId = findViewById(R.id.tvTicketId);
        tvReceiptCustomerName = findViewById(R.id.tvReceiptCustomerName);
        tvReceiptAddress = findViewById(R.id.tvReceiptAddress);
        tvReceiptServiceType = findViewById(R.id.tvReceiptServiceType);
        tvReceiptSchedule = findViewById(R.id.tvReceiptSchedule);
        tvReceiptRequestDate = findViewById(R.id.tvReceiptRequestDate);
        tvReceiptUnitName = findViewById(R.id.tvReceiptUnitName);
        tvReceiptStatus = findViewById(R.id.tvReceiptStatus);
        tvReceiptTotalAmount = findViewById(R.id.tvReceiptTotalAmount);
        tvReceiptPaymentMethod = findViewById(R.id.tvReceiptPaymentMethod);
        tvReceiptBankRef = findViewById(R.id.tvReceiptBankRef);
        
        containerReceiptItems = findViewById(R.id.containerReceiptItems);
        layoutReceiptBankRef = findViewById(R.id.layoutReceiptBankRef);
    }

    private void setupClickListeners() {
        if (btnBackReceipt != null) {
            btnBackReceipt.setOnClickListener(v -> returnToDashboard());
        }
    }

    private void saveReceiptAsPdf() {
        if (currentSnapshot == null) {
            Toast.makeText(this, "Please wait for receipt to load fully", Toast.LENGTH_SHORT).show();
            return;
        }

        View pdfView = LayoutInflater.from(this).inflate(R.layout.layout_receipt_pdf_template, null);
        
        String dateStr = tvReceiptRequestDate.getText().toString();
        String timeStr = "09:15 AM";

        Long ts = currentSnapshot.child("timestamp").getValue(Long.class);
        if (ts != null) {
            Date submissionDate = new Date(ts);
            dateStr = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(submissionDate);
            timeStr = new SimpleDateFormat("hh:mm A", Locale.getDefault()).format(submissionDate);
        } else {
            timeStr = new SimpleDateFormat("hh:mm A", Locale.getDefault()).format(new Date());
        }
        
        ((TextView)pdfView.findViewById(R.id.pdfSubmittedDate)).setText(dateStr);
        ((TextView)pdfView.findViewById(R.id.pdfSubmittedTime)).setText(timeStr);
        String startTimeVal = currentSnapshot.child("startTime").getValue(String.class);
        String schedDate = currentSnapshot.child("date").getValue(String.class);
        
        if (startTimeVal != null && !startTimeVal.equalsIgnoreCase("To be confirmed")) {
            String timeOnly = startTimeVal.toUpperCase();
            String sessionTag = timeOnly.contains("AM") ? "MORNING" : "AFTERNOON";
            
            String finalSchedText = (schedDate != null ? schedDate : "TBC") + " • " + timeOnly + " (" + sessionTag + ")";
            ((TextView)pdfView.findViewById(R.id.pdfScheduleSummary)).setText(finalSchedText);
        } else {
            String waitText = (schedDate != null ? schedDate : "Pending") + " • TBC (PENDING CONFIRMATION)";
            ((TextView)pdfView.findViewById(R.id.pdfScheduleSummary)).setText(waitText);
        }
        
        TextView pdfStatusBadge = pdfView.findViewById(R.id.pdfStatusBadge);
        String currentStatus = tvReceiptStatus.getText().toString().toUpperCase();

        if (currentStatus.equalsIgnoreCase("OPEN")) {
            pdfStatusBadge.setText("PENDING");
            pdfStatusBadge.setBackgroundResource(R.drawable.bg_pdf_status_pending_pill);
            pdfStatusBadge.setTextColor(Color.parseColor("#D97706"));
            TextViewCompat.setCompoundDrawableTintList(pdfStatusBadge, ColorStateList.valueOf(Color.parseColor("#D97706")));
        } else if (currentStatus.contains("COMPLETED")) {
            pdfStatusBadge.setText("COMPLETED");
            pdfStatusBadge.setBackgroundResource(R.drawable.bg_pdf_status_green_pill);
            pdfStatusBadge.setTextColor(Color.parseColor("#166534"));
            TextViewCompat.setCompoundDrawableTintList(pdfStatusBadge, ColorStateList.valueOf(Color.parseColor("#166534")));
        } else {
            pdfStatusBadge.setText(currentStatus);
            pdfStatusBadge.setBackgroundResource(R.drawable.bg_pdf_status_blue_pill);
            pdfStatusBadge.setTextColor(Color.parseColor("#2563EB"));
            TextViewCompat.setCompoundDrawableTintList(pdfStatusBadge, ColorStateList.valueOf(Color.parseColor("#2563EB")));
        }

        String sroAtTop = currentSnapshot.child("referenceNo").getValue(String.class);
        if (sroAtTop == null) sroAtTop = currentSnapshot.child("sroNumber").getValue(String.class);
        ((TextView)pdfView.findViewById(R.id.pdfTicketId)).setText(sroAtTop != null ? sroAtTop : "SRO-00000000");

        ((TextView)pdfView.findViewById(R.id.pdfCustomerName)).setText(tvReceiptCustomerName.getText());
        String phone = currentSnapshot.child("customerPhone").getValue(String.class);
        if (phone == null) phone = currentSnapshot.child("userPhone").getValue(String.class);
        ((TextView)pdfView.findViewById(R.id.pdfContact)).setText(phone != null ? phone : "Not Provided");
        ((TextView)pdfView.findViewById(R.id.pdfAddress)).setText(tvReceiptAddress.getText());

        String unitModel = currentSnapshot.child("unitName").getValue(String.class);
        if (unitModel == null) unitModel = currentSnapshot.child("unitModel").getValue(String.class);
        if (unitModel == null) unitModel = "";
        
        String unitNumber = currentSnapshot.child("unitNumber").getValue(String.class);
        if (unitNumber == null) unitNumber = currentSnapshot.child("itemUnitNumber").getValue(String.class);
        if (unitNumber == null) unitNumber = "0000";

        String friendlyProductName = "STANDING WATER PURIFIER";
        String baseUnitTemplate = "ST-FXCU1-M-HCA-WT-**-***";

        if (unitModel.toUpperCase().contains("CUBE")) {
            friendlyProductName = "WL CUBE FIREWALL";
            baseUnitTemplate = "F-FXCU1-M-HCA-TT-K1-**-***";
        } else if (unitModel.toUpperCase().contains("SLIM")) {
            friendlyProductName = "SMART SLIM";
            baseUnitTemplate = "S-FXCU1-M-HCA-AA-B2-**-***";
        } else if (unitModel.toUpperCase().contains("COUNTER")) {
            friendlyProductName = "COUNTER TOP WATER PURIFIER";
            baseUnitTemplate = "CT-FXCU1-M-HCA-WT-**-***";
        } else if (unitModel.toUpperCase().contains("STANDING") || unitModel.toUpperCase().contains("ST")) {
            friendlyProductName = "STANDING WATER PURIFIER";
            baseUnitTemplate = "ST-FXCU1-M-HCA-WT-**-***";
        }

        String finalUnitCode = baseUnitTemplate.replace("**-***", unitNumber);

        ((TextView)pdfView.findViewById(R.id.pdfUnit)).setText(friendlyProductName);
        ((TextView)pdfView.findViewById(R.id.pdfUnitCode)).setText(finalUnitCode);
        
        String pt = currentSnapshot.child("purchaseType").getValue(String.class);
        ((TextView)pdfView.findViewById(R.id.pdfLocation)).setText(pt != null ? pt : "SUBSCRIPTION");
        String loc = currentSnapshot.child("installationLocation").getValue(String.class);
        ((TextView)pdfView.findViewById(R.id.pdfLocation)).setText(loc != null ? loc : "Kitchen Area");

        String rawInput = currentSnapshot.child("remarks").getValue(String.class);
        if (rawInput == null) rawInput = "";
        String p = rawInput.toLowerCase();

        String serviceCategory = "General Service";
        String serviceDesc = "Technical system inspection and evaluation.";
        String concernSummary = "General System Check";

        if (matchKeywords(p, "leak", "tulo", "baha", "basâ", "dripping", "leakage", "broken", "damage")) {
            serviceCategory = "Technical Repair";
            serviceDesc = "Onsite technical repair and parts verification.";
            concernSummary = "System Leakage & Repair";
        } else if (matchKeywords(p, "power", "board", "bukas", "patay", "electricity", "fuse", "dead", "error", "faulty")) {
            serviceCategory = "Technical Repair";
            serviceDesc = "Electrical system troubleshooting and repair.";
            concernSummary = "Electrical & Power Issue";
        } else if (matchKeywords(p, "mahina", "slow", "barado", "low pressure", "clogged", "no water")) {
            serviceCategory = "Technical Repair";
            serviceDesc = "Flow optimization and system restoration.";
            concernSummary = "Flow Optimization & Restoration";
        } else if (matchKeywords(p, "amoy", "mabaho", "malabo", "lasa", "filter", "odor", "smell", "stink", "dirty", "taste", "maintenance", "cleaning", "check")) {
            serviceCategory = "System Maintenance";
            serviceDesc = "Scheduled preventive maintenance and health check.";
            concernSummary = "Water Quality & Filter Care";
        } else if (matchKeywords(p, "install", "lipat", "kabit", "setup", "move", "relocate")) {
            serviceCategory = "Unit Installation";
            serviceDesc = "Professional unit setup and system calibration.";
            concernSummary = "Professional Unit Setup";
        }

        ((TextView)pdfView.findViewById(R.id.pdfServiceType)).setText(serviceCategory);
        ((TextView)pdfView.findViewById(R.id.pdfDescription)).setText(serviceDesc);
        ((TextView)pdfView.findViewById(R.id.pdfConcern)).setText(concernSummary);
        ((TextView)pdfView.findViewById(R.id.pdfRemarks)).setText(rawInput.isEmpty() ? "Standard maintenance request." : rawInput);

        String pm = tvReceiptPaymentMethod.getText().toString();
        ((TextView)pdfView.findViewById(R.id.pdfPaymentMethod)).setText(pm);
        ((TextView)pdfView.findViewById(R.id.pdfTotalAmount)).setText(tvReceiptTotalAmount.getText());

        TextView pdfPaymentStatus = pdfView.findViewById(R.id.pdfPaymentStatus);
        View layoutPdfReference = pdfView.findViewById(R.id.layoutPdfReference);
        TextView pdfReferenceNo = pdfView.findViewById(R.id.pdfReferenceNo);

        String refNo = currentSnapshot.child("paymentReference").getValue(String.class);
        String statusStr = currentSnapshot.child("paymentStatus").getValue(String.class);
        boolean isPaid = "PAID".equalsIgnoreCase(statusStr);

        if (pm.equalsIgnoreCase("GCash") || pm.equalsIgnoreCase("Maya") || pm.equalsIgnoreCase("Bank Transfer")) {
            if (isPaid) {
                pdfPaymentStatus.setText("PAID");
                pdfPaymentStatus.setBackgroundResource(R.drawable.bg_pdf_status_green_pill);
                pdfPaymentStatus.setTextColor(Color.parseColor("#166534"));
                TextViewCompat.setCompoundDrawableTintList(pdfPaymentStatus, ColorStateList.valueOf(Color.parseColor("#166534")));
            } else {
                pdfPaymentStatus.setText("FOR VERIFICATION");
                pdfPaymentStatus.setBackgroundResource(R.drawable.bg_pdf_status_blue_pill);
                pdfPaymentStatus.setTextColor(Color.parseColor("#2563EB"));
                TextViewCompat.setCompoundDrawableTintList(pdfPaymentStatus, ColorStateList.valueOf(Color.parseColor("#2563EB")));
            }
            
            if (refNo != null && !refNo.isEmpty()) {
                layoutPdfReference.setVisibility(View.VISIBLE);
                pdfReferenceNo.setText(refNo);
            }
        } else if (pm.equalsIgnoreCase("COD")) {
            if (isPaid) {
                pdfPaymentStatus.setText("PAID");
                pdfPaymentStatus.setBackgroundResource(R.drawable.bg_pdf_status_green_pill);
                pdfPaymentStatus.setTextColor(Color.parseColor("#166534"));
                TextViewCompat.setCompoundDrawableTintList(pdfPaymentStatus, ColorStateList.valueOf(Color.parseColor("#166534")));
            } else {
                pdfPaymentStatus.setText("PAYMENT DUE");
                pdfPaymentStatus.setBackgroundResource(R.drawable.bg_pdf_status_pending_pill);
                pdfPaymentStatus.setTextColor(Color.parseColor("#D97706"));
                TextViewCompat.setCompoundDrawableTintList(pdfPaymentStatus, ColorStateList.valueOf(Color.parseColor("#D97706")));
            }
            layoutPdfReference.setVisibility(View.GONE);
        }

        String techNameText = currentSnapshot.child("assignedTechName").getValue(String.class);
        if (techNameText == null) techNameText = "Aqua Technician";
        TextView pdfTechNameView = pdfView.findViewById(R.id.pdfTechName);
        if (pdfTechNameView != null) pdfTechNameView.setText(techNameText);

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

        try {
            int width = 1200;
            pdfView.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), 
                          View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            pdfView.layout(0, 0, pdfView.getMeasuredWidth(), pdfView.getMeasuredHeight());

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
                File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File folder = new File(downloadDir, "AquaTech");
                if (!folder.exists()) folder.mkdirs();
                File file = new File(folder, fileName);
                fos = new FileOutputStream(file);
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
            ((TextView)row.findViewById(R.id.pdfItemName)).setText(name);
            ((TextView)row.findViewById(R.id.pdfItemQty)).setText(String.valueOf(q));
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

                    String serviceType = snapshot.child("serviceType").getValue(String.class);
                    if (serviceType == null) serviceType = snapshot.child("purchaseType").getValue(String.class);
                    if (serviceType != null) {
                        tvReceiptServiceType.setText(serviceType);
                    }

                    String pm = snapshot.child("paymentMethod").getValue(String.class);
                    if (pm != null) tvReceiptPaymentMethod.setText(pm);
                    
                    String bankRef = snapshot.child("bankReference").getValue(String.class);
                    if (bankRef != null && !bankRef.isEmpty()) {
                        layoutReceiptBankRef.setVisibility(View.VISIBLE);
                        tvReceiptBankRef.setText(bankRef);
                    } else {
                        layoutReceiptBankRef.setVisibility(View.GONE);
                    }

                    String status = snapshot.child("status").getValue(String.class);
                    if (status == null) status = "OPEN";
                    updateStatusUI(status);
                    String techId = snapshot.child("assignedTechId").getValue(String.class);
                    if (techId == null || techId.isEmpty()) techId = snapshot.child("technicianId").getValue(String.class);

                    containerReceiptItems.removeAllViews();
                    addOrder(snapshot, "qty_wayvalve", "Installation Kit (3-Way Valve)", 350);
                    addOrder(snapshot, "qty_cbc", "Filter 0064-CBC", 2000);
                    addOrder(snapshot, "qty_sediment", "Filter 0055-SEDIMENT", 1000);
                    addOrder(snapshot, "qty_aquatal", "Aquatal Replacement", 2000);
                    addOrder(snapshot, "qty_inline", "Inline Filter", 3000);
                    addOrder(snapshot, "qty_uvlamp", "UV Lamp", 1500);
                    addOrder(snapshot, "qty_touchpanel", "Touch Panel", 750);
                    addOrder(snapshot, "qty_pbcboard", "PBC Board", 3000);
                    addOrder(snapshot, "qty_smsf1", "SMSF 1u CBC", 2000);
                    addOrder(snapshot, "qty_smsf10", "SMSF 1u SED", 1000);

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

    private boolean matchKeywords(String input, String... keywords) {
        for (String k : keywords) {
            if (input.contains(k)) return true;
        }
        return false;
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
