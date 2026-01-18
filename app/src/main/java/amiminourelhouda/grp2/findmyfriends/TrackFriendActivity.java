package amiminourelhouda.grp2.findmyfriends;

import androidx.fragment.app.FragmentActivity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class TrackFriendActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Position friendToTrack;
    private Marker currentMarker;
    private ArrayList<LatLng> locationHistory = new ArrayList<>();
    private Polyline pathPolyline;

    private Handler trackingHandler;
    private Runnable trackingRunnable;
    private boolean isTracking = false;

    private TextView tvFriendName, tvLastUpdate, tvCoordinates, tvInterval;
    private Button btnStartStop, btnClearHistory, btnChangeInterval;

    // Default interval: 1 minute (in milliseconds)
    private int trackingIntervalMinutes = 1;
    private long trackingIntervalMs = 60 * 1000;

    // Available intervals in minutes
    private final int[] INTERVAL_OPTIONS = {1, 2, 5, 10, 15, 30, 60};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_friend);

        int idposition = getIntent().getIntExtra("idposition", -1);
        String pseudo = getIntent().getStringExtra("pseudo");
        String numero = getIntent().getStringExtra("numero");
        String longitude = getIntent().getStringExtra("longitude");
        String latitude = getIntent().getStringExtra("latitude");

        if (idposition == -1) {
            Toast.makeText(this, "Erreur: Position invalide", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        friendToTrack = new Position(idposition, pseudo, numero, longitude, latitude);

        // Initialize views
        tvFriendName = findViewById(R.id.tv_friend_name);
        tvLastUpdate = findViewById(R.id.tv_last_update);
        tvCoordinates = findViewById(R.id.tv_coordinates);
        tvInterval = findViewById(R.id.tv_interval);
        btnStartStop = findViewById(R.id.btn_start_stop_tracking);
        btnClearHistory = findViewById(R.id.btn_clear_history);
        btnChangeInterval = findViewById(R.id.btn_change_interval);

        tvFriendName.setText("Tracking: " + friendToTrack.pseudo);
        updateCoordinatesDisplay();
        updateIntervalDisplay();

        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_tracking);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Initialize tracking handler
        trackingHandler = new Handler();
        trackingRunnable = new Runnable() {
            @Override
            public void run() {
                if (isTracking) {
                    fetchFriendLocation();
                    trackingHandler.postDelayed(this, trackingIntervalMs);
                }
            }
        };

        // Start/Stop button
        btnStartStop.setOnClickListener(v -> toggleTracking());

        // Clear history button
        btnClearHistory.setOnClickListener(v -> clearLocationHistory());

        // Change interval button
        btnChangeInterval.setOnClickListener(v -> showIntervalDialog());

        // Back button
        findViewById(R.id.btn_back).setOnClickListener(v -> {
            if (isTracking) {
                new AlertDialog.Builder(this)
                        .setTitle("Arrêter le tracking?")
                        .setMessage("Le tracking est en cours. Voulez-vous l'arrêter et quitter?")
                        .setPositiveButton("Oui", (dialog, which) -> {
                            stopTracking();
                            finish();
                        })
                        .setNegativeButton("Non", null)
                        .show();
            } else {
                finish();
            }
        });
    }

    private void showIntervalDialog() {
        // Don't allow changing interval while tracking
        if (isTracking) {
            Toast.makeText(this, "Arrêtez le tracking d'abord", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] intervalLabels = new String[INTERVAL_OPTIONS.length];
        for (int i = 0; i < INTERVAL_OPTIONS.length; i++) {
            if (INTERVAL_OPTIONS[i] < 60) {
                intervalLabels[i] = INTERVAL_OPTIONS[i] + " minute" + (INTERVAL_OPTIONS[i] > 1 ? "s" : "");
            } else {
                intervalLabels[i] = (INTERVAL_OPTIONS[i] / 60) + " heure";
            }
        }

        // Find current selection index
        int currentIndex = 0;
        for (int i = 0; i < INTERVAL_OPTIONS.length; i++) {
            if (INTERVAL_OPTIONS[i] == trackingIntervalMinutes) {
                currentIndex = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Choisir l'intervalle de tracking")
                .setSingleChoiceItems(intervalLabels, currentIndex, (dialog, which) -> {
                    trackingIntervalMinutes = INTERVAL_OPTIONS[which];
                    trackingIntervalMs = trackingIntervalMinutes * 60 * 1000L;
                    updateIntervalDisplay();
                    dialog.dismiss();
                    Toast.makeText(this, "Intervalle: " + intervalLabels[which], Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void showCustomIntervalDialog() {
        if (isTracking) {
            Toast.makeText(this, "Arrêtez le tracking d'abord", Toast.LENGTH_SHORT).show();
            return;
        }

        // Custom dialog with NumberPicker
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Intervalle personnalisé (minutes)");

        final NumberPicker numberPicker = new NumberPicker(this);
        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(120);
        numberPicker.setValue(trackingIntervalMinutes);
        numberPicker.setWrapSelectorWheel(false);

        builder.setView(numberPicker);
        builder.setPositiveButton("OK", (dialog, which) -> {
            trackingIntervalMinutes = numberPicker.getValue();
            trackingIntervalMs = trackingIntervalMinutes * 60 * 1000L;
            updateIntervalDisplay();
        });
        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private void updateIntervalDisplay() {
        String intervalText;
        if (trackingIntervalMinutes < 60) {
            intervalText = trackingIntervalMinutes + " min";
        } else {
            intervalText = (trackingIntervalMinutes / 60) + "h";
        }
        tvInterval.setText("⏱ " + intervalText);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        updateMapWithCurrentLocation();
    }

    private void toggleTracking() {
        if (isTracking) {
            stopTracking();
        } else {
            startTracking();
        }
    }

    private void startTracking() {
        isTracking = true;
        btnStartStop.setText("STOP");
        btnStartStop.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
        btnChangeInterval.setEnabled(false);
        btnChangeInterval.setAlpha(0.5f);

        String msg = "Tracking démarré - MAJ toutes les " + trackingIntervalMinutes + " min";
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();

        fetchFriendLocation();
        trackingHandler.postDelayed(trackingRunnable, trackingIntervalMs);
    }

    private void stopTracking() {
        isTracking = false;
        btnStartStop.setText("START");
        btnStartStop.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
        btnChangeInterval.setEnabled(true);
        btnChangeInterval.setAlpha(1.0f);
        trackingHandler.removeCallbacks(trackingRunnable);
        Toast.makeText(this, "Tracking arrêté", Toast.LENGTH_SHORT).show();
    }

    private void fetchFriendLocation() {
        new FetchLocationTask().execute();
    }

    private void updateMapWithCurrentLocation() {
        if (mMap == null) return;

        try {
            double lat = Double.parseDouble(friendToTrack.latitude);
            double lon = Double.parseDouble(friendToTrack.longitude);
            LatLng position = new LatLng(lat, lon);

            if (currentMarker != null) {
                currentMarker.remove();
            }

            currentMarker = mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(friendToTrack.pseudo)
                    .snippet("Position actuelle")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

            locationHistory.add(position);
            drawPath();
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15));
            updateCoordinatesDisplay();
            updateLastUpdateTime();

        } catch (NumberFormatException e) {
            Log.e("TrackFriend", "Invalid coordinates", e);
        }
    }

    private void drawPath() {
        if (mMap == null || locationHistory.size() < 2) return;

        if (pathPolyline != null) {
            pathPolyline.remove();
        }

        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(locationHistory)
                .color(Color.BLUE)
                .width(8)
                .geodesic(true);

        pathPolyline = mMap.addPolyline(polylineOptions);

        for (int i = 0; i < locationHistory.size() - 1; i++) {
            mMap.addMarker(new MarkerOptions()
                    .position(locationHistory.get(i))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                    .alpha(0.5f)
                    .title("Point " + (i + 1)));
        }
    }

    private void updateCoordinatesDisplay() {
        tvCoordinates.setText(String.format("Lat: %s, Lon: %s",
                friendToTrack.latitude, friendToTrack.longitude));
    }

    private void updateLastUpdateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        tvLastUpdate.setText("MAJ: " + sdf.format(new Date()));
    }

    private void clearLocationHistory() {
        new AlertDialog.Builder(this)
                .setTitle("Effacer l'historique")
                .setMessage("Voulez-vous effacer l'historique de tracking?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    locationHistory.clear();
                    if (mMap != null) mMap.clear();
                    if (pathPolyline != null) {
                        pathPolyline.remove();
                        pathPolyline = null;
                    }
                    updateMapWithCurrentLocation();
                    Toast.makeText(this, "Historique effacé", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Non", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (trackingHandler != null) trackingHandler.removeCallbacks(trackingRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isTracking) stopTracking();
    }

    class FetchLocationTask extends AsyncTask<Void, Void, Boolean> {
        String newLatitude, newLongitude;

        @Override
        protected void onPreExecute() {
            tvLastUpdate.setText("Mise à jour...");
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                JSONParser parser = new JSONParser();
                HashMap<String, String> params = new HashMap<>();
                params.put("idposition", String.valueOf(friendToTrack.idposition));

                JSONObject response = parser.makeHttpRequest(Config.URL_GetPositionById, "POST", params);
                Log.e("TrackingResponse", response.toString());

                int success = response.getInt("success");
                if (success == 1) {
                    JSONObject position = response.getJSONObject("position");
                    newLatitude = position.getString("latitude");
                    newLongitude = position.getString("longitude");
                    return true;
                }
                return false;
            } catch (Exception e) {
                Log.e("TrackingError", e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                friendToTrack.latitude = newLatitude;
                friendToTrack.longitude = newLongitude;
                updateMapWithCurrentLocation();
                Toast.makeText(TrackFriendActivity.this, "Position mise à jour", Toast.LENGTH_SHORT).show();
            } else {
                tvLastUpdate.setText("MAJ: Échec");
                Toast.makeText(TrackFriendActivity.this, "Échec de la mise à jour", Toast.LENGTH_SHORT).show();
            }
        }
    }
}