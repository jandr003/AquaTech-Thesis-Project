package com.example.aquatech;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import com.bumptech.glide.Glide;

import java.io.File;

public class ServiceRequestActivity extends AppCompatActivity {

    private int currentStep = 1; 
    private boolean adminWillSchedule = false;
    private ImageView imgPreview, imgPlaceHolder, exitIcon;
    private TextView customerLabel, customerRefNum, customerValidIdLabel, tvCustomerNameValue, takePhotoOrUpload;
    private CardView customerCard, btnNeedHelp;
    private File photoFile;
    private Uri photoUri;

    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<PickVisualMediaRequest> galleryLauncher;
    private ActivityResultLauncher<Intent> fileLauncher;
    private ActivityResultLauncher<Intent> mapLauncher;

    private EditText etCustomerNumber, etCustomerAddress, remarksInput, etCustomerRef;
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

        String userSro = getIntent().getStringExtra("UNIT_SRO");
        if (userSro != null && etCustomerRef != null) etCustomerRef.setText(userSro);
        
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
        pd.setMessage("Verifying active service records...");
        pd.setCancelable(false);
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

        if (!adminWillSchedule) {
            validateAndColorTime();
            String sTime = startTimeText.getText().toString();
            String eTime = endTimeText.getText().toString();

            if (sTime.equals("---") || eTime.equals("---") || isOutsideOfficeHours(sTime) || isOutsideOfficeHours(eTime)) {
                showTimeWarning();
                return;
            }
        }

            if (inputAddress.isEmpty() || inputMobile.isEmpty()) {
            Toast.makeText(this, "Please ensure all contact details are complete.", Toast.LENGTH_SHORT).show();
            currentStep = 1;
            updateStepUI();
            return;
        }

        Toast.makeText(this, "Optimizing service coordinates...", Toast.LENGTH_SHORT).show();

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
        if (timeStr == null || timeStr.isEmpty()) return true;
        if (timeStr.equalsIgnoreCase("To be confirmed")) return false;
        if (timeStr.equals("---")) return true;

        Calendar cal = Calendar.getInstance();
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == Calendar.SUNDAY) return true;
        
        try {
            String[] parts = timeStr.split("[: ]");
            int hour = Integer.parseInt(parts[0]);
            int min = Integer.parseInt(parts[1]);
            String amPm = parts[2];

            int militaryHour = hour;
            if (amPm.equalsIgnoreCase("PM") && hour != 12) militaryHour += 12;
            if (amPm.equalsIgnoreCase("AM") && hour == 12) militaryHour = 0;

            if (militaryHour < 8 || militaryHour > 17) return true;
            if (militaryHour == 17 && min > 0) return true;
            
            return false;
        } catch (Exception e) { return true; }
    }

    private void validateAndColorTime() {
        if (adminWillSchedule) return;
        String sTime = startTimeText.getText().toString();
        String eTime = endTimeText.getText().toString();

        boolean sInvalid = isOutsideOfficeHours(sTime);
        boolean eInvalid = isOutsideOfficeHours(eTime);

        View sUnderline = findViewById(R.id.startTimeUnderline);
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
        } else {
            endTimeText.setTextColor(Color.parseColor("#333333"));
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
                .setTitle("Preferred Schedule Conflict")
                .setMessage("Our onsite services are strictly available from 8:00 AM to 5:00 PM, Monday to Saturday. Please select a time within these operating hours.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void initializeViews() {
        exitIcon = findViewById(R.id.ExitIcon);
        exitIcon.setOnClickListener(v -> finish());
        customerLabel = findViewById(R.id.customerLabel);
        tvCustomerNameValue = findViewById(R.id.tvCustomerNameValue);
        customerRefNum = findViewById(R.id.customerRefNum);
        etCustomerRef = findViewById(R.id.etCustomerRef);
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

        findViewById(R.id.EditPen1).setOnClickListener(v -> {
            etCustomerNumber.requestFocus(); InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(etCustomerNumber, 0);
        });

        findViewById(R.id.EditPen2).setOnClickListener(v -> {
            Intent intent = new Intent(this, MapPickerActivity.class);
            mapLauncher.launch(intent);
        });

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.purchase_type_options, android.R.layout.simple_spinner_item);
        purchaseTypeDropdown.setAdapter(adapter);

        findViewById(R.id.startTimeArrow).setOnClickListener(v -> {
            AquaTimePickerDialog.showStart(getSupportFragmentManager(), (startTime, endTime) -> {
                startTimeText.setText(startTime);
                endTimeText.setText(endTime);
                validateAndColorTime();
            });
        });

        View.OnClickListener paymentClickListener = v -> {
            rbGCash.setChecked(v.getId() == R.id.cardGCash);
            rbMaya.setChecked(v.getId() == R.id.cardMaya);
            rbBank.setChecked(v.getId() == R.id.cardBank);
            rbCOD.setChecked(v.getId() == R.id.cardCOD);

            int blue = Color.parseColor("#2196F3");
            int gray = Color.parseColor("#E2E8F0");

            if (v.getId() == R.id.cardGCash) {
                cardGCash.setStrokeColor(blue);
            } else {
                cardGCash.setStrokeColor(gray);
            }

            if (v.getId() == R.id.cardMaya) {
                cardMaya.setStrokeColor(blue);
            } else {
                cardMaya.setStrokeColor(gray);
            }

            if (v.getId() == R.id.cardBank) {
                cardBank.setStrokeColor(blue);
            } else {
                cardBank.setStrokeColor(gray);
            }

            if (v.getId() == R.id.cardCOD) {
                cardCOD.setStrokeColor(blue);
            } else {
                cardCOD.setStrokeColor(gray);
            }

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
        String[] options = {"Call Support", "Message Support", "FAQ Guide", "Our Office"};
        new AlertDialog.Builder(this).setTitle("Support Center").setItems(options, (dialog, which) -> {
            if (which == 0) { Intent i = new Intent(Intent.ACTION_DIAL); i.setData(Uri.parse("tel:09171234567")); startActivity(i); }
            else if (which == 1) Toast.makeText(this, "Support chat opening...", Toast.LENGTH_SHORT).show();
            else if (which == 2) showFAQs();
            else if (which == 3) { new AlertDialog.Builder(this).setTitle("Office").setMessage("GF, Makati City, 1209 Metro Manila").setPositiveButton("OK", null).show(); }
        }).setNegativeButton("Close", null).show();
    }

    private void setupLaunchers() {
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), result -> {
            if (result && photoUri != null) {
                if (isUploadingReceipt) {
                    showReceiptPreview(photoUri);
                } else {
                    showPreview(photoUri);
                }
            }
        });

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                if (isUploadingReceipt) {
                    receiptUri = uri;
                    showReceiptPreview(uri);
                } else {
                    photoUri = uri;
                    showPreview(uri);
                }
            }
        });

        fileLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) {
                    if (isUploadingReceipt) {
                        receiptUri = uri;
                        showReceiptPreview(uri);
                    } else {
                        photoUri = uri;
                        showPreview(uri);
                    }
                }
            }
        });

        mapLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                String addr = result.getData().getStringExtra("ADDRESS");
                if (addr != null) {
                    etCustomerAddress.setText(addr);
                }
                currentLat = result.getData().getDoubleExtra("LAT", 0);
                currentLng = result.getData().getDoubleExtra("LNG", 0);
            }
        });
    }

    private void showReceiptPreview(Uri uri) {
        imgReceiptPreview.setVisibility(View.VISIBLE);
        findViewById(R.id.receiptPlaceholder).setVisibility(View.GONE);
        Glide.with(this).load(uri).centerCrop().into(imgReceiptPreview);
    }

    private void showUploadDialog() {
        String[] options = {"Take a Photo (Camera)", "Choose from Gallery", "Select from Files"};
        new AlertDialog.Builder(this)
                .setTitle("Select Image Source")
                .setItems(options, (d, w) -> {
                    if (w == 0) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            openCamera();
                        } else {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
                        }
                    } else if (w == 1) {
                        galleryLauncher.launch(new PickVisualMediaRequest.Builder()
                                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                                .build());
                    } else {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("image/*");
                        fileLauncher.launch(intent);
                    }
                }).show();
    }

    private void openCamera() {
        try {
            String timeStamp = String.valueOf(System.currentTimeMillis());
            String fileName = "id_" + timeStamp + ".jpg";
            photoFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName);
            photoUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", photoFile);

            cameraLauncher.launch(photoUri);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to open camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showPreview(Uri uri) {
        if (imgPreview != null) {
            imgPreview.setVisibility(View.VISIBLE);
        }

        View placeholder = findViewById(R.id.idPlaceholder);
        if (placeholder != null) {
            placeholder.setVisibility(View.GONE);
        }

        Glide.with(this)
                .load(uri)
                .fitCenter()
                .into(imgPreview);
    }

    private void proceedToAnimation() {
        Intent intent = new Intent(this, WaterDropFillAnimationActivity.class);
        intent.putExtra("TICKET_ID", "ASC2026-" + String.format(Locale.getDefault(), "%04d", new Random().nextInt(9000) + 1));
        intent.putExtra("CUSTOMER_NAME", tvCustomerNameValue.getText().toString());
        intent.putExtra("CONTACT_NUMBER", etCustomerNumber.getText().toString());
        intent.putExtra("ADMIN_WILL_SCHEDULE", adminWillSchedule);
        intent.putExtra("ADDRESS", etCustomerAddress.getText().toString());
        intent.putExtra("REF_NO", etCustomerRef.getText().toString());
        intent.putExtra("DATE", displayDate.getText().toString());
        intent.putExtra("START_TIME", startTimeText.getText().toString());
        intent.putExtra("END_TIME", endTimeText.getText().toString());
        intent.putExtra("REMARKS", remarksInput.getText().toString());
        
        int itemsTotal = calculateTotalAmount();
        int serviceFee = 0;
        intent.putExtra("TOTAL_AMOUNT", (double)(itemsTotal + serviceFee));
        intent.putExtra("SERVICE_FEE", (double)serviceFee);

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

        if (photoUri != null) {
            intent.putExtra("SELECTED_ID_URI", photoUri.toString());
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
        
        startActivity(intent); finish();
    }

    private int calculateTotalAmount() {
        int total = 0;
        total += currentQtyWayValve * PRICE_3WAY_VALVE;
        total += currentQtyCBC * PRICE_0064_CBC;
        total += currentQtySEDIMENT * PRICE_0055_SED;
        total += currentQtyAquatal * PRICE_AQUATAL;
        total += currentQtyInlineFilter * PRICE_INLINE;
        total += currentQtyUvLamp * PRICE_UV_LAMP;
        total += currentQtyTouchPanel * PRICE_TOUCH_PANEL;
        total += currentQtyPbcBoard * PRICE_PBC_BOARD;
        total += currentQtySmsf1Cbc * PRICE_SMSF_1_CBC;
        total += currentQtySmsf10Sed * PRICE_SMSF_10_SED;
        return total;
    }

    private void fetchCustomerDetails() {
        String uid = mAuth.getUid(); if (uid == null) return;
        FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                if (s.exists()) {
                    tvCustomerNameValue.setText(s.child("fullName").getValue(String.class));
                    etCustomerNumber.setText(s.child("mobile").getValue(String.class));
                    String addr = s.child("address").getValue(String.class); etCustomerAddress.setText(addr);
                    originalProfileAddress = addr != null ? addr : "";
                    String refNo = s.child("referenceNo").getValue(String.class);
                    if (refNo != null && etCustomerRef != null) etCustomerRef.setText(refNo);
                    if (s.hasChild("latitude")) currentLat = s.child("latitude").getValue(Double.class);
                    if (s.hasChild("longitude")) currentLng = s.child("longitude").getValue(Double.class);
                    if (unitModel == null || unitModel.isEmpty()) unitModel = s.child("unitModel").getValue(String.class);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void setupQuantityHandlers() {
        qtyCBC = findViewById(R.id.qtyCBC);
        incrementCBC = findViewById(R.id.incrementCBC);
        decrementCBC = findViewById(R.id.decrementCBC);
        qtySEDIMENT = findViewById(R.id.qtySEDIMENT);
        incrementSEDIMENT = findViewById(R.id.incrementSEDIMENT);
        decrementSEDIMENT = findViewById(R.id.decrementSEDIMENT);
        qtyAquatal = findViewById(R.id.qtyAquatal);
        incrementAquaTal = findViewById(R.id.incrementAquaTal);
        decrementAquatal = findViewById(R.id.decrementAquatal);
        qtyInlineFilter = findViewById(R.id.qtyInlineFilter);
        incrementInlineFilter = findViewById(R.id.incrementInlineFilter);
        decrementInlineFilter = findViewById(R.id.decrementInlineFilter);
        qtyUvLampLabel = findViewById(R.id.qtyUvLampLabel);
        incrementUvLampLabel = findViewById(R.id.incrementUvLampLabel);
        decrementUvLampLabel = findViewById(R.id.decrementUvLampLabel);
        qtyTouchPanel = findViewById(R.id.qtyTouchPanel);
        incrementTouchPanel = findViewById(R.id.incrementTouchPanel);
        decrementTouchPanel = findViewById(R.id.decrementTouchPanel);
        qtyPbcBoard = findViewById(R.id.qtyPbcBoard);
        incrementPbcBoard = findViewById(R.id.incrementPbcBoard);
        decrementPbcBoard = findViewById(R.id.decrementPbcBoard);
        qtySMSF1uCBC2 = findViewById(R.id.qtySMSF1uCBC2);
        incrementSMSF1uCBC = findViewById(R.id.incrementSMSF1uCBC);
        decrementSMSF1uCBC1 = findViewById(R.id.decrementSMSF1uCBC1);
        qtySMSF10uSED2 = findViewById(R.id.qtySMSF10uSED2);
        incrementSMSF10uSED = findViewById(R.id.incrementSMSF10uSED);
        decrementSMSF10uSED_1 = findViewById(R.id.decrementSMSF10uSED_1);
        qtyWayvalve2 = findViewById(R.id.qtyWayvalve2);
        incrementWayvalve = findViewById(R.id.incrementWayvalve);
        decrementWayvalve1 = findViewById(R.id.decrementWayvalve1);

        bindQuantity(incrementWayvalve, decrementWayvalve1, qtyWayvalve2, "WAYVALVE");
        bindQuantity(incrementCBC, decrementCBC, qtyCBC, "CBC");
        bindQuantity(incrementSEDIMENT, decrementSEDIMENT, qtySEDIMENT, "SEDIMENT");
        bindQuantity(incrementAquaTal, decrementAquatal, qtyAquatal, "AQUATAL");
        bindQuantity(incrementInlineFilter, decrementInlineFilter, qtyInlineFilter, "INLINE");
        bindQuantity(incrementUvLampLabel, decrementUvLampLabel, qtyUvLampLabel, "UV");
        bindQuantity(incrementTouchPanel, decrementTouchPanel, qtyTouchPanel, "TOUCH");
        bindQuantity(incrementPbcBoard, decrementPbcBoard, qtyPbcBoard, "PBC");
        bindQuantity(incrementSMSF1uCBC, decrementSMSF1uCBC1, qtySMSF1uCBC2, "SMSF1");
        bindQuantity(incrementSMSF10uSED, decrementSMSF10uSED_1, qtySMSF10uSED2, "SMSF10");
    }

    private void bindQuantity(AppCompatButton inc, AppCompatButton dec, TextView display, String key) {
        inc.setOnClickListener(v -> updateQuantityValue(key, true, display));
        dec.setOnClickListener(v -> updateQuantityValue(key, false, display));
    }

    private void updateQuantityValue(String key, boolean increment, TextView display) {
        switch (key) {
            case "WAYVALVE":
                if (increment) {
                    if (currentQtyWayValve < 8) currentQtyWayValve++;
                } else {
                    if (currentQtyWayValve > 0) currentQtyWayValve--;
                }
                display.setText(String.valueOf(currentQtyWayValve));
                break;

            case "CBC":
                if (increment) {
                    if (currentQtyCBC < 8) currentQtyCBC++;
                } else {
                    if (currentQtyCBC > 0) currentQtyCBC--;
                }
                display.setText(String.valueOf(currentQtyCBC));
                break;

            case "SEDIMENT":
                if (increment) {
                    if (currentQtySEDIMENT < 8) currentQtySEDIMENT++;
                } else {
                    if (currentQtySEDIMENT > 0) currentQtySEDIMENT--;
                }
                display.setText(String.valueOf(currentQtySEDIMENT));
                break;

            case "AQUATAL":
                if (increment) {
                    if (currentQtyAquatal < 8) currentQtyAquatal++;
                } else {
                    if (currentQtyAquatal > 0) currentQtyAquatal--;
                }
                display.setText(String.valueOf(currentQtyAquatal));
                break;

            case "INLINE":
                if (increment) {
                    if (currentQtyInlineFilter < 8) currentQtyInlineFilter++;
                } else {
                    if (currentQtyInlineFilter > 0) currentQtyInlineFilter--;
                }
                display.setText(String.valueOf(currentQtyInlineFilter));
                break;

            case "UV":
                if (increment) {
                    if (currentQtyUvLamp < 8) currentQtyUvLamp++;
                } else {
                    if (currentQtyUvLamp > 0) currentQtyUvLamp--;
                }
                display.setText(String.valueOf(currentQtyUvLamp));
                break;

            case "TOUCH":
                if (increment) {
                    if (currentQtyTouchPanel < 8) currentQtyTouchPanel++;
                } else {
                    if (currentQtyTouchPanel > 0) currentQtyTouchPanel--;
                }
                display.setText(String.valueOf(currentQtyTouchPanel));
                break;

            case "PBC":
                if (increment) {
                    if (currentQtyPbcBoard < 8) currentQtyPbcBoard++;
                } else {
                    if (currentQtyPbcBoard > 0) currentQtyPbcBoard--;
                }
                display.setText(String.valueOf(currentQtyPbcBoard));
                break;

            case "SMSF1":
                if (increment) {
                    if (currentQtySmsf1Cbc < 8) currentQtySmsf1Cbc++;
                } else {
                    if (currentQtySmsf1Cbc > 0) currentQtySmsf1Cbc--;
                }
                display.setText(String.valueOf(currentQtySmsf1Cbc));
                break;

            case "SMSF10":
                if (increment) {
                    if (currentQtySmsf10Sed < 8) currentQtySmsf10Sed++;
                } else {
                    if (currentQtySmsf10Sed > 0) currentQtySmsf10Sed--;
                }
                display.setText(String.valueOf(currentQtySmsf10Sed));
                break;
        }
        refreshTotal();
    }

    private void refreshTotal() {
        int itemsTotal = calculateTotalAmount();
        int serviceFee = 0;
        int grandTotal = itemsTotal + serviceFee;
        String formattedFee = String.format(Locale.getDefault(), "PHP %,d.00", serviceFee);
        String formattedGrand = String.format(Locale.getDefault(), "PHP %,d.00", grandTotal);
        if (tvServiceFee != null) tvServiceFee.setText(formattedFee);
        if (tvPaymentTotal != null) tvPaymentTotal.setText(formattedGrand);
        if (tvBankAmount != null) tvBankAmount.setText(formattedGrand);
    }

    private void setupNavigation() {
        findViewById(R.id.buttonNext).setOnClickListener(v -> {
            String problem = remarksInput.getText().toString().trim();
            if (problem.isEmpty() || problem.length() < 5) {
                remarksInput.setError("Detailing your concern is required for a better service assessment.");
                Toast.makeText(this, "Please provide a brief description of the issue to proceed.", Toast.LENGTH_SHORT).show();
                remarksInput.requestFocus();
                return;
            }

            Calendar now = Calendar.getInstance();
            int hour = now.get(Calendar.HOUR_OF_DAY);
            int dayOfWeek = now.get(Calendar.DAY_OF_WEEK);

            adminWillSchedule = (dayOfWeek == Calendar.SUNDAY || hour >= 17 || hour < 8);

            if (adminWillSchedule) {
                startTimeText.setText("To be confirmed");
                endTimeText.setText("To be confirmed");
            }

            analyzeProblemAndSuggest(problem);
            currentStep = 2;
            updateStepUI();
        });
        findViewById(R.id.buttonNext2).setOnClickListener(v -> {
            if (adminWillSchedule) {
                currentStep = 3;
                updateStepUI();
                return;
            }

            String start = startTimeText.getText().toString();
            String end = endTimeText.getText().toString();

            if (isOutsideOfficeHours(start) || isOutsideOfficeHours(end)) {
                showTimeWarning();
                return;
            }

            currentStep = 3;
            updateStepUI();
        });

        findViewById(R.id.buttonNext3).setOnClickListener(v -> {
            boolean noItemsSelected = (currentQtyCBC == 0 && currentQtySEDIMENT == 0 &&
                    currentQtyAquatal == 0 && currentQtyInlineFilter == 0 &&
                    currentQtyUvLamp == 0 && currentQtyTouchPanel == 0 &&
                    currentQtyPbcBoard == 0 && currentQtySmsf1Cbc == 0 &&
                    currentQtySmsf10Sed == 0 && currentQtyWayValve == 0);

            if (noItemsSelected) {
                new AlertDialog.Builder(this)
                        .setTitle("No Items Selected")
                        .setMessage("Please select at least one item to proceed.")
                        .setPositiveButton("OK", null)
                        .show();
                return;
            }

            currentStep = 4;
            updateStepUI();
        });
    }
    
    private void analyzeProblemAndSuggest(String problem) {
        String p = problem.toLowerCase();
        currentQtyWayValve = 0;
        currentQtyCBC = 0;
        currentQtySEDIMENT = 0;
        currentQtyAquatal = 0;
        currentQtyInlineFilter = 0;
        currentQtyUvLamp = 0;
        currentQtyTouchPanel = 0;
        currentQtyPbcBoard = 0;
        currentQtySmsf1Cbc = 0;
        currentQtySmsf10Sed = 0;

        if (p.contains("leak") || p.contains("tulo") || p.contains("patak") || p.contains("valve") ||
                p.contains("pito") || p.contains("hose") || p.contains("adapter") || p.contains("connect")) {
            currentQtyWayValve = 1;
        }

        if (p.contains("taste") || p.contains("lasa") || p.contains("smell") || p.contains("amoy") ||
                p.contains("dirty") || p.contains("madumi") || p.contains("mabaho") || p.contains("maintenance") ||
                p.contains("change") || p.contains("filter") || p.contains("pait") || p.contains("kalawang") ||
                p.contains("dilaw") || p.contains("luma")) {
            currentQtyCBC = 1;
            currentQtySEDIMENT = 1;
        }

        if (p.contains("aquatal") || p.contains("alkaline") || p.contains("mineral") || p.contains("hataw")) {
            currentQtyAquatal = 1;
        }

        if (p.contains("inline") || p.contains("dagdag") || p.contains("pre-filter") || p.contains("sedimentation")) {
            currentQtyInlineFilter = 1;
        }

        if (p.contains("light") || p.contains("uv") || p.contains("lamp") || p.contains("bacteria") ||
                p.contains("germs") || p.contains("patay") || p.contains("ilaw")) {
            currentQtyUvLamp = 1;
        }

        if (p.contains("panel") || p.contains("display") || p.contains("touch") || p.contains("pindutan") ||
                p.contains("press") || p.contains("screen") || p.contains("pindot")) {
            currentQtyTouchPanel = 1;
        }

        if (p.contains("power") || p.contains("board") || p.contains("dead") || p.contains("bukas") ||
                p.contains("fuse") || p.contains("kuryente") || p.contains("short")) {
            currentQtyPbcBoard = 1;
        }

        if (p.contains("micron") || p.contains("smsf") || p.contains("slim") || p.contains("fine") ||
                p.contains("maselan") || p.contains("malabo") || p.contains("pino")) {
            currentQtySmsf1Cbc = 1;
            currentQtySmsf10Sed = 1;
        }

        if (qtyWayvalve2 != null) qtyWayvalve2.setText(String.valueOf(currentQtyWayValve));
        if (qtyCBC != null) qtyCBC.setText(String.valueOf(currentQtyCBC));
        if (qtySEDIMENT != null) qtySEDIMENT.setText(String.valueOf(currentQtySEDIMENT));
        if (qtyAquatal != null) qtyAquatal.setText(String.valueOf(currentQtyAquatal));
        if (qtyInlineFilter != null) qtyInlineFilter.setText(String.valueOf(currentQtyInlineFilter));
        if (qtyUvLampLabel != null) qtyUvLampLabel.setText(String.valueOf(currentQtyUvLamp));
        if (qtyTouchPanel != null) qtyTouchPanel.setText(String.valueOf(currentQtyTouchPanel));
        if (qtyPbcBoard != null) qtyPbcBoard.setText(String.valueOf(currentQtyPbcBoard));
        if (qtySMSF1uCBC2 != null) qtySMSF1uCBC2.setText(String.valueOf(currentQtySmsf1Cbc));
        if (qtySMSF10uSED2 != null) qtySMSF10uSED2.setText(String.valueOf(currentQtySmsf10Sed));
        refreshTotal();
    }

    private void setActive(TextView tv, CardView bg, String num) {
        tv.setText(num);
        tv.setTextColor(Color.WHITE);
        bg.setCardBackgroundColor(Color.parseColor("#2196F3"));
    }

    private void setCompleted(TextView tv, CardView bg) {
        tv.setText("\u2713");
        tv.setTextColor(Color.WHITE);
        bg.setCardBackgroundColor(Color.parseColor("#2196F3"));
    }

    private void resetCircle(TextView tv, CardView bg, String num) {
        tv.setText(num);
        tv.setTextColor(Color.parseColor("#94A3B8"));
        bg.setCardBackgroundColor(Color.parseColor("#E2E8F0"));
    }

    private void updateStepUI() {
        TextView tv1 = findViewById(R.id.circleText1);
        TextView tv2 = findViewById(R.id.circleText2);
        TextView tv3 = findViewById(R.id.circleText3);
        TextView tv4 = findViewById(R.id.circleText4);

        CardView c1 = findViewById(R.id.circleNumber1);
        CardView c2 = findViewById(R.id.circleNumber2);
        CardView c3 = findViewById(R.id.circleNumber3);
        CardView c4 = findViewById(R.id.circleNumber4);

        TextView lb1 = findViewById(R.id.label1);
        TextView lb2 = findViewById(R.id.label2);
        TextView lb3 = findViewById(R.id.label3);
        TextView lb4 = findViewById(R.id.label4);

        View l1 = findViewById(R.id.line1);
        View l2 = findViewById(R.id.line2);
        View l3 = findViewById(R.id.line3);

        resetCircle(tv1, c1, "1");
        resetCircle(tv2, c2, "2");
        resetCircle(tv3, c3, "3");
        resetCircle(tv4, c4, "4");

        lb1.setTextColor(Color.parseColor("#94A3B8"));
        lb2.setTextColor(Color.parseColor("#94A3B8"));
        lb3.setTextColor(Color.parseColor("#94A3B8"));
        lb4.setTextColor(Color.parseColor("#94A3B8"));

        l1.setBackgroundColor(Color.parseColor("#E2E8F0"));
        l2.setBackgroundColor(Color.parseColor("#E2E8F0"));
        l3.setBackgroundColor(Color.parseColor("#E2E8F0"));

        if (currentStep == 1) {
            setActive(tv1, c1, "1");
            lb1.setTextColor(Color.parseColor("#2196F3"));
        } else if (currentStep == 2) {
            setCompleted(tv1, c1);
            setActive(tv2, c2, "2");
            lb1.setTextColor(Color.parseColor("#2196F3"));
            lb2.setTextColor(Color.parseColor("#2196F3"));
            l1.setBackgroundColor(Color.parseColor("#2196F3"));
        } else if (currentStep == 3) {
            setCompleted(tv1, c1);
            setCompleted(tv2, c2);
            setActive(tv3, c3, "3");
            lb1.setTextColor(Color.parseColor("#2196F3"));
            lb2.setTextColor(Color.parseColor("#2196F3"));
            lb3.setTextColor(Color.parseColor("#2196F3"));
            l1.setBackgroundColor(Color.parseColor("#2196F3"));
            l2.setBackgroundColor(Color.parseColor("#2196F3"));
        } else if (currentStep == 4) {
            setCompleted(tv1, c1);
            setCompleted(tv2, c2);
            setCompleted(tv3, c3);
            setActive(tv4, c4, "4");
            lb1.setTextColor(Color.parseColor("#2196F3"));
            lb2.setTextColor(Color.parseColor("#2196F3"));
            lb3.setTextColor(Color.parseColor("#2196F3"));
            lb4.setTextColor(Color.parseColor("#2196F3"));
            l1.setBackgroundColor(Color.parseColor("#2196F3"));
            l2.setBackgroundColor(Color.parseColor("#2196F3"));
            l3.setBackgroundColor(Color.parseColor("#2196F3"));
        }

        int s1 = (currentStep == 1) ? View.VISIBLE : View.GONE;
        int s2 = (currentStep == 2) ? View.VISIBLE : View.GONE;
        int s3 = (currentStep == 3) ? View.VISIBLE : View.GONE;
        int s4 = (currentStep == 4) ? View.VISIBLE : View.GONE;

        findViewById(R.id.step1MainContainer).setVisibility(s1);
        findViewById(R.id.step2MainContainer).setVisibility(s2);
        findViewById(R.id.step3MainContainer).setVisibility(s3);
        findViewById(R.id.step4MainContainer).setVisibility(s4);

        findViewById(R.id.buttonNext).setVisibility(s1);
        findViewById(R.id.buttonNext2).setVisibility(s2);
        findViewById(R.id.buttonNext3).setVisibility(s3);
        findViewById(R.id.buttonSubmit).setVisibility(s4);

        if (currentStep == 4) refreshTotal();
        if (btnBackNav != null) btnBackNav.setVisibility(currentStep > 1 ? View.VISIBLE : View.GONE);
    }
}
