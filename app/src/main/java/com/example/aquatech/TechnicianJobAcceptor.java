package com.example.aquatech;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class TechnicianJobAcceptor {

    public static void acceptTask(
            Context context,
            String dbUrl,
            DataSnapshot activeTaskSnapshot,
            DataSnapshot taskToAccept,
            String currentTechUid,
            String currentTechName,
            Runnable onSuccess
    ) {
        if (activeTaskSnapshot != null) {
            Toast.makeText(context, "Finish your active ticket first!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentTechName == null || currentTechName.trim().isEmpty() || currentTechUid == null || currentTechUid.trim().isEmpty()) {
            Toast.makeText(context, "Loading user data...", Toast.LENGTH_SHORT).show();
            return;
        }

        String dbKey = taskToAccept.getKey();
        String ticketId = taskToAccept.child("ticketId").getValue(String.class);
        String customerId = taskToAccept.child("userId").getValue(String.class);
        String customerName = taskToAccept.child("customerName").getValue(String.class);

        if (dbKey == null) return;

        DatabaseReference requestsRef = FirebaseDatabase.getInstance(dbUrl).getReference("ServiceRequests");
        DatabaseReference techRef = FirebaseDatabase.getInstance(dbUrl).getReference("Technicians").child(currentTechUid).child("signature");

        techRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String signature = snapshot.getValue(String.class);
                if (signature == null || signature.isEmpty()) signature = "signature1_png";
                performAccept(context, dbUrl, requestsRef, dbKey, ticketId, customerId, customerName, currentTechUid, currentTechName, signature, onSuccess);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                performAccept(context, dbUrl, requestsRef, dbKey, ticketId, customerId, customerName, currentTechUid, currentTechName, "signature1_png", onSuccess);
            }
        });
    }

    private static void performAccept(
            Context context,
            String dbUrl,
            DatabaseReference requestsRef,
            String dbKey,
            String ticketId,
            String customerId,
            String customerName,
            String currentTechUid,
            String currentTechName,
            String signature,
            Runnable onSuccess
    ) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "In Progress");
        updates.put("assignedTechId", currentTechUid);
        updates.put("assignedTechName", currentTechName);
        updates.put("assignedTimestamp", ServerValue.TIMESTAMP);
        updates.put("technicianSignature", signature);

        requestsRef.child(dbKey).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    AdminDashboardActivity.addAdminLog("Technician " + currentTechName + " accepted ticket #" + ticketId);
                    if (customerId != null && !customerId.isEmpty()) {
                        String htmlMsg = "<b>Technician Accepted!</b><br>Technician " + currentTechName + " has accepted your ticket #" + ticketId;
                        NotificationActivity.addNotification(customerId.trim(), htmlMsg, "CHAT", ticketId);
                        TechnicianChatAutomation.sendAcceptanceChatMessage(dbUrl, currentTechUid, currentTechName, customerId.trim(), ticketId, customerName, htmlMsg);
                    }
                    Toast.makeText(context, "Ticket Accepted!", Toast.LENGTH_SHORT).show();
                    if (onSuccess != null) onSuccess.run();
                })
                .addOnFailureListener(e -> Toast.makeText(context, "Failed to accept ticket", Toast.LENGTH_SHORT).show());
    }
}
