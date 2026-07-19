package com.example.aquatech;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TrackTechniciansActivity extends AppCompatActivity {

    private MapView mMap;
    private RecyclerView rvTrackTechs;
    private TrackTechAdapter adapter;
    private List<ServiceLogModel> trackList = new ArrayList<>();

    private DatabaseReference requestsRef, usersRef;
    private Map<String, ValueEventListener> techLocationListeners = new HashMap<>();
    private Map<String, Marker> techMarkers = new HashMap<>();
    private Map<String, Marker> customerMarkers = new HashMap<>();
    private Map<String, Polyline> trackingLines = new HashMap<>();
    private Map<String, Long> lastRouteFetchTimes = new HashMap<>();
    private Map<String, GeoPoint> lastTechPositions = new HashMap<>();
    private Map<String, Float> currentBearings = new HashMap<>();

    private final GeoPoint COMPANY_LOC = new GeoPoint(14.5584827, 121.0323902);
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private Set<String> currentTechIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_track_technicians);

        setupStatusBar();

        mMap = findViewById(R.id.mapTrack);
        rvTrackTechs = findViewById(R.id.rvTrackTechs);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        if (mMap == null) {
            Toast.makeText(this, "Map initialization failed", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupMap();
        setupRecyclerView();

        usersRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users");
        requestsRef = FirebaseDatabase.getInstance(DB_URL).getReference("ServiceRequests");
        loadActiveTickets();
    }

    private void setupMap() {
        try {
            mMap.setTileSource(TileSourceFactory.MAPNIK);
            mMap.setMultiTouchControls(true);
            mMap.getController().setZoom(15.0);
            mMap.getController().setCenter(COMPANY_LOC);
        } catch (Exception e) {
            Log.e("MAP", "Error setting up map", e);
        }
    }

    private void setupRecyclerView() {
        rvTrackTechs.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TrackTechAdapter(this, trackList, new TrackTechAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ServiceLogModel model) {
                // Optional: Open detailed request view if needed
            }

            @Override
            public void onTrackClick(ServiceLogModel model) {
                //REAL-TIME TRACKING ON MAP
                if (model == null || model.getTechId() == null) return;
                
                Marker marker = techMarkers.get(model.getTechId());
                if (marker != null) {
                    GeoPoint techPos = marker.getPosition();
                    mMap.getController().animateTo(techPos);
                    mMap.getController().setZoom(18.0);
                    marker.showInfoWindow();
                    Toast.makeText(TrackTechniciansActivity.this, "Tracking " + model.getTechName(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(TrackTechniciansActivity.this, "Technician location not available yet", Toast.LENGTH_SHORT).show();
                }
            }
        });
        rvTrackTechs.setAdapter(adapter);
    }

    private void loadActiveTickets() {
        requestsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    trackList.clear();
                    Set<String> newTechIds = new HashSet<>();

                    for (DataSnapshot ds : snapshot.getChildren()) {
                        try {
                            String status = ds.child("status").getValue(String.class);
                            String assignedTechName = ds.child("assignedTechName").getValue(String.class);
                            String techId = ds.child("assignedTechId").getValue(String.class);
                            String ticketId = ds.child("ticketId").getValue(String.class);
                            String sro = ds.child("sroNumber").getValue(String.class);
                            String custName = ds.child("customerName").getValue(String.class);
                            String address = ds.child("address").getValue(String.class);
                            String timeRange = ds.child("timeRange").getValue(String.class);
                            Double custLat = ds.child("latitude").getValue(Double.class);
                            Double custLng = ds.child("longitude").getValue(Double.class);

                            if (techId != null && !techId.isEmpty() &&
                                    (("In Progress".equalsIgnoreCase(status)) ||
                                            ("Open".equalsIgnoreCase(status) && assignedTechName != null))) {

                                String key = ticketId != null ? ticketId : ds.getKey();
                                newTechIds.add(techId);

                                if (custLat != null && custLng != null && custLat != 0 && mMap != null) {
                                    GeoPoint custLoc = new GeoPoint(custLat, custLng);
                                    updateCustomerMarker(key, custLoc, custName);
                                }

                                GeoPoint custLoc = (custLat != null && custLng != null && custLat != 0) ?
                                        new GeoPoint(custLat, custLng) : null;
                                trackTechnicianLocation(techId, assignedTechName, custLoc, status, key);

                                ServiceLogModel model = new ServiceLogModel();
                                model.setSroNumber(sro != null ? sro : key);
                                model.setTechName(assignedTechName);
                                model.setCustomerName(custName);
                                model.setTechRole(getServiceTypeFromSnapshot(ds));
                                model.setDateTime(timeRange);
                                model.setAddress(address);
                                model.setStatus(status);
                                model.setTechId(techId);
                                if (custLat != null) model.setLatitude(custLat);
                                if (custLng != null) model.setLongitude(custLng);

                                trackList.add(model);
                            }
                        } catch (Exception e) {
                            Log.e("TICKET_PROCESS", "Error processing ticket", e);
                        }
                    }

                    removeInactiveTechs(newTechIds);
                    currentTechIds = newTechIds;

                    adapter.notifyDataSetChanged();

                } catch (Exception e) {
                    Log.e("LOAD_TICKETS", "Fatal error loading tickets", e);
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void trackTechnicianLocation(String techId, String techName, GeoPoint custLoc, String status, String ticketKey) {
        if (techId == null || techId.isEmpty()) return;
        if (techLocationListeners.containsKey(techId)) return;

        ValueEventListener locationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    Double tLat = snapshot.child("latitude").getValue(Double.class);
                    Double tLng = snapshot.child("longitude").getValue(Double.class);
                    GeoPoint techLoc = (tLat != null && tLng != null && tLat != 0.0) ?
                            new GeoPoint(tLat, tLng) : COMPANY_LOC;

                    updateTechMarker(techId, techLoc, techName);

                    // Sync for live data binding in the list
                    for (ServiceLogModel m : trackList) {
                        if (techId.equals(m.getTechId())) {
                            m.setTechLat(techLoc.getLatitude());
                            m.setTechLng(techLoc.getLongitude());
                            break;
                        }
                    }

                    if ("In Progress".equalsIgnoreCase(status) && custLoc != null) {
                        long now = System.currentTimeMillis();
                        Long lastFetch = lastRouteFetchTimes.get(techId);
                        if (lastFetch == null || now - lastFetch > 7000) {
                            new FetchRouteTask(techId, techLoc, custLoc).execute();
                            lastRouteFetchTimes.put(techId, now);
                        }
                    } else if (trackingLines.containsKey(techId)) {
                        mMap.getOverlays().remove(trackingLines.get(techId));
                        trackingLines.remove(techId);
                        mMap.invalidate();
                    }
                } catch (Exception e) {
                    Log.e("TRACK_LOC", "Error tracking location", e);
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };

        usersRef.child(techId).addValueEventListener(locationListener);
        techLocationListeners.put(techId, locationListener);
    }

    private void updateTechMarker(String techId, GeoPoint newPos, String techName) {
        if (mMap == null) return;

        try {
            Marker marker = techMarkers.get(techId);
            if (marker == null) {
                marker = new Marker(mMap);
                marker.setPosition(newPos);
                marker.setTitle("Tech: " + techName);
                Drawable icon = ContextCompat.getDrawable(this, R.drawable.tech_gps_icon_png);
                if (icon instanceof BitmapDrawable) {
                    Bitmap bitmap = ((BitmapDrawable) icon).getBitmap();
                    int size = (int) (12 * getResources().getDisplayMetrics().density);
                    marker.setIcon(new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap, size, size, true)));
                }
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                marker.setFlat(true);
                mMap.getOverlays().add(marker);
                techMarkers.put(techId, marker);
                lastTechPositions.put(techId, newPos);
                currentBearings.put(techId, 0f);
            } else {
                animateMarkerWithBearing(techId, marker, newPos);
            }
        } catch (Exception e) {}
    }

    private void animateMarkerWithBearing(final String techId, final Marker marker, final GeoPoint toPosition) {
        if (marker == null || toPosition == null) return;
        final GeoPoint startPosition = marker.getPosition();
        if (startPosition.equals(toPosition)) return;

        try {
            float bearing = calculateBearing(startPosition, toPosition);
            ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
            animator.setDuration(1000);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(animation -> {
                try {
                    float v = animation.getAnimatedFraction();
                    double lat = v * toPosition.getLatitude() + (1 - v) * startPosition.getLatitude();
                    double lng = v * toPosition.getLongitude() + (1 - v) * startPosition.getLongitude();
                    GeoPoint currentPos = new GeoPoint(lat, lng);
                    
                    marker.setRotation(bearing);
                    marker.setPosition(currentPos);

                    Polyline line = trackingLines.get(techId);
                    if (line != null) {
                        pruneRoute(line, currentPos);
                    }

                    mMap.invalidate();
                } catch (Exception e) {}
            });
            animator.start();
            lastTechPositions.put(techId, toPosition);
        } catch (Exception e) {}
    }

    private float calculateBearing(GeoPoint start, GeoPoint end) {
        double lat1 = Math.toRadians(start.getLatitude());
        double lon1 = Math.toRadians(start.getLongitude());
        double lat2 = Math.toRadians(end.getLatitude());
        double lon2 = Math.toRadians(end.getLongitude());
        double dLon = lon2 - lon1;
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        return (float) ((Math.toDegrees(Math.atan2(y, x)) + 360) % 360);
    }

    private void pruneRoute(Polyline line, GeoPoint currentPos) {
        try {
            List<GeoPoint> points = line.getActualPoints();
            if (points.size() < 2) return;

            while (points.size() > 1 && currentPos.distanceToAsDouble(points.get(0)) < 20) {
                points.remove(0);
            }
            if (!points.isEmpty()) {
                points.set(0, currentPos);
            }
            line.setPoints(points);
        } catch (Exception e) {}
    }

    private void updateCustomerMarker(String ticketKey, GeoPoint loc, String custName) {
        if (mMap == null) return;
        try {
            Marker marker = customerMarkers.get(ticketKey);
            if (marker == null) {
                marker = new Marker(mMap);
                marker.setPosition(loc);
                marker.setTitle("Customer: " + custName);
                Drawable custIcon = ContextCompat.getDrawable(this, R.drawable.technician_pin_point_customer_loc_png);
                if (custIcon instanceof BitmapDrawable) {
                    Bitmap bitmap = ((BitmapDrawable) custIcon).getBitmap();
                    int size = (int) (12 * getResources().getDisplayMetrics().density);
                    marker.setIcon(new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap, size, size, true)));
                }
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                mMap.getOverlays().add(marker);
                customerMarkers.put(ticketKey, marker);
            } else {
                marker.setPosition(loc);
            }
        } catch (Exception e) {}
    }

    private void removeInactiveTechs(Set<String> activeTechIds) {
        List<String> toRemove = new ArrayList<>();
        for (String techId : techMarkers.keySet()) {
            if (!activeTechIds.contains(techId)) toRemove.add(techId);
        }
        for (String techId : toRemove) {
            Marker m = techMarkers.remove(techId);
            if (m != null) mMap.getOverlays().remove(m);
            ValueEventListener listener = techLocationListeners.remove(techId);
            if (listener != null) usersRef.child(techId).removeEventListener(listener);
            Polyline line = trackingLines.remove(techId);
            if (line != null) mMap.getOverlays().remove(line);
        }
    }

    private class FetchRouteTask extends AsyncTask<Void, Void, List<GeoPoint>> {
        private String techId;
        private GeoPoint start, end;
        public FetchRouteTask(String techId, GeoPoint start, GeoPoint end) { this.techId = techId; this.start = start; this.end = end; }
        @Override protected List<GeoPoint> doInBackground(Void... voids) {
            List<GeoPoint> routePoints = new ArrayList<>();
            try {
                String urlStr = "https://router.project-osrm.org/route/v1/driving/" + start.getLongitude() + "," + start.getLatitude() + ";" + end.getLongitude() + "," + end.getLatitude() + "?overview=full&geometries=geojson";
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder(); String line;
                while ((line = reader.readLine()) != null) response.append(line);
                JSONObject json = new JSONObject(response.toString());
                JSONArray routes = json.getJSONArray("routes");
                if (routes.length() > 0) {
                    JSONArray coordinates = routes.getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates");
                    for (int i = 0; i < coordinates.length(); i++) {
                        JSONArray coord = coordinates.getJSONArray(i);
                        routePoints.add(new GeoPoint(coord.getDouble(1), coord.getDouble(0)));
                    }
                }
            } catch (Exception e) {}
            return routePoints;
        }
        @Override protected void onPostExecute(List<GeoPoint> points) {
            if (points.isEmpty() || mMap == null) return;
            Polyline line = trackingLines.get(techId);
            if (line == null) {
                line = new Polyline(); line.setColor(Color.parseColor("#3775BB")); line.setWidth(10f);
                mMap.getOverlays().add(0, line); trackingLines.put(techId, line);
            }
            line.setPoints(points); mMap.invalidate();
        }
    }

    private String getServiceTypeFromSnapshot(DataSnapshot ds) {
        List<String> categories = new ArrayList<>();
        if (hasQty(ds, "qty_wayvalve")) categories.add("Installation Kit");
        if (hasQty(ds, "qty_aquatal") || hasQty(ds, "qty_uvlamp") || hasQty(ds, "qty_touchpanel") || hasQty(ds, "qty_pbcboard")) categories.add("Other Parts");
        if (hasQty(ds, "qty_cbc") || hasQty(ds, "qty_sediment") || hasQty(ds, "qty_inline") || hasQty(ds, "qty_smsf1") || hasQty(ds, "qty_smsf10")) categories.add("Filter Preventive");
        if (categories.isEmpty()) return "General Service";
        if (categories.size() == 1) return categories.get(0);
        return categories.get(0) + " & others";
    }

    private boolean hasQty(DataSnapshot ds, String key) {
        Object val = ds.child(key).getValue();
        return val instanceof Number && ((Number) val).intValue() > 0;
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        for (Map.Entry<String, ValueEventListener> entry : techLocationListeners.entrySet()) {
            usersRef.child(entry.getKey()).removeEventListener(entry.getValue());
        }
    }
}
