package amiminourelhouda.grp2.findmyfriends.ui.notifications;

import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.json.JSONObject;

import java.util.HashMap;

import amiminourelhouda.grp2.findmyfriends.Config;
import amiminourelhouda.grp2.findmyfriends.JSONParser;
import amiminourelhouda.grp2.findmyfriends.databinding.FragmentNotificationsBinding;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.btnAddLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String pseudo = binding.edPseudoAddLocation.getText().toString().trim();
                String numero = binding.edNumeroAddLocation.getText().toString().trim();

                if(pseudo.isEmpty() || numero.isEmpty()) {
                    Toast.makeText(getContext(), "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Search for existing coordinates using the phone number
                new SearchAndUpdateLocationTask(pseudo, numero).execute();
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * AsyncTask that searches for existing location by phone number
     * and updates the pseudo, or inserts new location if not found
     */
    private class SearchAndUpdateLocationTask extends AsyncTask<Void, Void, JSONObject> {

        private String pseudo, numero;
        private AlertDialog progressDialog;

        public SearchAndUpdateLocationTask(String pseudo, String numero) {
            this.pseudo = pseudo;
            this.numero = numero;
        }

        @Override
        protected void onPreExecute() {
            AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
            dialog.setTitle("Recherche en cours...");
            dialog.setMessage("Veuillez patienter...");
            dialog.setCancelable(false);
            progressDialog = dialog.create();
            progressDialog.show();
        }

        @Override
        protected JSONObject doInBackground(Void... voids) {
            try {
                JSONParser parser = new JSONParser();
                HashMap<String, String> params = new HashMap<>();
                params.put("numero", numero);

                // Search for existing location with this phone number
                JSONObject response = parser.makeHttpRequest(
                        Config.URL_SearchLocationByNumero,  // New endpoint to search by phone
                        "POST",
                        params
                );

                return response;

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONObject response) {
            if(progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            try {
                if(response != null) {
                    int success = response.getInt("success");

                    if(success == 1) {
                        // Location found - get coordinates and update pseudo
                        String longitude = response.getString("longitude");
                        String latitude = response.getString("latitude");
                        String idposition = response.getString("idposition");

                        // Update the existing location with new pseudo
                        new UpdateLocationTask(idposition, pseudo, numero, longitude, latitude).execute();

                    } else {
                        // Location not found - show error message
                        Toast.makeText(getContext(),
                                "Numéro non trouvé. Veuillez vérifier le numéro de téléphone.",
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Erreur serveur", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Erreur: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * AsyncTask that updates the location with new pseudo
     */
    private class UpdateLocationTask extends AsyncTask<Void, Void, JSONObject> {

        private String idposition, pseudo, numero, longitude, latitude;
        private AlertDialog progressDialog;

        public UpdateLocationTask(String idposition, String pseudo, String numero, String longitude, String latitude) {
            this.idposition = idposition;
            this.pseudo = pseudo;
            this.numero = numero;
            this.longitude = longitude;
            this.latitude = latitude;
        }

        @Override
        protected void onPreExecute() {
            AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
            dialog.setTitle("Mise à jour...");
            dialog.setMessage("Veuillez patienter...");
            dialog.setCancelable(false);
            progressDialog = dialog.create();
            progressDialog.show();
        }

        @Override
        protected JSONObject doInBackground(Void... voids) {
            JSONParser parser = new JSONParser();
            HashMap<String, String> params = new HashMap<>();
            params.put("idposition", idposition);
            params.put("pseudo", pseudo);
            params.put("numero", numero);
            params.put("longitude", longitude);
            params.put("latitude", latitude);

            JSONObject response = parser.makeHttpRequest(
                    Config.URL_UpdatePosition,
                    "POST",
                    params
            );
            return response;
        }

        @Override
        protected void onPostExecute(JSONObject response) {
            if(progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            try {
                if(response != null) {
                    int success = response.getInt("success");
                    String message = response.getString("message");

                    Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();

                    if(success == 1) {
                        // Clear input fields on success
                        binding.edPseudoAddLocation.setText("");
                        binding.edNumeroAddLocation.setText("");
                    }
                } else {
                    Toast.makeText(getContext(), "Erreur serveur", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Erreur: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
}