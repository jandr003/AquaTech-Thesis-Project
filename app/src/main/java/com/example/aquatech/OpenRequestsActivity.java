package com.example.aquatech;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OpenRequestsActivity extends AppCompatActivity {

    private MapView mMap;
    private RecyclerView rvOpenRequests;
    private DatabaseReference dbRef;
    private List<ServiceLogModel> openList = new ArrayList<>();
    private OpenRequestAdapter adapter;
    private TextView tvNoRequests;
    private boolean isFirstLoad = true;



    private Map<String, Marker> activeMarkers = new HashMap<>();
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private ValueEventListener requestsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_open_requests);

        setupStatusBar();
        requestPermissionsIfNecessary();

        mMap = findViewById(R.id.mapOpen);
        mMap.setTileSource(TileSourceFactory.MAPNIK);
        mMap.setMultiTouchControls(true);
        mMap.getController().setZoom(14.0);

        rvOpenRequests = findViewById(R.id.rvOpenRequests);
        tvNoRequests = findViewById(R.id.tvNoRequests);
        findViewById(R.id.btnBackOpen).setOnClickListener(v -> finish());

        rvOpenRequests.setLayoutManager(new LinearLayoutManager(this));

        adapter = new OpenRequestAdapter(openList, this::showTechnicianDialog, ticket -> {
            if (ticket.getLatitude() != 0.0 && ticket.getLongitude() != 0.0) {
                GeoPoint target = new GeoPoint(ticket.getLatitude(), ticket.getLongitude());
                mMap.getController().animateTo(target);
                mMap.getController().setZoom(18.0);
                Marker m = activeMarkers.get(ticket.getTicketId());
                if (m != null) m.showInfoWindow();
            }
        });

        rvOpenRequests.setAdapter(adapter);
        setupRealtimeTracking();
    }

    private void setupRealtimeTracking() {
        dbRef = FirebaseDatabase.getInstance(DB_URL).getReference("ServiceRequests");
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;
                openList.clear();
                Set<String> currentIds = new HashSet<>();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String status = ds.child("status").getValue(String.class);
                    String assignedName = ds.child("assignedTechName").getValue(String.class);

                    if ("Open".equalsIgnoreCase(status) && (assignedName == null || assignedName.isEmpty())) {
                        double lat = 0.0, lng = 0.0;
                        try {
                            Object latObj = ds.child("latitude").getValue();
                            Object lngObj = ds.child("longitude").getValue();
                            if (latObj instanceof Number) lat = ((Number) latObj).doubleValue();
                            if (lngObj instanceof Number) lng = ((Number) lngObj).doubleValue();
                        } catch (Exception e) {}

                        String ticketId = ds.child("ticketId").getValue(String.class);
                        String sro = ds.child("sroNumber").getValue(String.class);
                        if (sro == null) sro = ds.child("referenceNo").getValue(String.class);

                        String customer = ds.child("customerName").getValue(String.class);
                        String address = ds.child("address").getValue(String.class);
                        String mobile = ds.child("contactNumber").getValue(String.class);
                        String timeRange = ds.child("timeRange").getValue(String.class);

                        String id = (ticketId != null && !ticketId.isEmpty()) ? ticketId : ds.getKey();
                        currentIds.add(id);

                        List<String> categories = new ArrayList<>();
                        if (hasQty(ds, "qty_wayvalve")) categories.add("Installation Kit");
                        if (hasQty(ds, "qty_aquatal") || hasQty(ds, "qty_uvlamp") || hasQty(ds, "qty_touchpanel") || hasQty(ds, "qty_pbcboard")) categories.add("Other Parts");
                        if (hasQty(ds, "qty_cbc") || hasQty(ds, "qty_sediment") || hasQty(ds, "qty_inline") || hasQty(ds, "qty_smsf1") || hasQty(ds, "qty_smsf10")) categories.add("Filter Preventive");

                        String finalServiceType = "";
                        if (categories.size() == 1) {
                            finalServiceType = categories.get(0);
                        } else if (categories.size() == 2) {
                            finalServiceType = categories.get(0) + " & " + categories.get(1);
                        } else if (categories.size() >= 3) {
                            finalServiceType = categories.get(0) + ", " + categories.get(1) + " and " + categories.get(2);
                        } else {
                            finalServiceType = ds.child("serviceType").getValue(String.class);
                            if (finalServiceType == null) finalServiceType = "General Service";
                        }

                        ServiceLogModel model = new ServiceLogModel();
                        model.setTicketId(id);
                        model.setSroNumber(sro != null ? sro : "N/A");
                        model.setTechName(customer != null ? customer : "Unknown");
                        model.setTechRole(finalServiceType);
                        model.setDateTime(timeRange != null ? timeRange : "Not Set");
                        model.setAddress(address != null ? address : "N/A");
                        model.setCustomerPhone(mobile != null ? mobile : "N/A");
                        model.setLatitude(lat);
                        model.setLongitude(lng);

                        openList.add(model);

                        if (lat != 0.0) {
                            updateMarkerOnMap(id, new GeoPoint(lat, lng), customer);
                        }
                    }
                }

                List<String> toRemove = new ArrayList<>();
                for (String markerId : activeMarkers.keySet()) {
                    if (!currentIds.contains(markerId)) toRemove.add(markerId);
                }
                for (String id : toRemove) {
                    Marker m = activeMarkers.get(id);
                    if (m != null) mMap.getOverlays().remove(m);
                    activeMarkers.remove(id);
                }

                adapter.notifyDataSetChanged();
                if (tvNoRequests != null) tvNoRequests.setVisibility(openList.isEmpty() ? View.VISIBLE : View.GONE);
                mMap.invalidate();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private boolean hasQty(DataSnapshot ds, String key) {
        Object val = ds.child(key).getValue();
        return val instanceof Number && ((Number) val).intValue() > 0;
    }

    private void updateMarkerOnMap(String id, GeoPoint pos, String name) {
        if (activeMarkers.containsKey(id)) {
            activeMarkers.get(id).setPosition(pos);
        } else {
            Marker marker = new Marker(mMap);
            marker.setPosition(pos);
            marker.setTitle(name);
            Drawable icon = ContextCompat.getDrawable(this, R.drawable.technician_pin_point_customer_loc_png);
            if (icon != null && icon instanceof BitmapDrawable) {
                Bitmap bitmap = ((BitmapDrawable) icon).getBitmap();
                int size = (int) (12 * getResources().getDisplayMetrics().density);
                marker.setIcon(new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap, size, size, true)));
            }
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mMap.getOverlays().add(marker);
            activeMarkers.put(id, marker);
        }
    }

    // ✅ FIXED: Technician dialog with full null safety
    private void showTechnicianDialog(ServiceLogModel ticket) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_technician_list);

        // 🛡️ Prevent dialog from closing when touching outside
        dialog.setCanceledOnTouchOutside(false);
        // Also prevent back button from closing accidentally (optional)
        dialog.setCancelable(true); // Back button still works, but you can set false if needed

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        RecyclerView rvTechs = dialog.findViewById(R.id.rvDialogTechs);
        Button btnClose = dialog.findViewById(R.id.btnCloseDialog);
        List<Map<String, String>> techList = new ArrayList<>();

        // ✅ Updated adapter with safe click listener
        DialogTechAdapter techAdapter = new DialogTechAdapter(techList, (name, uid) -> {
            // name is guaranteed non-null by adapter
            assignTechnicianToTicket(ticket, name, uid);
            dialog.dismiss();
        });

        rvTechs.setAdapter(techAdapter);
        rvTechs.setLayoutManager(new LinearLayoutManager(this));

        String[] techNamesArr = getResources().getStringArray(R.array.technician_names);

        FirebaseDatabase.getInstance(DB_URL).getReference("Users")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        techList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String role = ds.child("role").getValue(String.class);
                            if ("Technician".equalsIgnoreCase(role)) {
                                String name = ds.child("fullName").getValue(String.class);
                                String tId = ds.child("techId").getValue(String.class);
                                String workStatus = ds.child("workStatus").getValue(String.class); // ✅ fetch status

                                if (name != null && !name.isEmpty()) {
                                    Map<String, String> techMap = new HashMap<>();
                                    techMap.put("name", name);
                                    techMap.put("uid", ds.getKey());
                                    techMap.put("techId", tId != null ? tId : "ASC-T00");
                                    techMap.put("status", workStatus != null ? workStatus : "Available"); // ✅ store status
                                    techList.add(techMap);
                                }
                            }
                        }

                        // Add fallback technicians (with default status "Available")
                        for (int i = 0; i < techNamesArr.length; i++) {
                            String sName = techNamesArr[i];
                            boolean exists = false;
                            for (Map<String, String> m : techList) {
                                if (m.get("name").equalsIgnoreCase(sName)) {
                                    exists = true;
                                    break;
                                }
                            }
                            if (!exists) {
                                Map<String, String> techMap = new HashMap<>();
                                techMap.put("name", sName);
                                techMap.put("uid", sName);
                                techMap.put("techId", "ASC-T00" + (i + 1));
                                techMap.put("status", "Available"); // ✅ default status
                                techList.add(techMap);
                            }
                        }
                        techAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(OpenRequestsActivity.this, "Failed to load technicians", Toast.LENGTH_SHORT).show();
                    }
                });

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void assignTechnicianToTicket(ServiceLogModel ticket, String techName, String techId) {
        DatabaseReference requestRef = FirebaseDatabase.getInstance(DB_URL).getReference("ServiceRequests").child(ticket.getTicketId());
        Map<String, Object> updates = new HashMap<>();
        updates.put("assignedTechName", techName);
        updates.put("assignedTechId", techId);
        updates.put("assignedTimestamp", ServerValue.TIMESTAMP);
        updates.put("status", "Open");

        requestRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
            AdminDashboardActivity.addAdminLog("Admin assigned ticket #" + ticket.getTicketId() + " to " + techName);
            Toast.makeText(this, "Ticket assigned to " + techName, Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Assignment failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void requestPermissionsIfNecessary() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
        }
    }

    @Override protected void onResume() { super.onResume(); if (mMap != null) mMap.onResume(); }
    @Override protected void onPause() { super.onPause(); if (mMap != null) mMap.onPause(); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbRef != null && requestsListener != null) {
            dbRef.removeEventListener(requestsListener);
        }
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }
}