package com.example.aquatech;

import android.app.Activity;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;

public class CustomerDashboardAlertHelper {

    public static void showActiveRequestAlert(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        new AlertDialog.Builder(activity)
                .setTitle("Ongoing Request")
                .setMessage("You currently have an active service request. Please wait for it to be completed before submitting a new one.")
                .setPositiveButton("Track Progress", (dialog, which) -> activity.startActivity(new Intent(activity, ServiceLogActivity.class)))
                .setNegativeButton("Close", null)
                .show();
    }

    public static void showNoTechAssignedAlert(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        new AlertDialog.Builder(activity)
                .setTitle("Technician Not Assigned")
                .setMessage("A technician needs to be assigned to your request before you can chat.")
                .setPositiveButton("OK", null)
                .show();
    }

    public static void showTechNotAcceptedAlert(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        new AlertDialog.Builder(activity)
                .setTitle("Request Pending")
                .setMessage("The technician has been assigned but hasn't accepted your request yet. Please wait for them to accept to start chatting.")
                .setPositiveButton("OK", null)
                .show();
    }

    public static void showNoActiveRequestAlert(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        new AlertDialog.Builder(activity)
                .setTitle("No Active Request")
                .setMessage("Would you like to submit one now?")
                .setPositiveButton("Submit", (dialog, which) -> activity.startActivity(new Intent(activity, ServiceRequestActivity.class)))
                .setNegativeButton("Not Now", null)
                .show();
    }

    public static String getTemplateForUnit(String name) {
        if (name == null) {
            return "F-FXCU1-M-HCA-TT-**-***";
        }

        String upper = name.toUpperCase();
        if (upper.contains("SLIM")) {
            return "S-FXCU1-M-HCA-AA-**-***";
        }

        if (upper.contains("COUNTER")) {
            return "CT-FXCU1-M-HCA-WT-**-***";
        }

        if (upper.contains("STANDING")) {
            return "ST-FXCU1-M-HCA-WT-**-***";
        }

        return "F-FXCU1-M-HCA-TT-**-***";
    }

    public static int getImageForUnit(String name) {
        if (name == null) {
            return R.drawable.titled_design1;
        }

        String upper = name.toUpperCase();
        if (upper.contains("SLIM")) {
            return R.drawable.titled_design2;
        }

        if (upper.contains("COUNTER")) {
            return R.drawable.titled_design3;
        }

        if (upper.contains("STANDING")) {
            return R.drawable.titled_design4;
        }

        return R.drawable.titled_design1;
    }
}
