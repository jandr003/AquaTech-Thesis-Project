package com.example.aquatech;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Date;
import java.util.Map;

import com.bumptech.glide.Glide;

import java.io.File;

public class ServiceRequestActivity extends AppCompatActivity {

    private int currentStep = 1;
    private ImageView imgPreview, imgPlaceHolder, exitIcon;
    private TextView customerLabel, customerRefNum, customerValidIdLabel, tvCustomerNameValue, tvCustomerRefValue, takePhotoOrUpload;
    private CardView customerCard;
    private File photoFile;
    private Uri photoUri;

    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<PickVisualMediaRequest> galleryLauncher;
    private ActivityResultLauncher<Intent> fileLauncher;

    private EditText etCustomerNumber, etCustomerAddress, remarksInput;
    private TextView displayDate, startTimeText, endTimeText;
    private AppCompatSpinner purchaseTypeDropdown;
    private String unitModel;

    private TextView qtyCBC, qtySEDIMENT, qtyAquatal, qtyInlineFilter, qtyUvLampLabel, qtyTouchPanel, qtyPbcBoard, qtySMSF1µCBC2, qtySMSF10µSED2, qtyWayvalve2;
    private AppCompatButton incrementCBC, decrementCBC, incrementSEDIMENT, decrementSEDIMENT, incrementAquaTal, decrementAquatal, incrementInlineFilter, decrementInlineFilter, incrementUvLampLabel, decrementUvLampLabel, incrementTouchPanel, decrementTouchPanel, incrementPbcBoard, decrementPbcBoard, incrementSMSF1µCBC, decrementSMSF1µCBC1, incrementSMSF10µSED, decrementSMSF10µSED_1, incrementWayvalve, decrementWayvalve1;

    private int currentQtyCBC = 0, currentQtySEDIMENT = 0, currentQtyAquatal = 0, currentQtyInlineFilter = 0, currentQtyUvLamp = 0, currentQtyTouchPanel = 0, currentQtyPbcBoard = 0, currentQtySmsf1Cbc = 0, currentQtySmsf10Sed = 0, currentQtyWayValve = 0;

    private final int PRICE_3WAY_VALVE = 350, PRICE_0064_CBC = 2000, PRICE_0055_SED = 1000, PRICE_AQUATAL = 2000, PRICE_INLINE = 3000, PRICE_UV_LAMP = 1500, PRICE_TOUCH_PANEL = 750, PRICE_PBC_BOARD = 3000, PRICE_SMSF_1_CBC = 2000, PRICE_SMSF_10_SED = 1000;

    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private FirebaseAuth mAuth;
    private Button buttonSubmit;

    private FusedLocationProviderClient fusedLocationClient;
    private double currentLat = 0.0, currentLng = 0.0;
    private String originalProfileAddress = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_request);

        unitModel = getIntent().getStringExtra("UNIT_MODEL");
        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initializeViews();
        setupLaunchers();
        setupNavigation();
        setupQuantityHandlers();
        fetchCustomerDetails();
        requestLocationPermission();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }

        displayDate.setText(new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(new Date()));
        buttonSubmit.setOnClickListener(v -> checkExistingRequestBeforeSubmission());
        updateStepUI();
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
        }
    }

    private void checkExistingRequestBeforeSubmission() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Checking for active requests...");
        pd.show();

        FirebaseDatabase.getInstance(DB_URL).getReference("ServiceRequests")
                .orderByChild("userId").equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        pd.dismiss();
                        boolean hasActiveRequest = false;
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String status = ds.child("status").getValue(String.class);
                            if (status != null && (status.equalsIgnoreCase("Open") || 
                                status.equalsIgnoreCase("Assigned") || 
                                status.equalsIgnoreCase("In Progress") || 
                                status.equalsIgnoreCase("Arrived") || 
                                status.equalsIgnoreCase("Ongoing") || 
                                status.equalsIgnoreCase("Submission") || 
                                status.equalsIgnoreCase("Submitted"))) {
                                hasActiveRequest = true;
                                break;
                            }
                        }

                        if (hasActiveRequest) {
                            new AlertDialog.Builder(ServiceRequestActivity.this)
                                    .setTitle("Request Already Exists")
                                    .setMessage("You already have an active service request. Please wait for it to be completed or cancelled before making a new one.")
                                    .setPositiveButton("OK", (dialog, which) -> finish())
                                    .setCancelable(false)
                                    .show();
                        } else {
                            handleSubmission();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        pd.dismiss();
                        handleSubmission();
                    }
                });
    }

    private void handleSubmission() {
        String inputAddress = etCustomerAddress.getText().toString().trim();
        String inputMobile = etCustomerNumber.getText().toString().trim();

        validateAndColorTime();
        String sTime = startTimeText.getText().toString();
        String eTime = endTimeText.getText().toString();

        if (sTime.equals("---") || eTime.equals("---") || isOutsideOfficeHours(sTime) || isOutsideOfficeHours(eTime)) {
            showTimeWarning();
            return;
        }

        if (inputAddress.isEmpty() || inputMobile.isEmpty()) {
            Toast.makeText(this, "Please provide complete address and number", Toast.LENGTH_SHORT).show();
            currentStep = 1;
            updateStepUI();
            return;
        }

        Toast.makeText(this, "Pinpointing address location...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, new Locale("en", "PH"));
                List<Address> addresses = geocoder.getFromLocationName(inputAddress + ", Philippines", 1);
                if (addresses != null && !addresses.isEmpty()) {
                    currentLat = addresses.get(0).getLatitude();
                    currentLng = addresses.get(0).getLongitude();
                }
            } catch (Exception e) {
                Log.e("GEOCODE_FIX", "Error: " + e.getMessage());
            }
            runOnUiThread(this::proceedToAnimation);
        }).start();
    }

    private boolean isOutsideOfficeHours(String timeStr) {
        if (timeStr == null || timeStr.equals("---") || timeStr.isEmpty()) return true; 
        try {
            String[] parts = timeStr.split("[: ]");
            int hour = Integer.parseInt(parts[0]);
            int min = Integer.parseInt(parts[1]);
            String amPm = parts[2];

            int militaryHour = hour;
            if (amPm.equalsIgnoreCase("PM") && hour != 12) militaryHour += 12;
            if (amPm.equalsIgnoreCase("AM") && hour == 12) militaryHour = 0;

            if (militaryHour < 8 || militaryHour >= 17) return true;
            return false;
        } catch (Exception e) { return true; }
    }

    private void validateAndColorTime() {
        String sTime = startTimeText.getText().toString();
        String eTime = endTimeText.getText().toString();

        boolean sInvalid = isOutsideOfficeHours(sTime);
        boolean eInvalid = isOutsideOfficeHours(eTime);

        View sUnderline = findViewById(R.id.startTimeUnderline);
        View eUnderline = findViewById(R.id.endTimeUnderline);
        TextView serviceTimeLabel = findViewById(R.id.serviceTimeLabel);

        if (sInvalid) {
            startTimeText.setTextColor(Color.RED);
            if (sUnderline != null) sUnderline.setBackgroundColor(Color.RED);
        } else {
            startTimeText.setTextColor(Color.parseColor("#333333"));
            if (sUnderline != null) sUnderline.setBackgroundColor(Color.parseColor("#000000"));
        }

        if (eInvalid) {
            endTimeText.setTextColor(Color.RED);
            if (eUnderline != null) eUnderline.setBackgroundColor(Color.RED);
        } else {
            endTimeText.setTextColor(Color.parseColor("#333333"));
            if (eUnderline != null) eUnderline.setBackgroundColor(Color.parseColor("#000000"));
        }

        if (sInvalid || eInvalid) {
            if (serviceTimeLabel != null) {
                serviceTimeLabel.setText("Preferred Service Time (8:00 AM – 5:00 PM Only)");
                serviceTimeLabel.setTextColor(Color.RED);
            }
        } else {
            if (serviceTimeLabel != null) {
                serviceTimeLabel.setText("SERVICE TIME");
                serviceTimeLabel.setTextColor(Color.parseColor("#5B5B5B"));
            }
        }
    }

    private void showTimeWarning() {
        validateAndColorTime();
        new AlertDialog.Builder(this)
                .setTitle("Schedule Conflict")
                .setMessage("Appointments are available only from 8:00 AM to 5:00 PM. Please choose a preferred time within the allowed service hours.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void initializeViews() {
        exitIcon = findViewById(R.id.ExitIcon);
        exitIcon.setOnClickListener(v -> finish());
        customerLabel = findViewById(R.id.customerLabel);
        tvCustomerNameValue = findViewById(R.id.tvCustomerNameValue);
        customerRefNum = findViewById(R.id.customerRefNum);
        tvCustomerRefValue = findViewById(R.id.tvCustomerRefValue);
        etCustomerNumber = findViewById(R.id.etCustomerNumber);
        etCustomerAddress = findViewById(R.id.etCustomerAddress);
        displayDate = findViewById(R.id.displayDate);
        buttonSubmit = findViewById(R.id.buttonSubmit);
        imgPreview = findViewById(R.id.imgPreview);
        imgPlaceHolder = findViewById(R.id.imgPlaceHolder);
        takePhotoOrUpload = findViewById(R.id.takePhotoOrUpload);
        customerCard = findViewById(R.id.customerCard);
        remarksInput = findViewById(R.id.remarksInput);
        startTimeText = findViewById(R.id.startTimeText);
        endTimeText = findViewById(R.id.endTimeText);
        purchaseTypeDropdown = findViewById(R.id.purchaseTypeDropdown);
        customerValidIdLabel = findViewById(R.id.customerValidIdLabel);

        findViewById(R.id.EditPen1).setOnClickListener(v -> { etCustomerNumber.requestFocus(); InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE); imm.showSoftInput(etCustomerNumber, 0); });
        findViewById(R.id.EditPen2).setOnClickListener(v -> { etCustomerAddress.requestFocus(); ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(etCustomerAddress, 0); });

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.purchase_type_options, R.layout.spinner_item);
        adapter.setDropDownViewResource(R.layout.spinner_item);
        purchaseTypeDropdown.setAdapter(adapter);

        findViewById(R.id.startTimeArrow).setOnClickListener(v -> {
            AquaTimePickerDialog.showStart(getSupportFragmentManager(), (startTime, endTime) -> {
                startTimeText.setText(startTime);
                endTimeText.setText(endTime);
                validateAndColorTime();
            });
        });

        findViewById(R.id.endTimeArrow).setOnClickListener(v -> {
            AquaTimePickerDialog.showEnd(getSupportFragmentManager(), startTimeText.getText().toString(), (startTime, endTime) -> {
                endTimeText.setText(endTime);
                validateAndColorTime();
            });
        });

        View.OnClickListener uploadTrigger = v -> showUploadDialog();
        customerCard.setOnClickListener(uploadTrigger);
        imgPlaceHolder.setOnClickListener(uploadTrigger);
        takePhotoOrUpload.setOnClickListener(uploadTrigger);
    }

    private void setupLaunchers() {
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), result -> { if (result && photoUri != null) showPreview(photoUri); });
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> { if (uri != null) { photoUri = uri; showPreview(uri); } });
        fileLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> { if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) { Uri uri = result.getData().getData(); if (uri != null) { photoUri = uri; showPreview(uri); } } });
    }

    private void showUploadDialog() {
        String[] options = {"Take Photo", "Gallery", "File"};
        new AlertDialog.Builder(this).setTitle("Upload ID").setItems(options, (d, w) -> {
            if (w == 0) { if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) openCamera(); else ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100); }
            else if (w == 1) galleryLauncher.launch(new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
            else { Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT); intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("image/*"); fileLauncher.launch(intent); }
        }).show();
    }

    private void openCamera() {
        try { photoFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "id_" + System.currentTimeMillis() + ".jpg"); photoUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", photoFile); cameraLauncher.launch(photoUri); }
        catch (Exception e) { Toast.makeText(this, "Camera error", Toast.LENGTH_SHORT).show(); }
    }

    private void showPreview(Uri uri) {
        imgPreview.setVisibility(View.VISIBLE); imgPlaceHolder.setVisibility(View.GONE); takePhotoOrUpload.setVisibility(View.GONE);
        Glide.with(this).load(uri).fitCenter().into(imgPreview);
    }

    private void proceedToAnimation() {
        String ticketId = "ASC2026-" + (new java.util.Random().nextInt(9000) + 1000);
        Intent intent = new Intent(this, WaterDropFillAnimationActivity.class);

        intent.putExtra("TICKET_ID", ticketId);
        intent.putExtra("CUSTOMER_NAME", tvCustomerNameValue.getText().toString());
        intent.putExtra("CONTACT_NUMBER", etCustomerNumber.getText().toString());
        intent.putExtra("ADDRESS", etCustomerAddress.getText().toString());
        intent.putExtra("REF_NO", tvCustomerRefValue.getText().toString());
        intent.putExtra("DATE", displayDate.getText().toString());
        intent.putExtra("START_TIME", startTimeText.getText().toString());
        intent.putExtra("END_TIME", endTimeText.getText().toString());
        intent.putExtra("PURCHASE_TYPE", purchaseTypeDropdown.getSelectedItem().toString());
        intent.putExtra("REMARKS", remarksInput.getText().toString());
        intent.putExtra("TOTAL_AMOUNT", (double)calculateTotalAmount());
        intent.putExtra("UNIT_MODEL", unitModel);
        intent.putExtra("LATITUDE", currentLat);
        intent.putExtra("LONGITUDE", currentLng);

        if (photoUri != null) {
            intent.putExtra("SELECTED_ID_URI", photoUri.toString());
            intent.setClipData(ClipData.newRawUri("ID", photoUri));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        intent.putExtra("QTY_CBC", currentQtyCBC);
        intent.putExtra("QTY_SEDIMENT", currentQtySEDIMENT);
        intent.putExtra("QTY_AQUATAL", currentQtyAquatal);
        intent.putExtra("QTY_INLINE", currentQtyInlineFilter);
        intent.putExtra("QTY_UV_LAMP", currentQtyUvLamp);
        intent.putExtra("QTY_TOUCH_PANEL", currentQtyTouchPanel);
        intent.putExtra("QTY_PBC_BOARD", currentQtyPbcBoard);
        intent.putExtra("QTY_SMSF_1_CBC", currentQtySmsf1Cbc);
        intent.putExtra("QTY_SMSF_10_SED", currentQtySmsf10Sed);
        intent.putExtra("QTY_WAY_VALVE", currentQtyWayValve);

        startActivity(intent);
        finish();
    }

    private int calculateTotalAmount() {
        return (currentQtyWayValve * PRICE_3WAY_VALVE) + (currentQtyCBC * PRICE_0064_CBC) + (currentQtySEDIMENT * PRICE_0055_SED) + (currentQtyAquatal * PRICE_AQUATAL) + (currentQtyInlineFilter * PRICE_INLINE) + (currentQtyUvLamp * PRICE_UV_LAMP) + (currentQtyTouchPanel * PRICE_TOUCH_PANEL) + (currentQtyPbcBoard * PRICE_PBC_BOARD) + (currentQtySmsf1Cbc * PRICE_SMSF_1_CBC) + (currentQtySmsf10Sed * PRICE_SMSF_10_SED);
    }

    private void fetchCustomerDetails() {
        String uid = mAuth.getUid(); if (uid == null) return;
        FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                if (s.exists()) {
                    tvCustomerNameValue.setText(s.child("fullName").getValue(String.class));
                    String mobile = s.child("mobile").getValue(String.class);
                    etCustomerNumber.setText(mobile);
                    String addr = s.child("address").getValue(String.class);
                    etCustomerAddress.setText(addr);
                    originalProfileAddress = addr != null ? addr : "";
                    tvCustomerRefValue.setText(s.child("referenceNo").getValue(String.class));
                    Double lat = s.child("latitude").getValue(Double.class);
                    Double lng = s.child("longitude").getValue(Double.class);
                    if (lat != null && lng != null) {
                        currentLat = lat; currentLng = lng;
                    }
                    if (unitModel == null || unitModel.isEmpty()) {
                        unitModel = s.child("unitModel").getValue(String.class);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void setupQuantityHandlers() {
        qtyCBC = findViewById(R.id.qtyCBC); incrementCBC = findViewById(R.id.incrementCBC); decrementCBC = findViewById(R.id.decrementCBC);
        qtySEDIMENT = findViewById(R.id.qtySEDIMENT); incrementSEDIMENT = findViewById(R.id.incrementSEDIMENT); decrementSEDIMENT = findViewById(R.id.decrementSEDIMENT);
        qtyAquatal = findViewById(R.id.qtyAquatal); incrementAquaTal = findViewById(R.id.incrementAquaTal); decrementAquatal = findViewById(R.id.decrementAquatal);
        qtyInlineFilter = findViewById(R.id.qtyInlineFilter); incrementInlineFilter = findViewById(R.id.incrementInlineFilter); decrementInlineFilter = findViewById(R.id.decrementInlineFilter);
        qtyUvLampLabel = findViewById(R.id.qtyUvLampLabel); incrementUvLampLabel = findViewById(R.id.incrementUvLampLabel); decrementUvLampLabel = findViewById(R.id.decrementUvLampLabel);
        qtyTouchPanel = findViewById(R.id.qtyTouchPanel); incrementTouchPanel = findViewById(R.id.incrementTouchPanel); decrementTouchPanel = findViewById(R.id.decrementTouchPanel);
        qtyPbcBoard = findViewById(R.id.qtyPbcBoard); incrementPbcBoard = findViewById(R.id.incrementPbcBoard); decrementPbcBoard = findViewById(R.id.decrementPbcBoard);
        qtySMSF1µCBC2 = findViewById(R.id.qtySMSF1µCBC2); incrementSMSF1µCBC = findViewById(R.id.incrementSMSF1µCBC); decrementSMSF1µCBC1 = findViewById(R.id.decrementSMSF1µCBC1);
        qtySMSF10µSED2 = findViewById(R.id.qtySMSF10µSED2); incrementSMSF10µSED = findViewById(R.id.incrementSMSF10µSED); decrementSMSF10µSED_1 = findViewById(R.id.decrementSMSF10µSED_1);
        qtyWayvalve2 = findViewById(R.id.qtyWayvalve2); incrementWayvalve = findViewById(R.id.incrementWayvalve); decrementWayvalve1 = findViewById(R.id.decrementWayvalve1);

        incrementCBC.setOnClickListener(v -> { if (currentQtyCBC < 8) currentQtyCBC++; qtyCBC.setText(String.valueOf(currentQtyCBC)); });
        decrementCBC.setOnClickListener(v -> { if (currentQtyCBC > 0) currentQtyCBC--; qtyCBC.setText(String.valueOf(currentQtyCBC)); });
        incrementSEDIMENT.setOnClickListener(v -> { if (currentQtySEDIMENT < 8) currentQtySEDIMENT++; qtySEDIMENT.setText(String.valueOf(currentQtySEDIMENT)); });
        decrementSEDIMENT.setOnClickListener(v -> { if (currentQtySEDIMENT > 0) currentQtySEDIMENT--; qtySEDIMENT.setText(String.valueOf(currentQtySEDIMENT)); });
        incrementAquaTal.setOnClickListener(v -> { if (currentQtyAquatal < 8) currentQtyAquatal++; qtyAquatal.setText(String.valueOf(currentQtyAquatal)); });
        decrementAquatal.setOnClickListener(v -> { if (currentQtyAquatal > 0) currentQtyAquatal--; qtyAquatal.setText(String.valueOf(currentQtyAquatal)); });
        incrementInlineFilter.setOnClickListener(v -> { if (currentQtyInlineFilter < 8) currentQtyInlineFilter++; qtyInlineFilter.setText(String.valueOf(currentQtyInlineFilter)); });
        decrementInlineFilter.setOnClickListener(v -> { if (currentQtyInlineFilter > 0) currentQtyInlineFilter--; qtyInlineFilter.setText(String.valueOf(currentQtyInlineFilter)); });
        incrementUvLampLabel.setOnClickListener(v -> { if (currentQtyUvLamp < 8) currentQtyUvLamp++; qtyUvLampLabel.setText(String.valueOf(currentQtyUvLamp)); });
        decrementUvLampLabel.setOnClickListener(v -> { if (currentQtyUvLamp > 0) currentQtyUvLamp--; qtyUvLampLabel.setText(String.valueOf(currentQtyUvLamp)); });
        incrementTouchPanel.setOnClickListener(v -> { if (currentQtyTouchPanel < 8) currentQtyTouchPanel++; qtyTouchPanel.setText(String.valueOf(currentQtyTouchPanel)); });
        decrementTouchPanel.setOnClickListener(v -> { if (currentQtyTouchPanel > 0) currentQtyTouchPanel--; qtyTouchPanel.setText(String.valueOf(currentQtyTouchPanel)); });
        incrementPbcBoard.setOnClickListener(v -> { if (currentQtyPbcBoard < 8) currentQtyPbcBoard++; qtyPbcBoard.setText(String.valueOf(currentQtyPbcBoard)); });
        decrementPbcBoard.setOnClickListener(v -> { if (currentQtyPbcBoard > 0) currentQtyPbcBoard--; qtyPbcBoard.setText(String.valueOf(currentQtyPbcBoard)); });
        incrementSMSF1µCBC.setOnClickListener(v -> { if (currentQtySmsf1Cbc < 8) currentQtySmsf1Cbc++; qtySMSF1µCBC2.setText(String.valueOf(currentQtySmsf1Cbc)); });
        decrementSMSF1µCBC1.setOnClickListener(v -> { if (currentQtySmsf1Cbc > 0) currentQtySmsf1Cbc--; qtySMSF1µCBC2.setText(String.valueOf(currentQtySmsf1Cbc)); });
        incrementSMSF10µSED.setOnClickListener(v -> { if (currentQtySmsf10Sed < 8) currentQtySmsf10Sed++; qtySMSF10µSED2.setText(String.valueOf(currentQtySmsf10Sed)); });
        decrementSMSF10µSED_1.setOnClickListener(v -> { if (currentQtySmsf10Sed > 0) currentQtySmsf10Sed--; qtySMSF10µSED2.setText(String.valueOf(currentQtySmsf10Sed)); });
        incrementWayvalve.setOnClickListener(v -> { if (currentQtyWayValve < 8) currentQtyWayValve++; qtyWayvalve2.setText(String.valueOf(currentQtyWayValve)); });
        decrementWayvalve1.setOnClickListener(v -> { if (currentQtyWayValve > 0) currentQtyWayValve--; qtyWayvalve2.setText(String.valueOf(currentQtyWayValve)); });
    }

    private void setupNavigation() {
        findViewById(R.id.buttonNext).setOnClickListener(v -> { hideKeyboard(); currentStep = 2; updateStepUI(); });

        findViewById(R.id.buttonNext2).setOnClickListener(v -> {
            String sTime = startTimeText.getText().toString();
            String eTime = endTimeText.getText().toString();

            validateAndColorTime();

            if (isOutsideOfficeHours(sTime) || isOutsideOfficeHours(eTime)) {
                showTimeWarning();
                return;
            }

            hideKeyboard();
            currentStep = 3;
            updateStepUI();
        });

        findViewById(R.id.buttonNext3).setOnClickListener(v -> {
            if (currentQtyCBC == 0 && currentQtySEDIMENT == 0 && currentQtyAquatal == 0 &&
                    currentQtyInlineFilter == 0 && currentQtyUvLamp == 0 && currentQtyTouchPanel == 0 &&
                    currentQtyPbcBoard == 0 && currentQtySmsf1Cbc == 0 && currentQtySmsf10Sed == 0) {

                new AlertDialog.Builder(this)
                        .setTitle("No Items Selected")
                        .setMessage("Please add at least one item (Filter or Other Parts) to your service request before proceeding.")
                        .setPositiveButton("OK", null)
                        .show();
                return;
            }

            hideKeyboard();
            currentStep = 4;
            updateStepUI();
        });
        
        findViewById(R.id.buttonBack).setOnClickListener(v -> { if (currentStep > 1) { currentStep--; updateStepUI(); } });
        findViewById(R.id.buttonBack3).setOnClickListener(v -> { currentStep = 2; updateStepUI(); });
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void setActive(TextView tv, CardView bg, String number) {
        tv.setText(number); tv.setTextColor(Color.WHITE); tv.setVisibility(View.VISIBLE);
        bg.setCardBackgroundColor(Color.parseColor("#4B91C6")); bg.setElevation(8f);
    }

    private void setCompleted(TextView tv, CardView bg) {
        tv.setVisibility(View.VISIBLE); tv.setText("\u2713"); tv.setTextColor(Color.WHITE);
        bg.setCardBackgroundColor(Color.parseColor("#4B91C6")); bg.setElevation(0f);
    }

    private void resetCircle(TextView tv, CardView bg, String number) {
        tv.setVisibility(View.VISIBLE); tv.setText(number); tv.setTextColor(Color.parseColor("#3775BB"));
        bg.setCardBackgroundColor(Color.WHITE); bg.setElevation(8f);
    }

    private void updateStepUI() {
        TextView tv1 = findViewById(R.id.circleText1), tv2 = findViewById(R.id.circleText2), tv3 = findViewById(R.id.circleText3);
        CardView circle1 = findViewById(R.id.circleNumber1), circle2 = findViewById(R.id.circleNumber2), circle3 = findViewById(R.id.circleNumber3);
        View underline1 = findViewById(R.id.UnderLine), underline2 = findViewById(R.id.underline2), underline3 = findViewById(R.id.underline3), completeUnderline = findViewById(R.id.CompleteUnderline);

        resetCircle(tv1, circle1, "1"); resetCircle(tv2, circle2, "2"); resetCircle(tv3, circle3, "3");
        underline1.setVisibility(View.GONE); underline2.setVisibility(View.GONE); underline3.setVisibility(View.GONE); completeUnderline.setVisibility(View.GONE);

        if (currentStep == 1) { setActive(tv1, circle1, "1"); underline1.setVisibility(View.VISIBLE); }
        else if (currentStep == 2) { setCompleted(tv1, circle1); setActive(tv2, circle2, "2"); underline2.setVisibility(View.VISIBLE); }
        else if (currentStep == 3) { setCompleted(tv1, circle1); setCompleted(tv2, circle2); setActive(tv3, circle3, "3"); underline3.setVisibility(View.VISIBLE); }
        else if (currentStep == 4) { setCompleted(tv1, circle1); setCompleted(tv2, circle2); setCompleted(tv3, circle3); completeUnderline.setVisibility(View.VISIBLE); }

        int s1 = (currentStep == 1) ? View.VISIBLE : View.GONE;
        int s2 = (currentStep == 2) ? View.VISIBLE : View.GONE;
        int s3 = (currentStep == 3) ? View.VISIBLE : View.GONE;
        int s4 = (currentStep == 4) ? View.VISIBLE : View.GONE;

        findViewById(R.id.customerLabel).setVisibility(s1); tvCustomerNameValue.setVisibility(s1); etCustomerNumber.setVisibility(s1); etCustomerAddress.setVisibility(s1);
        findViewById(R.id.customerUnderline).setVisibility(s1); findViewById(R.id.customerUnderline2).setVisibility(s1);
        findViewById(R.id.customerUnderline3).setVisibility(s1); findViewById(R.id.customerUnderline4).setVisibility(s1);
        findViewById(R.id.customerUnderline5).setVisibility(s1); findViewById(R.id.customerNumber).setVisibility(s1);
        findViewById(R.id.customerAddress).setVisibility(s1); findViewById(R.id.customerRefNum).setVisibility(s1);
        tvCustomerRefValue.setVisibility(s1);
        findViewById(R.id.customerDate).setVisibility(s1); displayDate.setVisibility(s1);
        findViewById(R.id.EditPen1).setVisibility(s1); findViewById(R.id.EditPen2).setVisibility(s1); findViewById(R.id.buttonNext).setVisibility(s1);

        findViewById(R.id.serviceTimeLabel).setVisibility(s2); findViewById(R.id.serviceTimeRow).setVisibility(s2); findViewById(R.id.startTimeUnderline).setVisibility(s2);
        findViewById(R.id.startTimeArrow).setVisibility(s2); startTimeText.setVisibility(s2); findViewById(R.id.toLabel).setVisibility(s2);
        findViewById(R.id.serviceEndTimeRow).setVisibility(s2); findViewById(R.id.endTimeUnderline).setVisibility(s2); findViewById(R.id.endTimeArrow).setVisibility(s2);
        endTimeText.setVisibility(s2); findViewById(R.id.purchaseTypeLabel).setVisibility(s2); purchaseTypeDropdown.setVisibility(s2);
        customerValidIdLabel.setVisibility(s2); if (currentStep == 2) customerValidIdLabel.setText(android.text.Html.fromHtml("<b>Customer Valid ID</b> (optional)"));
        customerCard.setVisibility(s2); findViewById(R.id.buttonBack).setVisibility(s2); findViewById(R.id.buttonNext2).setVisibility(s2);

        findViewById(R.id.circle3ScrollContainer).setVisibility((currentStep == 3 || currentStep == 4) ? View.VISIBLE : View.GONE);
        findViewById(R.id.filterPreventiveLabel).setVisibility(s3); findViewById(R.id.qtyLabel).setVisibility(s3); findViewById(R.id.cbcLabel).setVisibility(s3);
        qtyCBC.setVisibility(s3); incrementCBC.setVisibility(s3); decrementCBC.setVisibility(s3); findViewById(R.id.sedimentLabel).setVisibility(s3);
        qtySEDIMENT.setVisibility(s3); incrementSEDIMENT.setVisibility(s3); decrementSEDIMENT.setVisibility(s3); findViewById(R.id.OtherPartsLabel).setVisibility(s3);
        findViewById(R.id.AquatalLabel).setVisibility(s3); qtyAquatal.setVisibility(s3); incrementAquaTal.setVisibility(s3); decrementAquatal.setVisibility(s3);
        findViewById(R.id.InlineFilterLabel).setVisibility(s3); qtyInlineFilter.setVisibility(s3); incrementInlineFilter.setVisibility(s3); decrementInlineFilter.setVisibility(s3);
        findViewById(R.id.UvLampLabel).setVisibility(s3); qtyUvLampLabel.setVisibility(s3); incrementUvLampLabel.setVisibility(s3); decrementUvLampLabel.setVisibility(s3);
        findViewById(R.id.TouchPanel).setVisibility(s3); qtyTouchPanel.setVisibility(s3); incrementTouchPanel.setVisibility(s3); decrementTouchPanel.setVisibility(s3);
        findViewById(R.id.PbcBoard).setVisibility(s3); qtyPbcBoard.setVisibility(s3); incrementPbcBoard.setVisibility(s3); decrementPbcBoard.setVisibility(s3);
        findViewById(R.id.SMSF1µ).setVisibility(s3); qtySMSF1µCBC2.setVisibility(s3); incrementSMSF1µCBC.setVisibility(s3); decrementSMSF1µCBC1.setVisibility(s3);
        findViewById(R.id.SMSF10µSED).setVisibility(s3); qtySMSF10µSED2.setVisibility(s3); incrementSMSF10µSED.setVisibility(s3); decrementSMSF10µSED_1.setVisibility(s3);
        findViewById(R.id.buttonBack3).setVisibility(s3); findViewById(R.id.buttonNext3).setVisibility(s3);

        findViewById(R.id.installationKitLabel).setVisibility(s4); findViewById(R.id.Wayvalve).setVisibility(s4);
        incrementWayvalve.setVisibility(s4); decrementWayvalve1.setVisibility(s4); qtyWayvalve2.setVisibility(s4);
        findViewById(R.id.remarksLabel).setVisibility(s4); remarksInput.setVisibility(s4);
        findViewById(R.id.remarksNotice).setVisibility(s4); findViewById(R.id.remarksNotice1).setVisibility(s4);
        findViewById(R.id.buttonSubmit).setVisibility(s4);
        if (currentStep == 4) {
            String text = "INSTALLATION KIT (Add if Needed)";
            SpannableString spannable = new SpannableString(text);
            spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, "INSTALLATION KIT".length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new RelativeSizeSpan(0.8f), "INSTALLATION KIT ".length(), text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ((TextView)findViewById(R.id.installationKitLabel)).setText(spannable);
        }
    }
}