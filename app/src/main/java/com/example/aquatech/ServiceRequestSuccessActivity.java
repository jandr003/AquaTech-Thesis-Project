package com.example.aquatech;

import android.app.Dialog;
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
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.OutputStream;

public class ServiceRequestSuccessActivity extends AppCompatActivity {

    private CardView serviceCard, cardValidID;
    private TextView timeDateReq, displayStatus;
    private ImageView btnDownload, ivValidID;

    private TextView displayTicketID, displayCustomerName, displayContactNumber,
            displayAddress, displayRefNo, displayUnit,
            displayValidIDStatus, displayRemarks, displayTotalAmount;

    private LinearLayout ordersListContainer;
    private Button btnReturnDashboard;
    private ConstraintLayout rootLayout;

    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private DataSnapshot currentSnapshot;
    private DatabaseReference statusRef;
    private ValueEventListener statusListener;

    private boolean isGenerating = false;
    private String currentIdUrl = "";
    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_service_request_success);
            currentUid = FirebaseAuth.getInstance().getUid();

            setupStatusBar();
            initializeViews();
            loadIntentData();
            setupRealtimeData();
            startAnimations();
            setupClickListeners();
        } catch (Exception e) {
            Log.e("SUCCESS_CRASH", "Error in onCreate: " + e.getMessage());
        }
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initializeViews() {
        rootLayout = findViewById(R.id.rootConstraintLayout);
        serviceCard = findViewById(R.id.serviceCard);
        timeDateReq = findViewById(R.id.timeDateReq);
        btnDownload = findViewById(R.id.btnDownload);

        displayTicketID = findViewById(R.id.displayTicketID);
        displayCustomerName = findViewById(R.id.displayCustomerName);
        displayContactNumber = findViewById(R.id.displayContactNumber);
        displayAddress = findViewById(R.id.displayAddress);
        displayRefNo = findViewById(R.id.displayRefNo);
        displayUnit = findViewById(R.id.displayUnit);
        displayValidIDStatus = findViewById(R.id.displayValidIDStatus);
        displayRemarks = findViewById(R.id.displayRemarks);
        displayTotalAmount = findViewById(R.id.displayTotalAmount);
        displayStatus = findViewById(R.id.displayStatus);

        ivValidID = findViewById(R.id.ivValidID);
        cardValidID = findViewById(R.id.cardValidID);

        ordersListContainer = findViewById(R.id.ordersListContainer);
        btnReturnDashboard = findViewById(R.id.ReturnDashBoard);
    }

    private void loadIntentData() {
        Intent intent = getIntent();
        if (intent == null) return;

        String ticketId = intent.getStringExtra("TICKET_ID");
        if (ticketId != null && displayTicketID != null) displayTicketID.setText("#" + ticketId);

        if (displayCustomerName != null) displayCustomerName.setText(intent.getStringExtra("CUSTOMER_NAME"));
        if (displayContactNumber != null) displayContactNumber.setText(intent.getStringExtra("CONTACT_NUMBER"));
        if (displayAddress != null) displayAddress.setText(intent.getStringExtra("ADDRESS"));
        if (displayRefNo != null) displayRefNo.setText(intent.getStringExtra("REF_NO"));

        String remarks = intent.getStringExtra("REMARKS");
        if (displayRemarks != null) displayRemarks.setText((remarks == null || remarks.trim().isEmpty()) ? "N/A" : remarks);

        double total = intent.getDoubleExtra("TOTAL_AMOUNT", 0.0);
        if (displayTotalAmount != null) displayTotalAmount.setText(String.format("₱%,.2f", total));

        String date = intent.getStringExtra("DATE");
        if (date != null && timeDateReq != null) timeDateReq.setText(date.toUpperCase());

        // Load Order Summary from Intent
        if (ordersListContainer != null) {
            ordersListContainer.removeAllViews();
            addOrderFromIntent("qty_wayvalve", "Installation Kit (3-Way Valve)");
            addOrderFromIntent("qty_cbc", "Filter 0064-CBC");
            addOrderFromIntent("qty_sediment", "Filter 0055-SEDIMENT");
            addOrderFromIntent("qty_aquatal", "Aquatal Replacement");
            addOrderFromIntent("qty_inline", "Inline Filter");
            addOrderFromIntent("qty_uvlamp", "UV Lamp");
            addOrderFromIntent("qty_touchpanel", "Touch Panel");
            addOrderFromIntent("qty_pbcboard", "PBC Board");
            addOrderFromIntent("qty_smsf1", "SMSF 1µ CBC");
            addOrderFromIntent("qty_smsf10", "SMSF 10µ SED");
        }

        String fbUrl = intent.getStringExtra("VALID_ID_URL");
        String localUri = intent.getStringExtra("SELECTED_ID_URI");

        if (isValidUrl(fbUrl)) {
            currentIdUrl = fbUrl;
        } else if (isValidUrl(localUri)) {
            currentIdUrl = localUri;
        }
        updateIdStatusUI(currentIdUrl);
    }

    private void addOrderFromIntent(String key, String label) {
        int qty = getIntent().getIntExtra(key, 0);
        if (qty > 0 && ordersListContainer != null) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_order_summary, ordersListContainer, false);
            TextView name = itemView.findViewById(R.id.orderItemName);
            TextView q = itemView.findViewById(R.id.orderItemQty);
            if (name != null) name.setText(label);
            if (q != null) q.setText("x" + qty);
            ordersListContainer.addView(itemView);
        }
    }

    private void setupRealtimeData() {
        String ticketId = getIntent().getStringExtra("TICKET_ID");
        if (ticketId == null) return;

        statusRef = FirebaseDatabase.getInstance(DB_URL).getReference("ServiceRequests").child(ticketId);
        statusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentSnapshot = snapshot;
                    updateUIWithLatestData(snapshot);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        statusRef.addValueEventListener(statusListener);
    }

    private void updateUIWithLatestData(DataSnapshot snapshot) {
        if (displayCustomerName != null) displayCustomerName.setText(snapshot.child("customerName").getValue(String.class));
        if (displayAddress != null) displayAddress.setText(snapshot.child("address").getValue(String.class));

        String unitName = snapshot.child("unitName").getValue(String.class);
        if (unitName == null) unitName = snapshot.child("unitModel").getValue(String.class);

        if (unitName != null && displayUnit != null) {
            String unitCode = unitName.toUpperCase().contains("CUBE") ? "F-FXCU1-M-HCA-TT" :
                             unitName.toUpperCase().contains("SLIM") ? "S-FXCU1-M-HCA-AA" :
                             unitName.toUpperCase().contains("COUNTER") ? "CT-FXCU1-M-HCA-WT" : "ST-FXCU1-M-HCA-WT";
            String tId = snapshot.child("ticketId").getValue(String.class);
            String suffix = (tId != null && tId.length() >= 3) ? tId.substring(tId.length() - 3) : "000";
            displayUnit.setText(unitCode + "-T1-" + suffix);
        }

        String remoteIdUrl = snapshot.child("validIdUrl").getValue(String.class);
        currentIdUrl = isValidUrl(remoteIdUrl) ? remoteIdUrl : currentIdUrl;
        updateIdStatusUI(currentIdUrl);

        String remarksFromDb = snapshot.child("remarks").getValue(String.class);
        if (displayRemarks != null) displayRemarks.setText((remarksFromDb == null || remarksFromDb.trim().isEmpty()) ? "N/A" : remarksFromDb);

        String status = snapshot.child("status").getValue(String.class);
        if (status != null && displayStatus != null) displayStatus.setText(status.toUpperCase());

        Object totalObj = snapshot.child("totalAmount").getValue();
        if (totalObj instanceof Number && displayTotalAmount != null) {
            displayTotalAmount.setText(String.format("₱%,.2f", ((Number) totalObj).doubleValue()));
        }

        if (ordersListContainer != null) {
            ordersListContainer.removeAllViews();
            addOrderItem(snapshot, "qty_wayvalve", "Installation Kit (3-Way Valve)");
            addOrderItem(snapshot, "qty_cbc", "Filter 0064-CBC");
            addOrderItem(snapshot, "qty_sediment", "Filter 0055-SEDIMENT");
            addOrderItem(snapshot, "qty_aquatal", "Aquatal Replacement");
            addOrderItem(snapshot, "qty_inline", "Inline Filter");
            addOrderItem(snapshot, "qty_uvlamp", "UV Lamp");
            addOrderItem(snapshot, "qty_touchpanel", "Touch Panel");
            addOrderItem(snapshot, "qty_pbcboard", "PBC Board");
            addOrderItem(snapshot, "qty_smsf1", "SMSF 1µ CBC");
            addOrderItem(snapshot, "qty_smsf10", "SMSF 10µ SED");
        }

        String date = snapshot.child("date").getValue(String.class);
        if (date != null && timeDateReq != null) timeDateReq.setText(date.toUpperCase());
    }

    private boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) return false;
        String u = url.trim().toLowerCase();
        return !u.equals("none provided") && !u.equals("upload failed") && (u.startsWith("http") || u.startsWith("content:"));
    }

    private void updateIdStatusUI(String url) {
        boolean valid = isValidUrl(url);
        if (displayValidIDStatus != null) {
            displayValidIDStatus.setText(valid ? "ATTACHED" : "NOT PROVIDED");
            displayValidIDStatus.setTextColor(valid ? Color.parseColor("#3EB5E8") : Color.parseColor("#FF5252"));
        }

        if (cardValidID != null) cardValidID.setVisibility(valid ? View.VISIBLE : View.GONE);
        if (ivValidID != null) {
            if (valid) {
                ivValidID.setVisibility(View.VISIBLE);
                Glide.with(this)
                        .load(url)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .fitCenter()
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_report_image)
                        .into(ivValidID);
            } else {
                ivValidID.setVisibility(View.GONE);
            }
        }
    }

    private void addOrderItem(DataSnapshot snapshot, String key, String label) {
        Object qtyObj = snapshot.child(key).getValue();
        if (qtyObj instanceof Number) {
            long qty = ((Number) qtyObj).longValue();
            if (qty > 0 && ordersListContainer != null) {
                View itemView = LayoutInflater.from(this).inflate(R.layout.item_order_summary, ordersListContainer, false);
                TextView name = itemView.findViewById(R.id.orderItemName);
                TextView q = itemView.findViewById(R.id.orderItemQty);
                if (name != null) name.setText(label);
                if (q != null) q.setText("x" + qty);
                ordersListContainer.addView(itemView);
            }
        }
    }

    private void setupClickListeners() {
        if (btnDownload != null) {
            btnDownload.setOnClickListener(v -> {
                if (!isGenerating) {
                    isGenerating = true;
                    generateProfessionalForm();
                    new Handler().postDelayed(() -> isGenerating = false, 2000);
                }
            });
        }

        View.OnClickListener viewIdListener = v -> {
            if (isValidUrl(currentIdUrl)) showFullScreenID(currentIdUrl);
        };
        if (displayValidIDStatus != null) displayValidIDStatus.setOnClickListener(viewIdListener);
        if (ivValidID != null) ivValidID.setOnClickListener(viewIdListener);

        if (btnReturnDashboard != null) btnReturnDashboard.setOnClickListener(v -> returnToDashboard());
    }

    private void showFullScreenID(String url) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        ImageView iv = new ImageView(this);
        iv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iv.setBackgroundColor(Color.BLACK);
        Glide.with(this).load(url).into(iv);
        iv.setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(iv);
        dialog.show();
    }

    private void generateProfessionalForm() {
        if (currentSnapshot == null) {
            Toast.makeText(this, "Data not yet synced, please wait.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            View pdfView = LayoutInflater.from(this).inflate(R.layout.layout_receipt_pdf_template, null);
            TextView name = pdfView.findViewById(R.id.pdfCustomerName);
            if (name != null) name.setText(displayCustomerName.getText());

            pdfView.measure(View.MeasureSpec.makeMeasureSpec(1200, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            pdfView.layout(0, 0, pdfView.getMeasuredWidth(), pdfView.getMeasuredHeight());

            Bitmap bitmap = Bitmap.createBitmap(pdfView.getMeasuredWidth(), pdfView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.WHITE);
            pdfView.draw(canvas);

            PdfDocument document = new PdfDocument();
            PdfDocument.Page page = document.startPage(new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), 1).create());
            page.getCanvas().drawBitmap(bitmap, 0, 0, new Paint());
            document.finishPage(page);

            String tId = displayTicketID.getText().toString().replace("#", "").trim();
            String fName = "AquaTech_Receipt_" + tId + ".pdf";

            ContentValues cv = new ContentValues();
            cv.put(MediaStore.MediaColumns.DISPLAY_NAME, fName);
            cv.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
            cv.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AquaTech");

            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
            if (uri != null) {
                try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                    document.writeTo(os);
                    showTopNotification(fName, uri, tId);
                }
            }
            document.close();

        } catch (Exception e) {
            Toast.makeText(this, "Failed to generate PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showTopNotification(String fileName, Uri pdfUri, String ticketId) {
        runOnUiThread(() -> {
            if (rootLayout == null) return;
            final View notifView = LayoutInflater.from(this).inflate(R.layout.item_notification, rootLayout, false);
            ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
            int marginSide = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
            int marginTop = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics());
            params.setMargins(marginSide, marginTop, marginSide, 0);
            params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
            notifView.setLayoutParams(params);
            rootLayout.addView(notifView);
            notifView.setTranslationY(-500f);
            notifView.animate().translationY(0f).setDuration(600).setInterpolator(new OvershootInterpolator()).start();

            // 🛠️ NOTIFY CHATBOT ABOUT THE DOWNLOAD
            if (currentUid != null) {
                NotificationActivity.addNotification(currentUid, "You have downloaded a receipt: <b>" + fileName + "</b>", "PDF", ticketId);
            }

            notifView.setOnClickListener(v -> {
                // 🛠️ REDIRECT TO CHATBOT TO AUTO-SEND THE FILE
                Intent intent = new Intent(this, AquaBuddyActivity.class);
                intent.putExtra("SEND_FILE_MESSAGE", true);
                intent.putExtra("FILE_NAME", fileName);
                intent.putExtra("FILE_URI", pdfUri.toString());
                intent.putExtra("TICKET_ID", ticketId);
                startActivity(intent);
                removeNotif(notifView);
            });
            new Handler(Looper.getMainLooper()).postDelayed(() -> removeNotif(notifView), 6000);
        });
    }

    private void removeNotif(View v) {
        if (v != null && v.getParent() != null) {
            runOnUiThread(() -> v.animate().translationY(-500f).setDuration(500).withEndAction(() -> rootLayout.removeView(v)).start());
        }
    }

    private void startAnimations() {
        if (serviceCard != null) {
            serviceCard.setAlpha(0f);
            serviceCard.setTranslationY(100f);
            serviceCard.animate().alpha(1f).translationY(0f).setDuration(800).setInterpolator(new DecelerateInterpolator()).start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (statusRef != null && statusListener != null) statusRef.removeEventListener(statusListener);
    }

    @Override public void onBackPressed() { returnToDashboard(); }

    private void returnToDashboard() {
        Intent intent = new Intent(this, CustomerDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
