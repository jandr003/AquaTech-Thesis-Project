package com.example.aquatech;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class MapPickerActivity extends AppCompatActivity {

    private MapView mMap;
    private EditText etSearch;
    private TextView tvAddress;
    private ImageView btnBack, btnClear;
    private View btnConfirm;
    private GeoPoint selectedPoint;
    private String selectedAddress = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_map_picker);

        mMap = findViewById(R.id.mapPicker);
        etSearch = findViewById(R.id.etSearchLocation);
        tvAddress = findViewById(R.id.tvSelectedAddress);
        btnBack = findViewById(R.id.btnBackPicker);
        btnClear = findViewById(R.id.btnClearSearch);
        btnConfirm = findViewById(R.id.btnConfirmLocation);

        mMap.setTileSource(TileSourceFactory.MAPNIK);
        mMap.setMultiTouchControls(true);
        mMap.getController().setZoom(17.0);

        GeoPoint startPoint = new GeoPoint(14.558472, 121.032389);
        mMap.getController().setCenter(startPoint);

        setupListeners();
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnClear.setOnClickListener(v -> {
            etSearch.setText("");
            btnClear.setVisibility(View.GONE);
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchLocation(etSearch.getText().toString());
                return true;
            }
            return false;
        });

        btnConfirm.setOnClickListener(v -> {
            if (selectedPoint != null) {
                Intent data = new Intent();
                data.putExtra("ADDRESS", selectedAddress);
                data.putExtra("LAT", selectedPoint.getLatitude());
                data.putExtra("LNG", selectedPoint.getLongitude());
                setResult(RESULT_OK, data);
                finish();
            }
        });

        mMap.addMapListener(new MapListener() {
            @Override public boolean onScroll(ScrollEvent event) { updateCenter(); return true; }
            @Override public boolean onZoom(ZoomEvent event) { return true; }
        });
    }

    private void updateCenter() {
        GeoPoint center = (GeoPoint) mMap.getMapCenter();
        selectedPoint = center;
        new ReverseGeocodeTask().execute(center);
    }

    private void searchLocation(String query) {
        if (query.isEmpty()) return;
        new SearchTask().execute(query);
    }

    private class SearchTask extends AsyncTask<String, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(String... params) {
            try {
                String urlStr = "https://nominatim.openstreetmap.org/search?q=" + URLEncoder.encode(params[0], "UTF-8") + "&format=json&limit=1";
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", getPackageName());
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder(); String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                JSONArray array = new JSONArray(sb.toString());
                return array.length() > 0 ? array.getJSONObject(0) : null;
            } catch (Exception e) { return null; }
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            if (result != null) {
                double lat = result.optDouble("lat");
                double lon = result.optDouble("lon");
                GeoPoint point = new GeoPoint(lat, lon);
                mMap.getController().animateTo(point);
                selectedAddress = result.optString("display_name");
                tvAddress.setText(selectedAddress);
            } else {
                Toast.makeText(MapPickerActivity.this, "Location not found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class ReverseGeocodeTask extends AsyncTask<GeoPoint, Void, String> {
        @Override
        protected String doInBackground(GeoPoint... points) {
            try {
                String urlStr = "https://nominatim.openstreetmap.org/reverse?lat=" + points[0].getLatitude() + "&lon=" + points[0].getLongitude() + "&format=json";
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", getPackageName());
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder(); String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                JSONObject json = new JSONObject(sb.toString());
                return json.optString("display_name");
            } catch (Exception e) { return null; }
        }

        @Override
        protected void onPostExecute(String address) {
            if (address != null) {
                selectedAddress = address;
                tvAddress.setText(address);
            }
        }
    }

    @Override protected void onResume() { super.onResume(); mMap.onResume(); }
    @Override protected void onPause() { super.onPause(); mMap.onPause(); }
}
