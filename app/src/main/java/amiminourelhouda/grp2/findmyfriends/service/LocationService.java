package amiminourelhouda.grp2.findmyfriends.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONObject;

import java.util.HashMap;

import amiminourelhouda.grp2.findmyfriends.Config;
import amiminourelhouda.grp2.findmyfriends.JSONParser;

public class LocationService extends Service {

    private static final String TAG = "LocationService";
    private static final String CHANNEL_ID = "location_service_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Récupération de la localisation")
                .setContentText("Veuillez patienter...")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true)
                .build();
        startForeground(1, notification);

        String sender = intent.getStringExtra("sender");
        if (sender == null) {
            Toast.makeText(this, "Numéro destinataire non fourni", Toast.LENGTH_SHORT).show();
            stopSelf();
            return START_NOT_STICKY;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission de localisation non accordée", Toast.LENGTH_SHORT).show();
            stopSelf();
            return START_NOT_STICKY;
        }

        FusedLocationProviderClient fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        double lat = location.getLatitude();
                        double lon = location.getLongitude();
                        String message = "Ma position :\nLatitude: " + lat + "\nLongitude: " + lon;

                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                                == PackageManager.PERMISSION_GRANTED) {
                            SmsManager smsManager = SmsManager.getDefault();
                            smsManager.sendTextMessage(sender, null, message, null, null);
                            Toast.makeText(this, "Localisation envoyée à " + sender, Toast.LENGTH_SHORT).show();

                            // Insertion directe dans la base de données
                            Log.e(TAG, "Lancement insertion BDD: sender=" + sender + ", lat=" + lat + ", lon=" + lon);
                            new InsertLocationTask(sender, lat, lon).execute();
                        }
                    } else {
                        Toast.makeText(this, "Impossible de récupérer la localisation", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Location null");
                        stopSelf();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur récupération localisation", e);
                    stopSelf();
                });

        return START_NOT_STICKY;
    }

    // AsyncTask pour l'insertion en base de données
    private class InsertLocationTask extends AsyncTask<Void, Void, String> {

        private final String numero;
        private final double latitude, longitude;

        InsertLocationTask(String numero, double latitude, double longitude) {
            this.numero = numero;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                JSONParser parser = new JSONParser();

                HashMap<String, String> params = new HashMap<>();
                params.put("pseudo", "Unknown");
                params.put("numero", numero);
                params.put("latitude", String.valueOf(latitude));
                params.put("longitude", String.valueOf(longitude));

                Log.e(TAG, "Envoi requête HTTP vers: " + Config.URL_InsertLocation);
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
            Log.e(TAG, "Insert result: " + message);
            Toast.makeText(LocationService.this, message, Toast.LENGTH_LONG).show();
            stopSelf(); // Arrête le service après l'insertion
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Service de localisation",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Notifications pour le service de localisation");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}