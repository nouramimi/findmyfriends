package amiminourelhouda.grp2.findmyfriends.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.HashMap;

import amiminourelhouda.grp2.findmyfriends.Config;
import amiminourelhouda.grp2.findmyfriends.JSONParser;

public class LocationSentReceiver extends BroadcastReceiver {

    private static final String TAG = "LocationSentReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (!"ACTION_LOCATION_SENT".equals(intent.getAction())) return;

        String numero = intent.getStringExtra("sender");
        double latitude = intent.getDoubleExtra("latitude", 0);
        double longitude = intent.getDoubleExtra("longitude", 0);

        Log.e(TAG, "Broadcast received: numero=" + numero + ", lat=" + latitude + ", lon=" + longitude);

        // Lance la tâche d'insertion
        new InsertLocationTask(context, numero, latitude, longitude).execute();
    }

    static class InsertLocationTask extends AsyncTask<Void, Void, String> {

        private final Context context;
        private final String numero;
        private final double latitude, longitude;

        InsertLocationTask(Context context, String numero, double latitude, double longitude) {
            this.context = context;
            this.numero = numero;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                JSONParser parser = new JSONParser();

                // Préparer les paramètres POST
                HashMap<String, String> params = new HashMap<>();
                params.put("pseudo", "Unknown");
                params.put("numero", numero);
                params.put("latitude", String.valueOf(latitude));
                params.put("longitude", String.valueOf(longitude));

                JSONObject response = parser.makeHttpRequest(Config.URL_InsertLocation, "POST", params);

                if (response != null) {
                    Log.e(TAG, "Server Response JSON: " + response.toString());
                    return response.optString("message", "Aucune réponse du serveur");
                } else {
                    Log.e(TAG, "Server Response is null");
                    return "Erreur : réponse nulle du serveur";
                }

            } catch (Exception e) {
                Log.e(TAG, "Exception HTTP", e);
                return "Exception : " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String message) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            Log.e(TAG, "Insert result: " + message);
        }
    }
}
