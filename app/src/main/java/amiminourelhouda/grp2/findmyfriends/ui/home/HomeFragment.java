package amiminourelhouda.grp2.findmyfriends.ui.home;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import amiminourelhouda.grp2.findmyfriends.Config;
import amiminourelhouda.grp2.findmyfriends.JSONParser;
import amiminourelhouda.grp2.findmyfriends.MapsActivity;
import amiminourelhouda.grp2.findmyfriends.MyRecyclerPositionAdapter;
import amiminourelhouda.grp2.findmyfriends.Position;
import amiminourelhouda.grp2.findmyfriends.R;
import amiminourelhouda.grp2.findmyfriends.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment implements OnMapReadyCallback {

    public ArrayList<Position> data = new ArrayList<>();
    private FragmentHomeBinding binding;
    private GoogleMap mMap;
    private ViewMode currentViewMode = ViewMode.SPLIT;
    private SupportMapFragment mapFragment;
    private MyRecyclerPositionAdapter adapter;
    private HashMap<Marker, Position> markerPositionMap = new HashMap<>();

    private enum ViewMode {
        LIST_ONLY, SPLIT, MAP_ONLY
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        setupRecyclerView();
        initializeMapFragment();

        binding.btnHome.setOnClickListener(v -> new Download().execute());
        binding.btnSyncEmpty.setOnClickListener(v -> new Download().execute());

        binding.btnToggleList.setOnClickListener(v -> setViewMode(ViewMode.LIST_ONLY));
        binding.btnToggleSplit.setOnClickListener(v -> setViewMode(ViewMode.SPLIT));
        binding.btnToggleMap.setOnClickListener(v -> setViewMode(ViewMode.MAP_ONLY));

        binding.searchViewHome.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (adapter != null) adapter.filter(query);
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapter != null) adapter.filter(newText);
                return false;
            }
        });

        return root;
    }

    private void setupRecyclerView() {
        adapter = new MyRecyclerPositionAdapter(getContext(), data, new MyRecyclerPositionAdapter.OnPositionClickListener() {
            @Override
            public void onShowMapClick(Position position) {
                Intent intent = new Intent(getContext(), MapsActivity.class);
                try {
                    double lat = Double.parseDouble(position.latitude);
                    double lon = Double.parseDouble(position.longitude);
                    intent.putExtra("latitude", lat);
                    intent.putExtra("longitude", lon);
                    startActivity(intent);
                } catch (NumberFormatException e) {
                    Log.e("HomeFragment", "Invalid coordinates", e);
                }
            }
            @Override
            public void onPositionDeleted() {
                if (mMap != null) displayMarkersOnMap();
            }
            @Override
            public void onPositionUpdated() {
                if (mMap != null) displayMarkersOnMap();
            }
        });
        binding.rvHome.setAdapter(adapter);
        binding.rvHome.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
    }

    private void initializeMapFragment() {
        mapFragment = SupportMapFragment.newInstance();
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(R.id.map_container, mapFragment);
        transaction.commit();
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (mMap != null) {
            mMap.getUiSettings().setZoomControlsEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);

            // Click on marker -> show info window
            mMap.setOnMarkerClickListener(marker -> {
                marker.showInfoWindow();
                return true;
            });

            // Click on info window -> edit/delete
            mMap.setOnInfoWindowClickListener(marker -> {
                Position position = markerPositionMap.get(marker);
                if (position != null) {
                    showMarkerOptionsDialog(position, marker);
                }
            });

            // LONG CLICK on map -> add new position
            mMap.setOnMapLongClickListener(latLng -> {
                showAddPositionDialog(latLng);
            });

            if (!data.isEmpty()) {
                displayMarkersOnMap();
            }
        }
    }

    // Dialog to add a new position when long-clicking on map
    private void showAddPositionDialog(LatLng latLng) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Ajouter une position");

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_add_position, null);
        builder.setView(dialogView);

        EditText edPseudo = dialogView.findViewById(R.id.ed_pseudo_add);
        EditText edNumero = dialogView.findViewById(R.id.ed_numero_add);
        EditText edLongitude = dialogView.findViewById(R.id.ed_longitude_add);
        EditText edLatitude = dialogView.findViewById(R.id.ed_latitude_add);
        Button btnValider = dialogView.findViewById(R.id.btn_valider_add);
        Button btnAnnuler = dialogView.findViewById(R.id.btn_annuler_add);

        // Pre-fill coordinates from the clicked location
        edLatitude.setText(String.valueOf(latLng.latitude));
        edLongitude.setText(String.valueOf(latLng.longitude));

        AlertDialog dialog = builder.create();
        dialog.show();

        btnValider.setOnClickListener(view -> {
            String pseudo = edPseudo.getText().toString().trim();
            String numero = edNumero.getText().toString().trim();
            String longitude = edLongitude.getText().toString().trim();
            String latitude = edLatitude.getText().toString().trim();

            if (pseudo.isEmpty() || numero.isEmpty() || longitude.isEmpty() || latitude.isEmpty()) {
                Toast.makeText(getContext(), "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                return;
            }

            new InsertPositionTask(pseudo, numero, longitude, latitude, dialog).execute();
        });

        btnAnnuler.setOnClickListener(view -> dialog.dismiss());
    }

    // AsyncTask to insert new position
    private class InsertPositionTask extends AsyncTask<Void, Void, JSONObject> {
        String pseudo, numero, longitude, latitude;
        AlertDialog addDialog;
        AlertDialog progressDialog;

        InsertPositionTask(String pseudo, String numero, String longitude, String latitude, AlertDialog addDialog) {
            this.pseudo = pseudo;
            this.numero = numero;
            this.longitude = longitude;
            this.latitude = latitude;
            this.addDialog = addDialog;
        }

        @Override
        protected void onPreExecute() {
            if (!isAdded()) return;
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Ajout en cours...");
            builder.setMessage("Veuillez patienter...");
            builder.setCancelable(false);
            progressDialog = builder.create();
            progressDialog.show();
        }

        @Override
        protected JSONObject doInBackground(Void... voids) {
            try {
                JSONParser parser = new JSONParser();
                HashMap<String, String> params = new HashMap<>();
                params.put("pseudo", pseudo);
                params.put("numero", numero);
                params.put("longitude", longitude);
                params.put("latitude", latitude);

                return parser.makeHttpRequest(Config.URL_InsertLocation, "POST", params);
            } catch (Exception e) {
                Log.e("InsertError", e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONObject response) {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            try {
                if (response != null) {
                    int success = response.optInt("success", 0);
                    String message = response.optString("message", "Erreur");

                    if (success == 1) {
                        Toast.makeText(getContext(), "Position ajoutée avec succès!", Toast.LENGTH_SHORT).show();
                        addDialog.dismiss();
                        // Refresh the list
                        new Download().execute();
                    } else {
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Erreur serveur", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(getContext(), "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showMarkerOptionsDialog(Position position, Marker marker) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(position.pseudo);
        builder.setItems(new CharSequence[]{"Modifier", "Supprimer", "Annuler"},
                (dialog, which) -> {
                    switch (which) {
                        case 0: showEditDialogForMap(position); break;
                        case 1: showDeleteDialogForMap(position); break;
                        case 2: dialog.dismiss(); break;
                    }
                });
        builder.show();
    }

    private void showEditDialogForMap(Position position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setCancelable(true);
        builder.setTitle("Modifier la position");

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_edit_position, null);
        builder.setView(dialogView);

        EditText edPseudo = dialogView.findViewById(R.id.ed_pseudo_edit);
        EditText edNumero = dialogView.findViewById(R.id.ed_numero_edit);
        EditText edLongitude = dialogView.findViewById(R.id.ed_longitude_edit);
        EditText edLatitude = dialogView.findViewById(R.id.ed_latitude_edit);
        Button btnValider = dialogView.findViewById(R.id.btn_valider_edit);
        Button btnAnnuler = dialogView.findViewById(R.id.btn_annuler_edit);

        edPseudo.setText(position.pseudo);
        edNumero.setText(position.numero);
        edLongitude.setText(position.longitude);
        edLatitude.setText(position.latitude);

        AlertDialog dialog = builder.create();
        dialog.show();

        btnValider.setOnClickListener(view -> {
            String pseudo = edPseudo.getText().toString().trim();
            String numero = edNumero.getText().toString().trim();
            String longitude = edLongitude.getText().toString().trim();
            String latitude = edLatitude.getText().toString().trim();

            if (pseudo.isEmpty() || numero.isEmpty() || longitude.isEmpty() || latitude.isEmpty()) {
                Toast.makeText(getContext(), "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                return;
            }
            new UpdatePositionFromMapTask(position.idposition, pseudo, numero, longitude, latitude, dialog).execute();
        });

        btnAnnuler.setOnClickListener(view -> dialog.dismiss());
    }

    private void showDeleteDialogForMap(Position position) {
        new AlertDialog.Builder(getContext())
                .setTitle("Suppression")
                .setMessage("Voulez-vous vraiment supprimer cette position ?")
                .setPositiveButton("Oui", (dialog, which) -> new DeletePositionFromMapTask(position.idposition).execute())
                .setNegativeButton("Non", null)
                .show();
    }

    private void setViewMode(ViewMode mode) {
        currentViewMode = mode;
        switch (mode) {
            case LIST_ONLY:
                binding.rvHome.setVisibility(View.VISIBLE);
                binding.divider.setVisibility(View.GONE);
                binding.mapContainer.setVisibility(View.GONE);
                binding.searchViewHome.setVisibility(View.VISIBLE);
                LinearLayout.LayoutParams listParams = (LinearLayout.LayoutParams) binding.rvHome.getLayoutParams();
                listParams.weight = 1;
                binding.rvHome.setLayoutParams(listParams);
                break;
            case SPLIT:
                binding.rvHome.setVisibility(View.VISIBLE);
                binding.divider.setVisibility(View.VISIBLE);
                binding.mapContainer.setVisibility(View.VISIBLE);
                binding.searchViewHome.setVisibility(View.VISIBLE);
                LinearLayout.LayoutParams splitListParams = (LinearLayout.LayoutParams) binding.rvHome.getLayoutParams();
                splitListParams.weight = 1;
                binding.rvHome.setLayoutParams(splitListParams);
                LinearLayout.LayoutParams splitMapParams = (LinearLayout.LayoutParams) binding.mapContainer.getLayoutParams();
                splitMapParams.weight = 1;
                binding.mapContainer.setLayoutParams(splitMapParams);
                if (mMap != null && !data.isEmpty()) displayMarkersOnMap();
                break;
            case MAP_ONLY:
                binding.rvHome.setVisibility(View.GONE);
                binding.divider.setVisibility(View.GONE);
                binding.mapContainer.setVisibility(View.VISIBLE);
                binding.searchViewHome.setVisibility(View.GONE);
                LinearLayout.LayoutParams mapParams = (LinearLayout.LayoutParams) binding.mapContainer.getLayoutParams();
                mapParams.weight = 1;
                binding.mapContainer.setLayoutParams(mapParams);
                if (mMap != null && !data.isEmpty()) displayMarkersOnMap();
                break;
        }
    }

    private void displayMarkersOnMap() {
        if (mMap == null || data.isEmpty()) return;
        mMap.clear();
        markerPositionMap.clear();
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boolean hasValidPositions = false;

        for (Position position : data) {
            try {
                double lat = Double.parseDouble(position.latitude);
                double lon = Double.parseDouble(position.longitude);
                LatLng latLng = new LatLng(lat, lon);
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(position.pseudo)
                        .snippet("Tel: " + position.numero));
                markerPositionMap.put(marker, position);
                boundsBuilder.include(latLng);
                hasValidPositions = true;
            } catch (NumberFormatException e) {
                Log.e("HomeFragment", "Invalid coordinates", e);
            }
        }

        if (hasValidPositions) {
            try {
                LatLngBounds bounds = boundsBuilder.build();
                binding.mapContainer.post(() -> {
                    try {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
                    } catch (Exception e) {
                        Log.e("HomeFragment", "Camera error", e);
                    }
                });
            } catch (Exception e) {
                Log.e("HomeFragment", "Bounds error", e);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ... AsyncTasks for Update and Delete (same as before)
    class UpdatePositionFromMapTask extends AsyncTask<Void, Void, Boolean> {
        int idposition; String pseudo, numero, longitude, latitude;
        AlertDialog editDialog, progressDialog;

        UpdatePositionFromMapTask(int id, String p, String n, String lon, String lat, AlertDialog d) {
            idposition = id; pseudo = p; numero = n; longitude = lon; latitude = lat; editDialog = d;
        }
        @Override protected void onPreExecute() {
            if (!isAdded()) return;
            progressDialog = new AlertDialog.Builder(getContext()).setTitle("Mise à jour...").setMessage("Patientez...").setCancelable(false).create();
            progressDialog.show();
        }
        @Override protected Boolean doInBackground(Void... v) {
            try {
                JSONParser parser = new JSONParser();
                HashMap<String, String> params = new HashMap<>();
                params.put("idposition", String.valueOf(idposition));
                params.put("pseudo", pseudo); params.put("numero", numero);
                params.put("longitude", longitude); params.put("latitude", latitude);
                JSONObject response = parser.makeHttpRequest(Config.URL_UpdatePosition, "POST", params);
                return response.getInt("success") == 1;
            } catch (Exception e) { return false; }
        }
        @Override protected void onPostExecute(Boolean success) {
            if (progressDialog != null) progressDialog.dismiss();
            if (success) {
                for (Position pos : data) if (pos.idposition == idposition) { pos.pseudo = pseudo; pos.numero = numero; pos.longitude = longitude; pos.latitude = latitude; break; }
                if (adapter != null) adapter.updateData(data);
                displayMarkersOnMap();
                Toast.makeText(getContext(), "Position modifiée", Toast.LENGTH_SHORT).show();
                editDialog.dismiss();
            } else Toast.makeText(getContext(), "Échec modification", Toast.LENGTH_SHORT).show();
        }
    }

    class DeletePositionFromMapTask extends AsyncTask<Void, Void, Boolean> {
        int idposition; AlertDialog progressDialog;
        DeletePositionFromMapTask(int id) { idposition = id; }
        @Override protected void onPreExecute() {
            if (!isAdded()) return;
            progressDialog = new AlertDialog.Builder(getContext()).setTitle("Suppression...").setMessage("Patientez...").setCancelable(false).create();
            progressDialog.show();
        }
        @Override protected Boolean doInBackground(Void... v) {
            try {
                JSONParser parser = new JSONParser();
                HashMap<String, String> params = new HashMap<>();
                params.put("idposition", String.valueOf(idposition));
                JSONObject response = parser.makeHttpRequest(Config.URL_DeletePosition, "POST", params);
                return response.getInt("success") == 1;
            } catch (Exception e) { return false; }
        }
        @Override protected void onPostExecute(Boolean success) {
            if (progressDialog != null) progressDialog.dismiss();
            if (success) {
                data.removeIf(p -> p.idposition == idposition);
                if (adapter != null) adapter.updateData(data);
                displayMarkersOnMap();
                Toast.makeText(getContext(), "Position supprimée", Toast.LENGTH_SHORT).show();
            } else Toast.makeText(getContext(), "Échec suppression", Toast.LENGTH_SHORT).show();
        }
    }

    class Download extends AsyncTask<Void, Void, Void> {
        AlertDialog alert;
        @Override protected void onPreExecute() {
            if (!isAdded()) return;
            alert = new AlertDialog.Builder(getContext()).setTitle("Chargement...").setMessage("Patientez...").create();
            alert.show();
        }
        @Override protected Void doInBackground(Void... v) {
            data.clear();
            try {
                JSONParser parser = new JSONParser();
                JSONObject response = parser.makeHttpRequest(Config.URL_GetALLLocations, "GET", null);
                if (response.getInt("success") == 1) {
                    JSONArray result = response.getJSONArray("positions");
                    for (int i = 0; i < result.length(); i++) {
                        JSONObject ligne = result.getJSONObject(i);
                        data.add(new Position(ligne.getInt("idposition"), ligne.getString("pseudo"),
                                ligne.getString("numero"), ligne.getString("longitude"), ligne.getString("latitude")));
                    }
                }
            } catch (JSONException e) { e.printStackTrace(); }
            return null;
        }
        @Override protected void onPostExecute(Void v) {
            if (alert != null && isAdded()) alert.dismiss();
            if (!data.isEmpty()) {
                binding.emptyState.setVisibility(View.GONE);
                binding.searchViewHome.setVisibility(View.VISIBLE);
                binding.viewToggleContainer.setVisibility(View.VISIBLE);
                binding.contentContainer.setVisibility(View.VISIBLE);
                if (adapter != null) adapter.updateData(data);
                setViewMode(ViewMode.SPLIT);
            } else {
                binding.emptyState.setVisibility(View.VISIBLE);
                binding.searchViewHome.setVisibility(View.GONE);
                binding.viewToggleContainer.setVisibility(View.GONE);
                binding.contentContainer.setVisibility(View.GONE);
            }
        }
    }
}