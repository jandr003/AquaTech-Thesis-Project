package com.example.aquatech;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
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

public class TrackServiceActivity extends AppCompatActivity {

    private MapView mMap;
    private TextView tvTechName, tvTechPhone, tvEta;
    private ImageView btnChat, btnCall, btnBack;
    private DatabaseReference requestsRef, usersRef;
    private String currentUid, assignedTechId, assignedTechName;

    private final GeoPoint COMPANY_LOC = new GeoPoint(14.558472, 121.032389);
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private Marker techMarker, customerMarker;
    private Polyline routeLine;
    private boolean hasFocusedOnCustomer = false;
    private long lastRouteRequestTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_track_service);

        currentUid = FirebaseAuth.getInstance().getUid();

        mMap = findViewById(R.id.map);
        tvTechName = findViewById(R.id.tvTechName);
        tvTechPhone = findViewById(R.id.tvTechPhone);
        tvEta = findViewById(R.id.tvEtaMins);
        btnChat = findViewById(R.id.btnChat1);
        btnCall = findViewById(R.id.btnCall1);
        btnBack = findViewById(R.id.btnBack);

        mMap.setTileSource(TileSourceFactory.MAPNIK);
        mMap.setMultiTouchControls(true);
        mMap.getController().setZoom(18.0);

        btnBack.setOnClickListener(v -> finish());

        setupClickListeners();
        usersRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users");
        setupFirebase();
    }

    private void setupClickListeners() {
        btnChat.setOnClickListener(v -> {
            if (assignedTechId != null) {
                Intent intent = new Intent(this, CustomerChatActivity.class);
                intent.putExtra("TECH_ID", assignedTechId);
                intent.putExtra("TECH_NAME", assignedTechName);
                startActivity(intent);
            } else {
                Toast.makeText(this, "No technician assigned yet", Toast.LENGTH_SHORT).show();
            }
        });

        btnCall.setOnClickListener(v -> {
            if (assignedTechId != null && currentUid != null) {
                String chatId = assignedTechId + "_" + currentUid;
                FirebaseDatabase.getInstance(DB_URL).getReference("UserChats")
                        .child(chatId).child("callStatus").setValue("calling");

                DatabaseReference techNotifRef = FirebaseDatabase.getInstance(DB_URL)
                        .getReference("Notifications").child(assignedTechId);

                String notifId = techNotifRef.push().getKey();
                if (notifId != null) {
                    HashMap<String, Object> callData = new HashMap<>();
                    callData.put("id", notifId);
                    callData.put("type", "CALL");
                    callData.put("message", "<b>Customer</b> is calling you...");
                    callData.put("ticketId", currentUid);
                    callData.put("timestamp", System.currentTimeMillis());

                    techNotifRef.child(notifId).setValue(callData).addOnSuccessListener(aVoid -> {
                        Intent intent = new Intent(this, CustomerVoiceCallActivity.class);
                        intent.putExtra("TECH_ID", assignedTechId);
                        intent.putExtra("CUSTOMER_ID", currentUid);
                        intent.putExtra("NAME", assignedTechName);
                        startActivity(intent);
                    });
                }
            } else {
                Toast.makeText(this, "No technician assigned yet", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupFirebase() {
        if (currentUid == null) return;
        requestsRef = FirebaseDatabase.getInstance(DB_URL).getReference("ServiceRequests");
        Query customerQuery = requestsRef.orderByChild("userId").equalTo(currentUid).limitToLast(1);

        customerQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        assignedTechId = ds.child("assignedTechId").getValue(String.class);
                        assignedTechName = ds.child("assignedTechName").getValue(String.class);

                        Double cLat = ds.child("latitude").getValue(Double.class);
                        Double cLng = ds.child("longitude").getValue(Double.class);
                        GeoPoint custLoc = (cLat != null && cLng != null) ? new GeoPoint(cLat, cLng) : null;

                        if (custLoc != null) {
                            if (!hasFocusedOnCustomer) {
                                mMap.getController().setCenter(custLoc);
                                hasFocusedOnCustomer = true;
                            }
                            updateCustomerMarker(custLoc);

                            if (assignedTechId == null || assignedTechId.isEmpty()) {
                                tvTechName.setText("Waiting for Technician...");
                                updateCommunicationUI(false);
                                if (tvEta != null) tvEta.setText("-- mins away");
                                updateTechAndRoute(COMPANY_LOC, "AquaTech Hub", custLoc);
                            } else {
                                tvTechName.setText(assignedTechName);
                                tvTechPhone.setText(ds.child("techPhone").getValue(String.class));
                                updateCommunicationUI(true);
                                trackLiveTech(assignedTechId, custLoc);
                            }
                        }
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateCommunicationUI(boolean enabled) {
        int color = enabled ? Color.parseColor("#37AAFD") : Color.GRAY;
        btnChat.setColorFilter(color);
        btnCall.setColorFilter(color);
        btnChat.setEnabled(enabled);
        btnCall.setEnabled(enabled);
    }

    private void trackLiveTech(String techId, GeoPoint custLoc) {
        usersRef.child(techId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                Double tLat = s.child("latitude").getValue(Double.class);
                Double tLng = s.child("longitude").getValue(Double.class);
                
                // Real-time location check
                if (tLat != null && tLng != null && tLat != 0) {
                    GeoPoint techLoc = new GeoPoint(tLat, tLng);
                    updateTechAndRoute(techLoc, "Technician", custLoc);
                } else {
                    updateTechAndRoute(COMPANY_LOC, "AquaTech Hub", custLoc);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void updateTechAndRoute(GeoPoint techLoc, String label, GeoPoint custLoc) {
        if (techMarker == null) {
            techMarker = new Marker(mMap);
            techMarker.setIcon(getResizedIcon(R.drawable.tech_gps_icon_png));
            techMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            mMap.getOverlays().add(techMarker);
        }
        techMarker.setPosition(techLoc);
        techMarker.setTitle(label);
        
        // Rate limiting: Update route and mins away at most every 3 seconds to save data/API
        long currentTime = System.currentTimeMillis();
        if (custLoc != null && (currentTime - lastRouteRequestTime > 3000)) {
            new FetchRouteTask(techLoc, custLoc).execute();
            lastRouteRequestTime = currentTime;
        }
        
        mMap.invalidate();
    }

    private void updateCustomerMarker(GeoPoint loc) {
        if (customerMarker == null) {
            customerMarker = new Marker(mMap);
            customerMarker.setIcon(getResizedIcon(R.drawable.technician_pin_point_customer_loc_png));
            customerMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mMap.getOverlays().add(customerMarker);
        }
        customerMarker.setPosition(loc);
    }

    private Drawable getResizedIcon(int resId) {
        Drawable drawable = ContextCompat.getDrawable(this, resId);
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        return new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap, 25, 25, true));
    }

    private class FetchRouteTask extends AsyncTask<Void, Void, JSONObject> {
        private GeoPoint start, end;
        public FetchRouteTask(GeoPoint start, GeoPoint end) { this.start = start; this.end = end; }

        @Override
        protected JSONObject doInBackground(Void... voids) {
            try {
                String urlStr = "https://router.project-osrm.org/route/v1/driving/" + start.getLongitude() + "," + start.getLatitude() + ";" + end.getLongitude() + "," + end.getLatitude() + "?overview=full&geometries=geojson";
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder(); String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                return new JSONObject(sb.toString());
            } catch (Exception e) { return null; }
        }

        @Override
        protected void onPostExecute(JSONObject json) {
            if (json == null || isFinishing()) return;
            try {
                JSONObject route = json.getJSONArray("routes").getJSONObject(0);

                // REALTIME ETA CALCULATION (Mins update here)
                double durationSeconds = route.getDouble("duration");
                int minutes = (int) Math.ceil(durationSeconds / 60.0);
                if (tvEta != null) {
                    if (minutes <= 1) tvEta.setText("Arriving soon");
                    else tvEta.setText(minutes + " mins away");
                }

                JSONArray coords = route.getJSONObject("geometry").getJSONArray("coordinates");
                List<GeoPoint> pts = new ArrayList<>();
                for (int i=0; i<coords.length(); i++) {
                    JSONArray c = coords.getJSONArray(i);
                    pts.add(new GeoPoint(c.getDouble(1), c.getDouble(0)));
                }

                if (routeLine != null) mMap.getOverlays().remove(routeLine);
                routeLine = new Polyline();
                routeLine.setPoints(pts);
                routeLine.setColor(Color.parseColor("#3775BB"));
                routeLine.setWidth(12f);
                mMap.getOverlays().add(0, routeLine);
                mMap.invalidate();

            } catch (Exception e) {
                Log.e("ROUTE_ERR", "Error parsing OSRM: " + e.getMessage());
            }
        }
    }

    @Override protected void onResume() { super.onResume(); mMap.onResume(); }
    @Override protected void onPause() { super.onPause(); mMap.onPause(); }
}
