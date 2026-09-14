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
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatSpinner;
import com.google.android.material.card.MaterialCardView;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Date;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.Random;

public class ServiceRequestActivity extends AppCompatActivity {

    private int currentStep = 4;
    private ImageView imgPreview, imgPlaceHolder, exitIcon;
    private TextView customerLabel, customerRefNum, customerValidIdLabel, tvCustomerNameValue, tvCustomerRefValue, takePhotoOrUpload;
    private CardView customerCard, btnNeedHelp;
    private File photoFile;
    private Uri photoUri;

    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<PickVisualMediaRequest> galleryLauncher;
    private ActivityResultLauncher<Intent> fileLauncher;

    private EditText etCustomerNumber, etCustomerAddress, remarksInput;
    private TextView displayDate, startTimeText, endTimeText, tvPaymentTotal;
    private AppCompatSpinner purchaseTypeDropdown;
    private String unitModel;

    private RadioButton rbCOD, rbGCash, rbMaya, rbBank;
    private MaterialCardView cardGCash, cardMaya, cardBank, cardCOD;
    private LinearLayout bankDetailsContainer;
    private TextView tvBankAmount, tvBankAccNum, tvBankAccName;
    private EditText etBankRef;
    private AppCompatSpinner bankSpinner;
    private ImageView imgReceiptPreview;
    private boolean isUploadingReceipt = false;
    private Uri receiptUri;

    private TextView qtyCBC, qtySEDIMENT, qtyAquatal, qtyInlineFilter, qtyUvLampLabel, qtyTouchPanel, qtyPbcBoard, qtySMSF1uCBC2, qtySMSF10uSED2, qtyWayvalve2;
    private AppCompatButton incrementCBC, decrementCBC, incrementSEDIMENT, decrementSEDIMENT, incrementAquaTal, decrementAquatal, incrementInlineFilter, decrementInlineFilter, incrementUvLampLabel, decrementUvLampLabel, incrementTouchPanel, decrementTouchPanel, incrementPbcBoard, decrementPbcBoard, incrementSMSF1uCBC, decrementSMSF1uCBC1, incrementSMSF10uSED, decrementSMSF10uSED_1, incrementWayvalve, decrementWayvalve1;
    private AppCompatButton btnSubscription, btnOutright, btnOccular, btnBackNav;
    private TextView tvServiceFee;

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
        btnNeedHelp = findViewById(R.id.btnNeedHelp);
        btnNeedHelp.setOnClickListener(v -> showHelpDialog());

        rbCOD = findViewById(R.id.rbCOD);
        rbGCash = findViewById(R.id.rbGCash);
        rbMaya = findViewById(R.id.rbMaya);
        rbBank = findViewById(R.id.rbBank);
        
        cardGCash = findViewById(R.id.cardGCash);
        cardMaya = findViewById(R.id.cardMaya);
        cardBank = findViewById(R.id.cardBank);
        cardCOD = findViewById(R.id.cardCOD);
        
        tvPaymentTotal = findViewById(R.id.tvPaymentTotal);
        tvServiceFee = findViewById(R.id.tvServiceFee);

        bankDetailsContainer = findViewById(R.id.bankDetailsContainer);
        tvBankAmount = findViewById(R.id.tvBankAmount);
        tvBankAccNum = findViewById(R.id.tvBankAccNum);
        tvBankAccName = findViewById(R.id.tvBankAccName);
        etBankRef = findViewById(R.id.etBankRef);
        imgReceiptPreview = findViewById(R.id.imgReceiptPreview);
        bankSpinner = findViewById(R.id.bankSpinner);

        ArrayAdapter<CharSequence> bankAdapter = ArrayAdapter.createFromResource(this, R.array.bank_options, android.R.layout.simple_spinner_item);
        bankAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bankSpinner.setAdapter(bankAdapter);

        bankSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedBank = parent.getItemAtPosition(position).toString();
                if (selectedBank.contains("BPI")) tvBankAccNum.setText("1234 5678 90");
                else if (selectedBank.contains("BDO")) tvBankAccNum.setText("0012 3456 7890");
                else if (selectedBank.contains("Metrobank")) tvBankAccNum.setText("987 654 321");
                else if (selectedBank.contains("Unionbank")) tvBankAccNum.setText("1094 1234 5678");
                else tvBankAccNum.setText("0000 0000 0000");
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnSubscription = findViewById(R.id.btnSubscription);
        btnOutright = findViewById(R.id.btnOutright);
        btnOccular = findViewById(R.id.btnOccular);
        btnBackNav = findViewById(R.id.btnBackNav);
        btnBackNav.setOnClickListener(v -> handleBackNavigation());

        findViewById(R.id.EditPen1).setOnClickListener(v -> { etCustomerNumber.requestFocus(); InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE); imm.showSoftInput(etCustomerNumber, 0); });
        findViewById(R.id.EditPen2).setOnClickListener(v -> { etCustomerAddress.requestFocus(); ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(etCustomerAddress, 0); });

        // Hidden purchase type setup for data compatibility
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.purchase_type_options, android.R.layout.simple_spinner_item);
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

        View.OnClickListener paymentClickListener = v -> {
            rbGCash.setChecked(v.getId() == R.id.cardGCash);
            rbMaya.setChecked(v.getId() == R.id.cardMaya);
            rbBank.setChecked(v.getId() == R.id.cardBank);
            rbCOD.setChecked(v.getId() == R.id.cardCOD);

            cardGCash.setStrokeColor(v.getId() == R.id.cardGCash ? Color.parseColor("#2196F3") : Color.parseColor("#E2E8F0"));
            cardMaya.setStrokeColor(v.getId() == R.id.cardMaya ? Color.parseColor("#2196F3") : Color.parseColor("#E2E8F0"));
            cardBank.setStrokeColor(v.getId() == R.id.cardBank ? Color.parseColor("#2196F3") : Color.parseColor("#E2E8F0"));
            cardCOD.setStrokeColor(v.getId() == R.id.cardCOD ? Color.parseColor("#2196F3") : Color.parseColor("#E2E8F0"));

            if (v.getId() == R.id.cardBank) {
                bankDetailsContainer.setVisibility(View.VISIBLE);
                refreshTotal();
            } else {
                bankDetailsContainer.setVisibility(View.GONE);
            }
        };

        cardGCash.setOnClickListener(paymentClickListener);
        cardMaya.setOnClickListener(paymentClickListener);
        cardBank.setOnClickListener(paymentClickListener);
        cardCOD.setOnClickListener(paymentClickListener);

        // Set Default
        rbCOD.setChecked(true);
        cardCOD.setStrokeColor(Color.parseColor("#2196F3"));

        findViewById(R.id.btnUploadReceipt).setOnClickListener(v -> {
            isUploadingReceipt = true;
            showUploadDialog();
        });

        View.OnClickListener idUploadTrigger = v -> {
            isUploadingReceipt = false;
            showUploadDialog();
        };
        customerCard.setOnClickListener(idUploadTrigger);
        findViewById(R.id.idPlaceholder).setOnClickListener(idUploadTrigger);
        takePhotoOrUpload.setOnClickListener(idUploadTrigger);

        setupPurchaseTypeHandlers();
    }

    private void handleBackNavigation() {
        if (currentStep > 1) {
            currentStep--;
            updateStepUI();
        }
    }

    private void setupPurchaseTypeHandlers() {
        View.OnClickListener listener = v -> {
            btnSubscription.setSelected(false);
            btnOutright.setSelected(false);
            btnOccular.setSelected(false);
            v.setSelected(true);
        };
        btnSubscription.setOnClickListener(listener);
        btnOutright.setOnClickListener(listener);
        btnOccular.setOnClickListener(listener);
        btnSubscription.setSelected(true);
    }

    private void showFAQs() {
        new AlertDialog.Builder(this)
                .setTitle("Quick Service Guide")
                .setMessage("• Process: Fill info > Pick schedule > Select items > Choose payment.\n\n" +
                        "• Office Hours: Service is only available from 8:00 AM to 5:00 PM.\n\n" +
                        "• ID Upload: Attaching a valid ID helps us verify your request faster.\n\n" +
                        "• Payment: For Bank Transfers, please upload your receipt for verification.")
                .setPositiveButton("Got it", null)
                .show();
    }

    private void showHelpDialog() {
        String[] options = {"📞 Call Support", "💬 Message Support", "❓ FAQ Guide", "📍 Our Office"};
        new AlertDialog.Builder(this).setTitle("Support Center").setItems(options, (dialog, which) -> {
            if (which == 0) { Intent i = new Intent(Intent.ACTION_DIAL); i.setData(Uri.parse("tel:09171234567")); startActivity(i); }
            else if (which == 1) Toast.makeText(this, "Support chat opening...", Toast.LENGTH_SHORT).show();
            else if (which == 2) showFAQs();
            else if (which == 3) { new AlertDialog.Builder(this).setTitle("Office").setMessage("123 Purity Ave, Quezon City").setPositiveButton("OK", null).show(); }
        }).setNegativeButton("Close", null).show();
    }

    private void setupLaunchers() {
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), result -> { if (result && photoUri != null) { if (isUploadingReceipt) showReceiptPreview(photoUri); else showPreview(photoUri); } });
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> { if (uri != null) { if (isUploadingReceipt) { receiptUri = uri; showReceiptPreview(uri); } else { photoUri = uri; showPreview(uri); } } });
        fileLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> { if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) { Uri uri = result.getData().getData(); if (uri != null) { if (isUploadingReceipt) { receiptUri = uri; showReceiptPreview(uri); } else { photoUri = uri; showPreview(uri); } } } });
    }

    private void showReceiptPreview(Uri uri) {
        imgReceiptPreview.setVisibility(View.VISIBLE);
        findViewById(R.id.receiptPlaceholder).setVisibility(View.GONE);
        Glide.with(this).load(uri).centerCrop().into(imgReceiptPreview);
    }

    private void showUploadDialog() {
        String[] options = {"Take Photo", "Gallery", "File"};
        new AlertDialog.Builder(this).setTitle("Upload ID").setItems(options, (d, w) -> {
            if (w == 0) { if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) openCamera(); else ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100); }
            else if (w == 1) galleryLauncher.launch(new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
            else { Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT); intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("image/*"); fileLauncher.launch(intent); }
        }).show();
    }

    private void openCamera() { try { photoFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "id_" + System.currentTimeMillis() + ".jpg"); photoUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", photoFile); cameraLauncher.launch(photoUri); } catch (Exception e) { Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show(); } }

    private void showPreview(Uri uri) { imgPreview.setVisibility(View.VISIBLE); findViewById(R.id.idPlaceholder).setVisibility(View.GONE); Glide.with(this).load(uri).fitCenter().into(imgPreview); }

    private void proceedToAnimation() {
        Intent intent = new Intent(this, WaterDropFillAnimationActivity.class);
        intent.putExtra("TICKET_ID", "ASC2026-" + (new Random().nextInt(9000) + 1000));
        intent.putExtra("CUSTOMER_NAME", tvCustomerNameValue.getText().toString());
        intent.putExtra("CONTACT_NUMBER", etCustomerNumber.getText().toString());
        intent.putExtra("ADDRESS", etCustomerAddress.getText().toString());
        intent.putExtra("REF_NO", tvCustomerRefValue.getText().toString());
        intent.putExtra("DATE", displayDate.getText().toString());
        intent.putExtra("START_TIME", startTimeText.getText().toString());
        intent.putExtra("END_TIME", endTimeText.getText().toString());
        intent.putExtra("REMARKS", remarksInput.getText().toString());
        
        int itemsTotal = calculateTotalAmount();
        int serviceFee = 0; // Matching screenshot: PHP 0.00
        intent.putExtra("TOTAL_AMOUNT", (double)(itemsTotal + serviceFee));
        intent.putExtra("SERVICE_FEE", (double)serviceFee);
        
        // Payment Details
        String method = "COD";
        if (rbGCash.isChecked()) method = "GCash";
        else if (rbMaya.isChecked()) method = "Maya";
        else if (rbBank.isChecked()) method = "Bank Transfer";
        
        intent.putExtra("PAYMENT_METHOD", method);
        if (method.equals("Bank Transfer")) {
            intent.putExtra("BANK_NAME", bankSpinner.getSelectedItem().toString());
            intent.putExtra("BANK_REF", etBankRef.getText().toString());
            if (receiptUri != null) intent.putExtra("RECEIPT_URI", receiptUri.toString());
        }

        intent.putExtra("UNIT_MODEL", unitModel);
        intent.putExtra("LATITUDE", currentLat);
        intent.putExtra("LONGITUDE", currentLng);
        if (photoUri != null) { intent.putExtra("SELECTED_ID_URI", photoUri.toString()); intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); }
        intent.putExtra("QTY_CBC", currentQtyCBC); intent.putExtra("QTY_SEDIMENT", currentQtySEDIMENT); intent.putExtra("QTY_AQUATAL", currentQtyAquatal); intent.putExtra("QTY_INLINE", currentQtyInlineFilter); intent.putExtra("QTY_UV_LAMP", currentQtyUvLamp); intent.putExtra("QTY_TOUCH_PANEL", currentQtyTouchPanel); intent.putExtra("QTY_PBC_BOARD", currentQtyPbcBoard); intent.putExtra("QTY_SMSF_1_CBC", currentQtySmsf1Cbc); intent.putExtra("QTY_SMSF_10_SED", currentQtySmsf10Sed); intent.putExtra("QTY_WAY_VALVE", currentQtyWayValve);
        startActivity(intent); finish();
    }

    private int calculateTotalAmount() { return (currentQtyWayValve * PRICE_3WAY_VALVE) + (currentQtyCBC * PRICE_0064_CBC) + (currentQtySEDIMENT * PRICE_0055_SED) + (currentQtyAquatal * PRICE_AQUATAL) + (currentQtyInlineFilter * PRICE_INLINE) + (currentQtyUvLamp * PRICE_UV_LAMP) + (currentQtyTouchPanel * PRICE_TOUCH_PANEL) + (currentQtyPbcBoard * PRICE_PBC_BOARD) + (currentQtySmsf1Cbc * PRICE_SMSF_1_CBC) + (currentQtySmsf10Sed * PRICE_SMSF_10_SED); }

    private void fetchCustomerDetails() {
        String uid = mAuth.getUid(); if (uid == null) return;
        FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                if (s.exists()) {
                    tvCustomerNameValue.setText(s.child("fullName").getValue(String.class));
                    etCustomerNumber.setText(s.child("mobile").getValue(String.class));
                    String addr = s.child("address").getValue(String.class); etCustomerAddress.setText(addr);
                    originalProfileAddress = addr != null ? addr : "";
                    tvCustomerRefValue.setText(s.child("referenceNo").getValue(String.class));
                    if (s.hasChild("latitude")) currentLat = s.child("latitude").getValue(Double.class);
                    if (s.hasChild("longitude")) currentLng = s.child("longitude").getValue(Double.class);
                    if (unitModel == null || unitModel.isEmpty()) unitModel = s.child("unitModel").getValue(String.class);
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
        qtySMSF1uCBC2 = findViewById(R.id.qtySMSF1uCBC2); incrementSMSF1uCBC = findViewById(R.id.incrementSMSF1uCBC); decrementSMSF1uCBC1 = findViewById(R.id.decrementSMSF1uCBC1);
        qtySMSF10uSED2 = findViewById(R.id.qtySMSF10uSED2); incrementSMSF10uSED = findViewById(R.id.incrementSMSF10uSED); decrementSMSF10uSED_1 = findViewById(R.id.decrementSMSF10uSED_1);
        qtyWayvalve2 = findViewById(R.id.qtyWayvalve2); incrementWayvalve = findViewById(R.id.incrementWayvalve); decrementWayvalve1 = findViewById(R.id.decrementWayvalve1);

        incrementCBC.setOnClickListener(v -> { if (currentQtyCBC < 8) currentQtyCBC++; qtyCBC.setText(String.valueOf(currentQtyCBC)); refreshTotal(); });
        decrementCBC.setOnClickListener(v -> { if (currentQtyCBC > 0) currentQtyCBC--; qtyCBC.setText(String.valueOf(currentQtyCBC)); refreshTotal(); });
        incrementSEDIMENT.setOnClickListener(v -> { if (currentQtySEDIMENT < 8) currentQtySEDIMENT++; qtySEDIMENT.setText(String.valueOf(currentQtySEDIMENT)); refreshTotal(); });
        decrementSEDIMENT.setOnClickListener(v -> { if (currentQtySEDIMENT > 0) currentQtySEDIMENT--; qtySEDIMENT.setText(String.valueOf(currentQtySEDIMENT)); refreshTotal(); });
        incrementAquaTal.setOnClickListener(v -> { if (currentQtyAquatal < 8) currentQtyAquatal++; qtyAquatal.setText(String.valueOf(currentQtyAquatal)); refreshTotal(); });
        decrementAquatal.setOnClickListener(v -> { if (currentQtyAquatal > 0) currentQtyAquatal--; qtyAquatal.setText(String.valueOf(currentQtyAquatal)); refreshTotal(); });
        incrementInlineFilter.setOnClickListener(v -> { if (currentQtyInlineFilter < 8) currentQtyInlineFilter++; qtyInlineFilter.setText(String.valueOf(currentQtyInlineFilter)); refreshTotal(); });
        decrementInlineFilter.setOnClickListener(v -> { if (currentQtyInlineFilter > 0) currentQtyInlineFilter--; qtyInlineFilter.setText(String.valueOf(currentQtyInlineFilter)); refreshTotal(); });
        incrementUvLampLabel.setOnClickListener(v -> { if (currentQtyUvLamp < 8) currentQtyUvLamp++; qtyUvLampLabel.setText(String.valueOf(currentQtyUvLamp)); refreshTotal(); });
        decrementUvLampLabel.setOnClickListener(v -> { if (currentQtyUvLamp > 0) currentQtyUvLamp--; qtyUvLampLabel.setText(String.valueOf(currentQtyUvLamp)); refreshTotal(); });
        incrementTouchPanel.setOnClickListener(v -> { if (currentQtyTouchPanel < 8) currentQtyTouchPanel++; qtyTouchPanel.setText(String.valueOf(currentQtyTouchPanel)); refreshTotal(); });
        decrementTouchPanel.setOnClickListener(v -> { if (currentQtyTouchPanel > 0) currentQtyTouchPanel--; qtyTouchPanel.setText(String.valueOf(currentQtyTouchPanel)); refreshTotal(); });
        incrementPbcBoard.setOnClickListener(v -> { if (currentQtyPbcBoard < 8) currentQtyPbcBoard++; qtyPbcBoard.setText(String.valueOf(currentQtyPbcBoard)); refreshTotal(); });
        decrementPbcBoard.setOnClickListener(v -> { if (currentQtyPbcBoard > 0) currentQtyPbcBoard--; qtyPbcBoard.setText(String.valueOf(currentQtyPbcBoard)); refreshTotal(); });
        incrementSMSF1uCBC.setOnClickListener(v -> { if (currentQtySmsf1Cbc < 8) currentQtySmsf1Cbc++; qtySMSF1uCBC2.setText(String.valueOf(currentQtySmsf1Cbc)); refreshTotal(); });
        decrementSMSF1uCBC1.setOnClickListener(v -> { if (currentQtySmsf1Cbc > 0) currentQtySmsf1Cbc--; qtySMSF1uCBC2.setText(String.valueOf(currentQtySmsf1Cbc)); refreshTotal(); });
        incrementSMSF10uSED.setOnClickListener(v -> { if (currentQtySmsf10Sed < 8) currentQtySmsf10Sed++; qtySMSF10uSED2.setText(String.valueOf(currentQtySmsf10Sed)); refreshTotal(); });
        decrementSMSF10uSED_1.setOnClickListener(v -> { if (currentQtySmsf10Sed > 0) currentQtySmsf10Sed--; qtySMSF10uSED2.setText(String.valueOf(currentQtySmsf10Sed)); refreshTotal(); });
        incrementWayvalve.setOnClickListener(v -> { if (currentQtyWayValve < 8) currentQtyWayValve++; qtyWayvalve2.setText(String.valueOf(currentQtyWayValve)); refreshTotal(); });
        decrementWayvalve1.setOnClickListener(v -> { if (currentQtyWayValve > 0) currentQtyWayValve--; qtyWayvalve2.setText(String.valueOf(currentQtyWayValve)); refreshTotal(); });
    }

    private void refreshTotal() {
        int itemsTotal = calculateTotalAmount();
        int serviceFee = 0; // Matching screenshot: PHP 0.00
        int grandTotal = itemsTotal + serviceFee;
        
        String formattedFee = String.format(Locale.getDefault(), "PHP %,d.00", serviceFee);
        String formattedGrand = String.format(Locale.getDefault(), "PHP %,d.00", grandTotal);
        
        if (tvServiceFee != null) tvServiceFee.setText(formattedFee);
        if (tvPaymentTotal != null) tvPaymentTotal.setText(formattedGrand);
        if (tvBankAmount != null) tvBankAmount.setText(formattedGrand);
    }

    private void setupNavigation() {
        findViewById(R.id.buttonNext).setOnClickListener(v -> { currentStep = 2; updateStepUI(); });
        findViewById(R.id.buttonNext2).setOnClickListener(v -> {
            if (isOutsideOfficeHours(startTimeText.getText().toString()) || isOutsideOfficeHours(endTimeText.getText().toString())) { showTimeWarning(); return; }
            currentStep = 3; updateStepUI();
        });
        findViewById(R.id.buttonNext3).setOnClickListener(v -> {
            if (currentQtyCBC == 0 && currentQtySEDIMENT == 0 && currentQtyAquatal == 0 && currentQtyInlineFilter == 0 && currentQtyUvLamp == 0 && currentQtyTouchPanel == 0 && currentQtyPbcBoard == 0 && currentQtySmsf1Cbc == 0 && currentQtySmsf10Sed == 0) {
                new AlertDialog.Builder(this).setTitle("No Items").setMessage("Please select at least one item.").setPositiveButton("OK", null).show(); return;
            }
            currentStep = 4; updateStepUI();
        });
    }

    private void setActive(TextView tv, CardView bg, String num) { tv.setText(num); tv.setTextColor(Color.WHITE); bg.setCardBackgroundColor(Color.parseColor("#2196F3")); }
    private void setCompleted(TextView tv, CardView bg) { tv.setText("\u2713"); tv.setTextColor(Color.WHITE); bg.setCardBackgroundColor(Color.parseColor("#2196F3")); }
    private void resetCircle(TextView tv, CardView bg, String num) { tv.setText(num); tv.setTextColor(Color.parseColor("#94A3B8")); bg.setCardBackgroundColor(Color.parseColor("#E2E8F0")); }

    private void updateStepUI() {
        TextView tv1 = findViewById(R.id.circleText1), tv2 = findViewById(R.id.circleText2), tv3 = findViewById(R.id.circleText3), tv4 = findViewById(R.id.circleText4);
        CardView c1 = findViewById(R.id.circleNumber1), c2 = findViewById(R.id.circleNumber2), c3 = findViewById(R.id.circleNumber3), c4 = findViewById(R.id.circleNumber4);
        TextView lb1 = findViewById(R.id.label1), lb2 = findViewById(R.id.label2), lb3 = findViewById(R.id.label3), lb4 = findViewById(R.id.label4);
        View l1 = findViewById(R.id.line1), l2 = findViewById(R.id.line2), l3 = findViewById(R.id.line3);

        resetCircle(tv1, c1, "1"); resetCircle(tv2, c2, "2"); resetCircle(tv3, c3, "3"); resetCircle(tv4, c4, "4");
        lb1.setTextColor(Color.parseColor("#94A3B8")); lb2.setTextColor(Color.parseColor("#94A3B8")); lb3.setTextColor(Color.parseColor("#94A3B8")); lb4.setTextColor(Color.parseColor("#94A3B8"));
        l1.setBackgroundColor(Color.parseColor("#E2E8F0")); l2.setBackgroundColor(Color.parseColor("#E2E8F0")); l3.setBackgroundColor(Color.parseColor("#E2E8F0"));

        if (currentStep == 1) { setActive(tv1, c1, "1"); lb1.setTextColor(Color.parseColor("#2196F3")); }
        else if (currentStep == 2) { setCompleted(tv1, c1); setActive(tv2, c2, "2"); lb1.setTextColor(Color.parseColor("#2196F3")); lb2.setTextColor(Color.parseColor("#2196F3")); l1.setBackgroundColor(Color.parseColor("#2196F3")); }
        else if (currentStep == 3) { setCompleted(tv1, c1); setCompleted(tv2, c2); setActive(tv3, c3, "3"); lb1.setTextColor(Color.parseColor("#2196F3")); lb2.setTextColor(Color.parseColor("#2196F3")); lb3.setTextColor(Color.parseColor("#2196F3")); l1.setBackgroundColor(Color.parseColor("#2196F3")); l2.setBackgroundColor(Color.parseColor("#2196F3")); }
        else if (currentStep == 4) { setCompleted(tv1, c1); setCompleted(tv2, c2); setCompleted(tv3, c3); setActive(tv4, c4, "4"); lb1.setTextColor(Color.parseColor("#2196F3")); lb2.setTextColor(Color.parseColor("#2196F3")); lb3.setTextColor(Color.parseColor("#2196F3")); lb4.setTextColor(Color.parseColor("#2196F3")); l1.setBackgroundColor(Color.parseColor("#2196F3")); l2.setBackgroundColor(Color.parseColor("#2196F3")); l3.setBackgroundColor(Color.parseColor("#2196F3")); }

        int s1 = (currentStep == 1) ? View.VISIBLE : View.GONE;
        int s2 = (currentStep == 2) ? View.VISIBLE : View.GONE;
        int s3 = (currentStep == 3) ? View.VISIBLE : View.GONE;
        int s4 = (currentStep == 4) ? View.VISIBLE : View.GONE;

        // Display current step container
        findViewById(R.id.step1MainContainer).setVisibility(s1);
        findViewById(R.id.step2MainContainer).setVisibility(s2);
        findViewById(R.id.step3MainContainer).setVisibility(s3);
        findViewById(R.id.step4MainContainer).setVisibility(s4);

        // Manage Bottom Navigation Buttons
        findViewById(R.id.buttonNext).setVisibility(s1);
        findViewById(R.id.buttonNext2).setVisibility(s2);
        findViewById(R.id.buttonNext3).setVisibility(s3);
        findViewById(R.id.buttonSubmit).setVisibility(s4);

        if (currentStep == 4) refreshTotal();

        // Professional Back Button visibility (Show on steps 2, 3, 4)
        if (btnBackNav != null) {
            btnBackNav.setVisibility(currentStep > 1 ? View.VISIBLE : View.GONE);
        }
    }
}