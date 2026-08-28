package com.example.aquatech;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WaterDropFillAnimationActivity extends AppCompatActivity {

    private WaterDropView waterWaveView;
    private CircularProgressIndicator circularProgress;
    private TextView tv_to_be_continued;
    private ImageView ivCheckIcon;

    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;
    private StorageReference storageRef;

    private String ticketId, savedUri, remarks, customerName, contactNumber, address, referenceNo, date, startTime, endTime, purchaseType, unitModel;
    private double totalAmount, latitude, longitude;
    private int qtyCBC, qtySediment, qtyWayValve, qtyAquatal, qtyInline, qtyUvLamp, qtyTouchPanel, qtyPbcBoard, qtySmsf1Cbc, qtySmsf10Sed;

    private boolean isFirebaseDone = false;
    private boolean isAnimationDone = false;
    private String firebaseImageUrl = "";

    private ValueAnimator pulseAnimator;

    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_water_drop_fill);

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance(DB_URL).getReference("ServiceRequests");
        storageRef = FirebaseStorage.getInstance().getReference("CustomerIDs");

        loadIntentExtras();
        initializeViews();
        startAnimations();
        geocodeAddressAndUpload();
    }

    private void loadIntentExtras() {
        Intent intent = getIntent();
        ticketId = intent.getStringExtra("TICKET_ID");
        savedUri = intent.getStringExtra("SELECTED_ID_URI");
        remarks = intent.getStringExtra("REMARKS");
        customerName = intent.getStringExtra("CUSTOMER_NAME");
        contactNumber = intent.getStringExtra("CONTACT_NUMBER");
        address = intent.getStringExtra("ADDRESS");
        referenceNo = intent.getStringExtra("REF_NO");
        date = intent.getStringExtra("DATE");
        startTime = intent.getStringExtra("START_TIME");
        endTime = intent.getStringExtra("END_TIME");
        purchaseType = intent.getStringExtra("PURCHASE_TYPE");
        totalAmount = intent.getDoubleExtra("TOTAL_AMOUNT", 0.0);
        unitModel = intent.getStringExtra("UNIT_MODEL");
        latitude = intent.getDoubleExtra("LATITUDE", 0.0);
        longitude = intent.getDoubleExtra("LONGITUDE", 0.0);

        qtyCBC = intent.getIntExtra("QTY_CBC", 0);
        qtySediment = intent.getIntExtra("QTY_SEDIMENT", 0);
        qtyWayValve = intent.getIntExtra("QTY_WAY_VALVE", 0);
        qtyAquatal = intent.getIntExtra("QTY_AQUATAL", 0);
        qtyInline = intent.getIntExtra("QTY_INLINE", 0);
        qtyUvLamp = intent.getIntExtra("QTY_UV_LAMP", 0);
        qtyTouchPanel = intent.getIntExtra("QTY_TOUCH_PANEL", 0);
        qtyPbcBoard = intent.getIntExtra("QTY_PBC_BOARD", 0);
        qtySmsf1Cbc = intent.getIntExtra("QTY_SMSF_1_CBC", 0);
        qtySmsf10Sed = intent.getIntExtra("QTY_SMSF_10_SED", 0);
    }

    private void initializeViews() {
        waterWaveView = findViewById(R.id.waterWaveView);
        circularProgress = findViewById(R.id.circularProgress);
        tv_to_be_continued = findViewById(R.id.tv_to_be_continued);
        ivCheckIcon = findViewById(R.id.ivCheckIcon);
        tv_to_be_continued.setTextColor(Color.parseColor("#1C4E75"));
    }

    private void startAnimations() {
        pulseAnimator = ValueAnimator.ofFloat(1.0f, 1.1f);
        pulseAnimator.setDuration(500);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseAnimator.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            if(waterWaveView != null) { waterWaveView.setScaleX(scale); waterWaveView.setScaleY(scale); }
            if(tv_to_be_continued != null) { tv_to_be_continued.setScaleX(scale); tv_to_be_continued.setScaleY(scale); }
        });
        pulseAnimator.start();

        ValueAnimator animator = ValueAnimator.ofInt(0, 100);
        animator.setDuration(4000);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            int progress = (int) animation.getAnimatedValue();
            if(waterWaveView != null) waterWaveView.setWaveProgress(progress);
            if(circularProgress != null) circularProgress.setProgress(progress);
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                showSuccessUI();
            }
        });
        animator.start();
    }

    private void showSuccessUI() {
        if (pulseAnimator != null) pulseAnimator.cancel();
        if (circularProgress != null) circularProgress.setIndicatorColor(Color.parseColor("#4CAF50"));

        if (waterWaveView != null) {
            waterWaveView.animate()
                    .scaleX(0f).scaleY(0f).alpha(0f).setDuration(400)
                    .withEndAction(() -> {
                        waterWaveView.setVisibility(View.GONE);
                        if (ivCheckIcon != null) {
                            ivCheckIcon.setVisibility(View.VISIBLE);
                            ivCheckIcon.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(600).start();
                        }
                        if (tv_to_be_continued != null) {
                            tv_to_be_continued.setText("Request Sent!");
                            tv_to_be_continued.setTextColor(Color.parseColor("#4CAF50"));
                        }
                        isAnimationDone = true;
                        checkReadyToNavigate();
                    }).start();
        }
    }

    private void geocodeAddressAndUpload() {
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, new Locale("en", "PH"));
                String searchStr = address.replace("corner", "").replace("Corner", "") + ", Philippines";
                List<Address> addresses = geocoder.getFromLocationName(searchStr, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    latitude = addresses.get(0).getLatitude();
                    longitude = addresses.get(0).getLongitude();
                }
            } catch (Exception e) {}
            runOnUiThread(this::uploadRequestToFirebase);
        }).start();
    }

    private void uploadRequestToFirebase() {
        if (savedUri != null && !savedUri.isEmpty()) {
            try {
                StorageReference fileRef = storageRef.child(ticketId + ".jpg");
                fileRef.putFile(Uri.parse(savedUri))
                        .addOnSuccessListener(ts -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            firebaseImageUrl = uri.toString();
                            pushDataToDatabase();
                        }).addOnFailureListener(e -> { firebaseImageUrl = "Upload Failed"; pushDataToDatabase(); }))
                        .addOnFailureListener(e -> { firebaseImageUrl = "Upload Failed"; pushDataToDatabase(); });
            } catch (Exception e) {
                firebaseImageUrl = "Upload Failed";
                pushDataToDatabase();
            }
        } else {
            firebaseImageUrl = "None Provided";
            pushDataToDatabase();
        }
    }

    private void pushDataToDatabase() {
        Map<String, Object> data = new HashMap<>();
        data.put("ticketId", ticketId);
        data.put("customerName", customerName);
        data.put("contactNumber", contactNumber);
        data.put("address", address);
        data.put("referenceNo", referenceNo);
        data.put("date", date);
        data.put("startTime", startTime);
        data.put("endTime", endTime);
        data.put("timeRange", startTime + " - " + endTime);
        data.put("purchaseType", purchaseType);
        data.put("validIdUrl", firebaseImageUrl);
        data.put("remarks", remarks);
        data.put("totalAmount", totalAmount);
        data.put("unitName", unitModel);
        data.put("status", "Open");
        data.put("userId", mAuth.getUid());
        data.put("latitude", latitude);
        data.put("longitude", longitude);
        data.put("timestamp", ServerValue.TIMESTAMP);

        data.put("qty_cbc", qtyCBC);
        data.put("qty_sediment", qtySediment);
        data.put("qty_wayvalve", qtyWayValve);
        data.put("qty_aquatal", qtyAquatal);
        data.put("qty_inline", qtyInline);
        data.put("qty_uvlamp", qtyUvLamp);
        data.put("qty_touchpanel", qtyTouchPanel);
        data.put("qty_pbcboard", qtyPbcBoard);
        data.put("qty_smsf1", qtySmsf1Cbc);
        data.put("qty_smsf10", qtySmsf10Sed);

        dbRef.child(ticketId).setValue(data).addOnCompleteListener(task -> {
            AdminDashboardActivity.addAdminLog("New Service Request Form submitted by " + customerName);
            isFirebaseDone = true;
            checkReadyToNavigate();
        });
    }

    private void checkReadyToNavigate() {
        if (isAnimationDone && isFirebaseDone) {
            new Handler(Looper.getMainLooper()).postDelayed(this::navigateToSuccess, 500);
        }
    }

    private void navigateToSuccess() {
        Intent intent = new Intent(this, ServiceRequestSuccessActivity.class);
        intent.putExtra("TICKET_ID", ticketId);
        intent.putExtra("CUSTOMER_NAME", customerName);
        intent.putExtra("CONTACT_NUMBER", contactNumber);
        intent.putExtra("ADDRESS", address);
        intent.putExtra("REF_NO", referenceNo);
        intent.putExtra("TOTAL_AMOUNT", totalAmount);
        intent.putExtra("REMARKS", remarks);
        intent.putExtra("DATE", date);
        intent.putExtra("VALID_ID_URL", firebaseImageUrl);
        intent.putExtra("UNIT_MODEL", unitModel);

        intent.putExtra("qty_wayvalve", qtyWayValve);
        intent.putExtra("qty_cbc", qtyCBC);
        intent.putExtra("qty_sediment", qtySediment);
        intent.putExtra("qty_aquatal", qtyAquatal);
        intent.putExtra("qty_inline", qtyInline);
        intent.putExtra("qty_uvlamp", qtyUvLamp);
        intent.putExtra("qty_touchpanel", qtyTouchPanel);
        intent.putExtra("qty_pbcboard", qtyPbcBoard);
        intent.putExtra("qty_smsf1", qtySmsf1Cbc);
        intent.putExtra("qty_smsf10", qtySmsf10Sed);

        if (savedUri != null && !savedUri.isEmpty()) {
            intent.putExtra("SELECTED_ID_URI", savedUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        startActivity(intent);
        finish();
    }
}
