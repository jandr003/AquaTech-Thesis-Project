package com.example.aquatech;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CallHelper {
    public static final int REQUEST_MIC_PERMISSION = 200;
    private static final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    public static void initiateCall(Activity activity, String techId, String customerId, String otherName, boolean isTech) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MIC_PERMISSION);
            return;
        }

        String chatId = techId + "_" + customerId;
        DatabaseReference callRef = FirebaseDatabase.getInstance(DB_URL).getReference("UserChats").child(chatId);
        
        // Reset call metadata to trigger "calling" state
        callRef.child("callStatus").setValue("calling");
        callRef.child("callStartTime").removeValue();

        // Send Notification to Receiver
        String receiverId = isTech ? customerId : techId;
        String callerName = isTech ? "Technician" : otherName; 
        String notifMessage = "<b>Incoming Call</b><br>" + callerName + " is calling.";
        String notifType = "CALL";
        
        // Target correctly: if Technician is calling, send to CustomerNotifications. If Customer is calling, send to Notifications (for Tech).
        String notifTarget = isTech ? "CustomerNotifications" : "Notifications";
        
        DatabaseReference notifPushRef = FirebaseDatabase.getInstance(DB_URL).getReference(notifTarget).child(receiverId).push();
        notifPushRef.setValue(new NotificationModel(notifPushRef.getKey(), notifMessage, System.currentTimeMillis(), notifType, isTech ? techId : customerId));

        // Start appropriate activity for the caller
        // If isTech is true, start VoiceCallActivity (Technician's UI). 
        // If isTech is false, start CustomerVoiceCallActivity (Customer's UI).
        Intent intent = new Intent(activity, isTech ? VoiceCallActivity.class : CustomerVoiceCallActivity.class);
        intent.putExtra("TECH_ID", techId);
        intent.putExtra("CUSTOMER_ID", customerId);
        intent.putExtra("NAME", otherName);
        activity.startActivity(intent);
    }

    public static void startCall(Activity activity, String techName, String techId, String customerId) {
        // This is called by Technician, so isTech = true
        initiateCall(activity, techId, customerId, techName, true);
    }
}