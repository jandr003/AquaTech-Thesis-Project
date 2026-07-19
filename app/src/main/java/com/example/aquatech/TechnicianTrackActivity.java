package com.example.aquatech;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TechnicianTrackActivity extends AppCompatActivity {

    private MapView mMap;
    private TextView tvCustomerName, tvCustomerPhone;
    private ImageView btnBack, btnChat, btnCall, btnNextStep;
    private View btnZoomIn, btnZoomOut;

    private DatabaseReference requestsRef, usersRef, notifRef;
    private String ticketId, customerId, customerName, currentTechName, techUid;
    private ChildEventListener notifListener;
    private boolean isInitialNotifLoad = true;
    private boolean isActivityInForeground = false;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    // 📍 MAKATI COORDINATES (Anchor Point)
    private final GeoPoint COMPANY_LOC = new GeoPoint(14.5584827, 121.0323902);
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private Marker techMarker, customerMarker;
    private Polyline routeLine;
    private GeoPoint customerLoc;
    private boolean isRouteFetched = false;
    private ValueAnimator posAnimator;
    private long lastRouteFetchTime = 0;

    // Para sa mas accurate na rotation
    private GeoPoint lastPosition = null;
    private float currentBearing = 0f;

    // ✅ Flag para iwas multiple intents
    private boolean hasArrived = false;
    private ValueEventListener trackingListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_customer_track);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        initializeViews();

        // ✅ CHECK KUNG MAY NATANGGAP NA TICKET_ID
        ticketId = getIntent().getStringExtra("TICKET_ID");
        customerName = getIntent().getStringExtra("CUSTOMER_NAME");

        if (ticketId == null || ticketId.isEmpty()) {
            Toast.makeText(this, "ERROR: TechnicianTrackActivity received no TICKET_ID", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (customerName != null) tvCustomerName.setText(customerName);

        // Kunin ang technician UID
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            techUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "Technician not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        requestsRef = FirebaseDatabase.getInstance(DB_URL).getReference("ServiceRequests");
        usersRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users");

        setupMap();
        resolveTechName();       // Pupunuin ang currentTechName (display name)
        loadTrackingData();
        setupClickListeners();
        startLocationUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMap.onResume();
        isActivityInForeground = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMap.onPause();
        isActivityInForeground = false;
    }

    private void resolveTechName() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        if (email != null) {
            email = email.toLowerCase().trim();
            if (email.equals("admin@aquasmartguard.ph")) currentTechName = "John Andrew";
            else if (email.equals("management@aquasmartguard.ph")) currentTechName = "Glenn Jean";
            else if (email.equals("john1@aquasmartguard.ph")) currentTechName = "John";
            else if (email.equals("jhonny.s@aquasmartguard.ph")) currentTechName = "Jhonny Sinder";
            else if (email.equals("rarcilla@aquasmartguard.ph")) currentTechName = "Ricky Arcilla";
            else if (email.equals("r.trinidad@aquasmartguard.ph")) currentTechName = "Ruel Trinidad";
            else if (email.equals("gdelantar@aquasmartguard.ph")) currentTechName = "Glenda Delantar";
            else if (email.equals("duena@aquasmartguard.ph")) currentTechName = "Jerry Duena";
            else if (email.equals("ji@aquasmartguard.ph")) currentTechName = "Jonnifer Iglesias";
            else if (email.equals("marlon.salvador@aquasmartguard.ph")) currentTechName = "Marlon Salvador";
            else if (email.equals("smjr@aquasmartguard.ph")) currentTechName = "Selvino Magora Jr.";
            else currentTechName = "Technician";

            if (techUid != null) {
                setupNotificationMonitor(); // gumamit ng UID, hindi pangalan
            }
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
            return;
        }

        // ✅ MAS MABILIS NA LOCATION UPDATES (every 2 seconds)
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
                .setMinUpdateIntervalMillis(1000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    updateTechnicianPositionInFirebase(location);
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void updateTechnicianPositionInFirebase(Location loc) {
        if (loc == null) return;

        double lat = loc.getLatitude();
        double lng = loc.getLongitude();

        // 🛡️ PHILIPPINES BOUNDARY CHECK
        boolean isInPH = (lat > 4.0 && lat < 21.0 && lng > 116.0 && lng < 127.0);

        if (!isInPH) {
            Log.d("TRACK", "Outside PH detected. Staying at last valid location.");
            return;
        }

        GeoPoint newPos = new GeoPoint(lat, lng);

        // ✅ I-save ang last position para sa bearing calculation
        if (techMarker.getPosition() != null) {
            lastPosition = techMarker.getPosition();
        }

        animateMarkerSmoothly(techMarker, newPos);

        // Update Firebase – gamit ang techUid
        if (techUid != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("latitude", lat);
            updates.put("longitude", lng);
            usersRef.child(techUid).updateChildren(updates);
        }

        if (ticketId != null) {
            Map<String, Object> reqUpdates = new HashMap<>();
            reqUpdates.put("techLat", lat);
            reqUpdates.put("techLng", lng);
            requestsRef.child(ticketId).updateChildren(reqUpdates);
        }

        // Route update every 7 seconds
        long now = System.currentTimeMillis();
        if (customerLoc != null && (now - lastRouteFetchTime > 7000)) {
            new FetchRouteTask(newPos, customerLoc).execute();
            lastRouteFetchTime = now;
            isRouteFetched = true;
        }

        // ✅ I-center ang map sa technician para hindi mawala sa view
        mMap.getController().animateTo(newPos);

        // ✅ CHECK KUNG MALAPIT NA SA CUSTOMER
        checkProximityToCustomer(newPos);
    }

    // ✅ Bagong method para sa proximity check
    private void checkProximityToCustomer(GeoPoint techPos) {
        // Kung wala pang customer location o nakarating na, huwag nang mag-check
        if (customerLoc == null || hasArrived) return;

        // Kinukuha ang distansya sa pagitan ng technician at customer (sa meters)
        float[] results = new float[1];
        Location.distanceBetween(
                techPos.getLatitude(), techPos.getLongitude(),
                customerLoc.getLatitude(), customerLoc.getLongitude(),
                results
        );
        float distance = results[0];

        // ✅ Threshold: 20 meters (puwedeng baguhin)
        if (distance <= 20) {
            hasArrived = true; // Para hindi na ma-trigger ulit

            // I-update sa Firebase na nakarating na ang technician
            if (ticketId != null) {
                requestsRef.child(ticketId).child("status").setValue("Arrived");
            }

            // Ipakita ang ServiceStatusActivity
            runOnUiThread(() -> {
                Toast.makeText(this, "You have arrived at customer location!", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(TechnicianTrackActivity.this, ServiceStatusActivity.class);
                intent.putExtra("TICKET_ID", ticketId);
                startActivity(intent);
                finish(); // Optional: isara ang tracking activity
            });
        }
    }

    private void setupNotificationMonitor() {
        if (techUid == null) return;
        notifRef = FirebaseDatabase.getInstance(DB_URL).getReference("Notifications").child(techUid);
        notifListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (!isInitialNotifLoad) {
                    NotificationModel n = snapshot.getValue(NotificationModel.class);
                    if (n != null && n.getMessage() != null) {
                        if ("CALL".equalsIgnoreCase(n.getType())) {
                            if (isActivityInForeground) {
                                showIncomingCallDialog(n.getMessage(), n.getTicketId()); // ticketId dito ay customer UID
                            }
                            snapshot.getRef().removeValue();
                        } else if (isActivityInForeground) {
                            new AlertDialog.Builder(TechnicianTrackActivity.this)
                                    .setTitle("New Notification")
                                    .setMessage(Html.fromHtml(n.getMessage()))
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    }
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot s, @Nullable String p) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot s) {}
            @Override public void onChildMoved(@NonNull DataSnapshot s, @Nullable String p) {}
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        };
        notifRef.addChildEventListener(notifListener);
        notifRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) { isInitialNotifLoad = false; }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showIncomingCallDialog(String message, String callerCustId) {
        new AlertDialog.Builder(this)
                .setTitle("Incoming Call")
                .setMessage(Html.fromHtml(message))
                .setCancelable(false)
                .setPositiveButton("Answer", (dialog, which) -> {
                    String chatId = techUid + "_" + callerCustId;
                    FirebaseDatabase.getInstance(DB_URL).getReference("UserChats").child(chatId).child("callStatus").setValue("active");
                    FirebaseDatabase.getInstance(DB_URL).getReference("UserChats").child(chatId).child("callStartTime").setValue(ServerValue.TIMESTAMP);

                    Intent intent = new Intent(this, VoiceCallActivity.class);
                    intent.putExtra("TECH_ID", techUid);
                    intent.putExtra("CUSTOMER_ID", callerCustId);
                    intent.putExtra("NAME", customerName); // pangalan ng kausap (customer)
                    startActivity(intent);
                })
                .setNegativeButton("Decline", (dialog, which) -> {
                    String chatId = techUid + "_" + callerCustId;
                    FirebaseDatabase.getInstance(DB_URL).getReference("UserChats").child(chatId).child("callStatus").setValue("ended");
                })
                .show();
    }

    private void initializeViews() {
        tvCustomerName = findViewById(R.id.tvCustomerName);
        tvCustomerPhone = findViewById(R.id.tvCustomerPhone);
        btnBack = findViewById(R.id.btnBack);
        btnChat = findViewById(R.id.btnChat);
        btnCall = findViewById(R.id.btnCall);
        btnNextStep = findViewById(R.id.btnNextStep);
        mMap = findViewById(R.id.map);
        btnZoomIn = findViewById(R.id.btnZoomIn);
        btnZoomOut = findViewById(R.id.btnZoomOut);
    }

    private void setupMap() {
        mMap.setTileSource(TileSourceFactory.MAPNIK);
        mMap.setMultiTouchControls(true);
        mMap.getController().setZoom(18.0);
        mMap.getController().setCenter(COMPANY_LOC);

        techMarker = new Marker(mMap);
        techMarker.setPosition(COMPANY_LOC);
        techMarker.setTitle("Technician (You)");
        techMarker.setIcon(getResizedIcon(R.drawable.tech_gps_icon_png, 12));
        techMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        techMarker.setFlat(true);
        mMap.getOverlays().add(techMarker);

        routeLine = new Polyline();
        routeLine.setColor(Color.parseColor("#3775BB"));
        routeLine.setWidth(12f);
        mMap.getOverlays().add(routeLine);
    }

    private void loadTrackingData() {
        if (ticketId == null) return;

        trackingListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;
                if (routeLine == null || mMap == null) return;
                if (snapshot.exists()) {
                    customerId = snapshot.child("userId").getValue(String.class);
                    String assignedName = snapshot.child("assignedTechName").getValue(String.class);
                    if (assignedName != null) currentTechName = assignedName;

                    Double cLat = snapshot.child("latitude").getValue(Double.class);
                    Double cLng = snapshot.child("longitude").getValue(Double.class);
                    String phone = snapshot.child("customerPhone").getValue(String.class);

                    if (phone != null) tvCustomerPhone.setText(phone);

                    if (cLat != null && cLng != null && cLat != 0) {
                        customerLoc = new GeoPoint(cLat, cLng);
                        updateCustomerMarker(customerLoc);

                        if (!isRouteFetched || routeLine.getActualPoints() == null || routeLine.getActualPoints().isEmpty()) {
                            new FetchRouteTask(techMarker.getPosition(), customerLoc).execute();
                            isRouteFetched = true;
                            lastRouteFetchTime = System.currentTimeMillis();
                        }
                        checkProximityToCustomer(techMarker.getPosition());
                    }
                } else {
                    Toast.makeText(TechnicianTrackActivity.this, "Ticket not found in database", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        requestsRef.child(ticketId).addValueEventListener(trackingListener); // ← ISANG BESES LANG
    }
    private void animateMarkerSmoothly(final Marker marker, final GeoPoint toPosition) {
        if (marker.getPosition().equals(toPosition)) return;

        final GeoPoint startPosition = marker.getPosition();
        if (posAnimator != null) posAnimator.cancel();

        // ✅ MAS MABILIS NA ANIMATION (1 second lang)
        posAnimator = ValueAnimator.ofFloat(0, 1);
        posAnimator.setDuration(1000);
        posAnimator.setInterpolator(new LinearInterpolator());

        posAnimator.addUpdateListener(animation -> {
            float v = animation.getAnimatedFraction();
            double lat = v * toPosition.getLatitude() + (1 - v) * startPosition.getLatitude();
            double lng = v * toPosition.getLongitude() + (1 - v) * startPosition.getLongitude();
            GeoPoint currentPoint = new GeoPoint(lat, lng);

            // ✅ Mas accurate na bearing calculation gamit ang last two points
            if (lastPosition != null && v > 0.1f) {
                float newBearing = calculateBearing(lastPosition, currentPoint);
                // Smooth rotation - huwag masyadong biglaan
                currentBearing = currentBearing * 0.7f + newBearing * 0.3f;
                marker.setRotation(currentBearing);
            }

            marker.setPosition(currentPoint);

            // Prune route para mawala ang nadaanan na
            if (routeLine != null && routeLine.getActualPoints() != null && !routeLine.getActualPoints().isEmpty()) {
                pruneRoute(currentPoint);
            }

            mMap.invalidate();
        });

        posAnimator.start();
    }

    private void pruneRoute(GeoPoint currentPos) {
        if (routeLine == null) return;
        List<GeoPoint> points = routeLine.getActualPoints();
        if (points == null || points.size() < 2) return;


        // Remove points na malapit na sa current position
        while (points.size() > 1 && currentPos.distanceToAsDouble(points.get(0)) < 20) {
            points.remove(0);
        }

        // Update ang first point para mag-match sa current position
        if (!points.isEmpty()) {
            points.set(0, currentPos);
        }

        routeLine.setPoints(points);
    }

    private float calculateBearing(GeoPoint start, GeoPoint end) {
        double lat1 = Math.toRadians(start.getLatitude());
        double lon1 = Math.toRadians(start.getLongitude());
        double lat2 = Math.toRadians(end.getLatitude());
        double lon2 = Math.toRadians(end.getLongitude());

        double dLon = lon2 - lon1;
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);

        float bearing = (float) ((Math.toDegrees(Math.atan2(y, x)) + 360) % 360);
        return bearing;
    }

    private void updateCustomerMarker(GeoPoint loc) {
        if (customerMarker == null) {
            customerMarker = new Marker(mMap);
            customerMarker.setIcon(getResizedIcon(R.drawable.technician_pin_point_customer_loc_png, 12));
            customerMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mMap.getOverlays().add(customerMarker);
        }
        customerMarker.setPosition(loc);
        customerMarker.setTitle("Customer Location");
    }

    private Drawable getResizedIcon(int resId, int sizeDp) {
        Drawable d = ContextCompat.getDrawable(this, resId);
        if (d == null) return null;
        int px = (int) (sizeDp * getResources().getDisplayMetrics().density);
        if (d instanceof BitmapDrawable) {
            Bitmap b = ((BitmapDrawable) d).getBitmap();
            return new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(b, px, px, true));
        }
        d.setBounds(0, 0, px, px);
        return d;
    }

    private class FetchRouteTask extends AsyncTask<Void, Void, List<GeoPoint>> {
        private GeoPoint start, end;
        public FetchRouteTask(GeoPoint start, GeoPoint end) { this.start = start; this.end = end; }

        @Override protected List<GeoPoint> doInBackground(Void... voids) {
            List<GeoPoint> routePoints = new ArrayList<>();
            try {
                String urlStr = "https://router.project-osrm.org/route/v1/driving/"
                        + start.getLongitude() + "," + start.getLatitude() + ";"
                        + end.getLongitude() + "," + end.getLatitude()
                        + "?overview=full&geometries=geojson";
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder res = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) res.append(line);
                JSONObject json = new JSONObject(res.toString());
                JSONArray coords = json.getJSONArray("routes").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates");
                for (int i = 0; i < coords.length(); i++) {
                    JSONArray c = coords.getJSONArray(i);
                    routePoints.add(new GeoPoint(c.getDouble(1), c.getDouble(0)));
                }
            } catch (Exception e) { Log.e("ROUTE", "Task Error: " + e.getMessage()); }
            return routePoints;
        }

        @Override protected void onPostExecute(List<GeoPoint> points) {
            if (isFinishing() || isDestroyed()) return; // ✅
            if (points == null || points.isEmpty()) return; // ✅
            if (routeLine == null || mMap == null) return; // ✅
            routeLine.setPoints(points);
            mMap.invalidate();
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        if (btnZoomIn != null) btnZoomIn.setOnClickListener(v -> mMap.getController().zoomIn());
        if (btnZoomOut != null) btnZoomOut.setOnClickListener(v -> mMap.getController().zoomOut());

        // ✅ CHAT – gumamit ng UID
        btnChat.setOnClickListener(v -> {
            if (customerId == null) {
                Toast.makeText(this, "Customer data not loaded yet", Toast.LENGTH_SHORT).show();
                return;
            }
            if (techUid == null) {
                Toast.makeText(this, "Technician ID not found", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, TechChatActivity.class);
            intent.putExtra("CUSTOMER_ID", customerId);
            intent.putExtra("CUSTOMER_NAME", customerName);
            intent.putExtra("TECH_ID", techUid);
            intent.putExtra("TECH_NAME", currentTechName);
            startActivity(intent);
        });

        // ✅ CALL – gamit ang CallHelper
        btnCall.setOnClickListener(v -> {
            if (customerId == null) {
                Toast.makeText(this, "Customer data not loaded yet", Toast.LENGTH_SHORT).show();
                return;
            }
            if (techUid == null || currentTechName == null) {
                Toast.makeText(this, "Technician info not ready", Toast.LENGTH_SHORT).show();
                return;
            }

            CallHelper.initiateCall(
                    this,        // Activity context
                    techUid,            // Technician UID
                    customerId,         // Customer UID
                    customerName,       // Pangalan ng kausap (customer)
                    true                // true = technician ang tumatawag
            );
        });

        if (btnNextStep != null) {
            btnNextStep.setOnClickListener(v -> {
                Intent intent = new Intent(this, ServiceStatusActivity.class);
                intent.putExtra("TICKET_ID", ticketId);
                startActivity(intent);
            });
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        // ✅ Remove tracking listener
        if (requestsRef != null && trackingListener != null && ticketId != null) {
            requestsRef.child(ticketId).removeEventListener(trackingListener);
        }
        if (notifRef != null && notifListener != null) notifRef.removeEventListener(notifListener);
        if (locationCallback != null) fusedLocationClient.removeLocationUpdates(locationCallback);
        if (posAnimator != null) posAnimator.cancel();
        // ✅ Null out map objects
        routeLine = null;
        mMap = null;
    }
}