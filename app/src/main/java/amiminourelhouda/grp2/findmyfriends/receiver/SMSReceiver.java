package amiminourelhouda.grp2.findmyfriends.receiver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import amiminourelhouda.grp2.findmyfriends.MapsActivity;
import amiminourelhouda.grp2.findmyfriends.service.LocationService;

public class SMSReceiver extends BroadcastReceiver {

    private static final String TAG = "SMSReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        Bundle bundle = intent.getExtras();
        if (bundle == null) return;

        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null) return;

        for (Object pdu : pdus) {
            SmsMessage sms;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                String format = bundle.getString("format");
                sms = SmsMessage.createFromPdu((byte[]) pdu, format);
            } else {
                sms = SmsMessage.createFromPdu((byte[]) pdu);
            }

            String sender = sms.getOriginatingAddress();
            String message = sms.getMessageBody();

            if (message != null && message.trim().equalsIgnoreCase("send me your location")) {
                Toast.makeText(context, "Demande de localisation reçue de " + sender, Toast.LENGTH_SHORT).show();

                // Lance le service pour récupérer la position
                Intent serviceIntent = new Intent(context, LocationService.class);
                serviceIntent.putExtra("sender", sender);
                ContextCompat.startForegroundService(context, serviceIntent);
            }
        }
    }

    // Méthode à appeler depuis un BroadcastReceiver pour afficher la notification
    public static void showLocationNotification(Context context, String sender, double latitude, double longitude) {
        String channelId = "location_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Service de Localisation",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications pour la localisation reçue");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        Intent clickIntent = new Intent(context, MapsActivity.class);
        clickIntent.putExtra("latitude", latitude);
        clickIntent.putExtra("longitude", longitude);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0)
        );

        Notification notification = new NotificationCompat.Builder(context, channelId)
                .setContentTitle("Localisation reçue")
                .setContentText("Cliquez pour voir la position envoyée par " + sender)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build();

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(100, notification);
    }
}
