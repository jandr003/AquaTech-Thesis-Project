package com.example.aquatech;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.text.Html;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

public class AquaFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "aquatech_fcm_channel";
    private static final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        saveTokenToFirebase(token);
    }

    public static void saveTokenToFirebase(String token) {
        if (token == null || token.isEmpty()) {
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }

        String uid = currentUser.getUid();
        DatabaseReference rootRef = FirebaseDatabase.getInstance(DB_URL).getReference();

        Map<String, Object> updates = new HashMap<>();
        updates.put("fcmToken", token);
        updates.put("lastTokenUpdate", System.currentTimeMillis());

        rootRef.child("Users").child(uid).updateChildren(updates);
        rootRef.child("Technicians").child(uid).updateChildren(updates);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = "AquaTech Notification";
        String body = "";
        String type = "";
        String ticketId = "";

        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null) {
                title = remoteMessage.getNotification().getTitle();
            }

            if (remoteMessage.getNotification().getBody() != null) {
                body = remoteMessage.getNotification().getBody();
            }
        }

        if (remoteMessage.getData() != null && !remoteMessage.getData().isEmpty()) {
            Map<String, String> data = remoteMessage.getData();
            if (data.containsKey("title") && data.get("title") != null) {
                title = data.get("title");
            }

            if (data.containsKey("body") && data.get("body") != null) {
                body = data.get("body");
            }

            if (data.containsKey("type") && data.get("type") != null) {
                type = data.get("type");
            }

            if (data.containsKey("ticketId") && data.get("ticketId") != null) {
                ticketId = data.get("ticketId");
            }
        }

        if (!body.isEmpty()) {
            sendPushNotification(title, body, type, ticketId);
        }
    }

    private void sendPushNotification(String title, String message, String type, String ticketId) {
        Intent intent;

        if ("CALL".equalsIgnoreCase(type)) {
            intent = new Intent(this, IncomingCallActivity.class);
            intent.putExtra("TICKET_ID", ticketId);
        } else if ("CHAT".equalsIgnoreCase(type) || "MESSAGE".equalsIgnoreCase(type)) {
            intent = new Intent(this, CustomerChatActivity.class);
            intent.putExtra("TICKET_ID", ticketId);
        } else if ("ASSIGNED".equalsIgnoreCase(type) || "JOB".equalsIgnoreCase(type)) {
            intent = new Intent(this, TechnicianDashboardActivity.class);
            intent.putExtra("RESUBMIT_TICKET_ID", ticketId);
        } else {
            intent = new Intent(this, SplashActivity.class);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        String parsedMessage;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            parsedMessage = Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY).toString();
        } else {
            parsedMessage = Html.fromHtml(message).toString();
        }

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.asc_logo_hd)
                        .setContentTitle(title)
                        .setContentText(parsedMessage)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "AquaTech Push Alerts",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Channel for AquaTech Push Notifications and Job Alerts");
                channel.enableVibration(true);
                notificationManager.createNotificationChannel(channel);
            }

            notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
        }
    }
}
