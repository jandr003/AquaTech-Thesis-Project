package com.example.aquatech;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class TechnicianReportDialogHelper {

    public interface OnReportSubmittedListener {
        void onReportSubmitted();
    }

    public static void showSubmitReportDialog(
            Activity activity,
            String dbUrl,
            DataSnapshot activeTaskSnapshot,
            String currentTechUid,
            String currentTechName,
            Uri proofUri,
            String currentRemarks,
            Runnable onRequestPhoto,
            OnReportSubmittedListener callback
    ) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        Dialog reportDialog = new Dialog(activity);
        reportDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        reportDialog.setContentView(R.layout.dialog_submit_report);

        Window window = reportDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.CENTER);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        ImageView ivPreview = reportDialog.findViewById(R.id.imgProofPreview);
        EditText etRemarks = reportDialog.findViewById(R.id.etReportRemarks);
        SignaturePadView signaturePad = reportDialog.findViewById(R.id.signaturePad);
        TextView btnClearSignature = reportDialog.findViewById(R.id.btnClearSignature);

        if (btnClearSignature != null && signaturePad != null) {
            btnClearSignature.setOnClickListener(v -> signaturePad.clear());
        }

        if (etRemarks != null && currentRemarks != null) {
            etRemarks.setText(currentRemarks);
        }

        if (ivPreview != null) {
            if (proofUri != null) {
                ivPreview.setImageTintList(null);
                Glide.with(activity).load(proofUri).signature(new ObjectKey(System.currentTimeMillis())).into(ivPreview);
            } else {
                ivPreview.setImageResource(R.drawable.upload_file_png);
            }
        }

        View btnUpload = reportDialog.findViewById(R.id.btnUploadProof);
        if (btnUpload != null) {
            btnUpload.setOnClickListener(v -> {
                if (onRequestPhoto != null) {
                    onRequestPhoto.run();
                }
            });
        }

        View btnSubmit = reportDialog.findViewById(R.id.btnSubmitReport);
        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                if (proofUri == null) {
                    Toast.makeText(activity, "Please add proof of service photo first", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (signaturePad == null || !signaturePad.isSigned()) {
                    Toast.makeText(activity, "Please ask customer to sign before submitting", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (activeTaskSnapshot == null) {
                    Toast.makeText(activity, "No active ticket found to submit report for", Toast.LENGTH_SHORT).show();
                    return;
                }

                ProgressDialog pd = new ProgressDialog(activity);
                pd.setMessage("Uploading report and signature...");
                pd.setCancelable(false);
                pd.show();

                uploadPhotoAndSignature(activity, dbUrl, activeTaskSnapshot, currentTechUid, currentTechName, proofUri, signaturePad.getSignatureBitmap(), etRemarks != null ? etRemarks.getText().toString() : "", pd, reportDialog, callback);
            });
        }

        View btnClose = reportDialog.findViewById(R.id.btnCloseReport);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> reportDialog.dismiss());
        }

        reportDialog.show();
    }

    private static void uploadPhotoAndSignature(
            Activity activity,
            String dbUrl,
            DataSnapshot activeTaskSnapshot,
            String currentTechUid,
            String currentTechName,
            Uri proofUri,
            Bitmap signatureBitmap,
            String remarksText,
            ProgressDialog pd,
            Dialog reportDialog,
            OnReportSubmittedListener callback
    ) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        StorageReference proofRef = FirebaseStorage.getInstance().getReference("Reports/report_" + timestamp + ".jpg");
        StorageReference sigRef = FirebaseStorage.getInstance().getReference("Signatures/sig_" + timestamp + ".png");

        proofRef.putFile(proofUri)
                .addOnSuccessListener(taskSnapshot -> proofRef.getDownloadUrl().addOnSuccessListener(proofDownloadUrl -> {

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    if (signatureBitmap != null) {
                        signatureBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                    }
                    byte[] sigBytes = baos.toByteArray();

                    sigRef.putBytes(sigBytes)
                            .addOnSuccessListener(sigTask -> sigRef.getDownloadUrl().addOnSuccessListener(sigDownloadUrl -> {
                                saveReportToDatabase(activity, dbUrl, activeTaskSnapshot, currentTechUid, currentTechName, proofDownloadUrl.toString(), sigDownloadUrl.toString(), remarksText, pd, reportDialog, callback);
                            }))
                            .addOnFailureListener(e -> {
                                saveReportToDatabase(activity, dbUrl, activeTaskSnapshot, currentTechUid, currentTechName, proofDownloadUrl.toString(), "", remarksText, pd, reportDialog, callback);
                            });

                }))
                .addOnFailureListener(e -> {
                    try {
                        pd.dismiss();
                    } catch (Exception ignored) {}
                    Toast.makeText(activity, "Failed to upload report: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private static void saveReportToDatabase(
            Activity activity,
            String dbUrl,
            DataSnapshot activeTaskSnapshot,
            String currentTechUid,
            String currentTechName,
            String downloadUrl,
            String signatureUrl,
            String remarks,
            ProgressDialog pd,
            Dialog reportDialog,
            OnReportSubmittedListener callback
    ) {
        if (activeTaskSnapshot == null) {
            return;
        }

        String ticketId = activeTaskSnapshot.getKey();
        String ticketNum = activeTaskSnapshot.child("ticketId").getValue(String.class);

        DatabaseReference dbReportRef = FirebaseDatabase.getInstance(dbUrl).getReference("Reports").push();
        HashMap<String, Object> data = new HashMap<>();
        data.put("techUid", currentTechUid);
        data.put("techName", currentTechName);
        data.put("customerName", activeTaskSnapshot.child("customerName").getValue(String.class));
        data.put("remarks", remarks);
        data.put("proofUrl", downloadUrl);
        data.put("signatureUrl", signatureUrl);
        data.put("ticketId", ticketId);
        data.put("ticketNum", ticketNum);
        data.put("timestamp", ServerValue.TIMESTAMP);

        dbReportRef.setValue(data).addOnCompleteListener(task -> {
            if (task.isSuccessful() && ticketId != null) {
                DatabaseReference requestsRef = FirebaseDatabase.getInstance(dbUrl).getReference("ServiceRequests");
                Map<String, Object> updates = new HashMap<>();
                updates.put("status", "Submitted");
                updates.put("technicianRemarks", remarks);
                updates.put("proofImageUrl", downloadUrl);
                updates.put("signatureUrl", signatureUrl);
                updates.put("submissionTimestamp", ServerValue.TIMESTAMP);

                requestsRef.child(ticketId).updateChildren(updates).addOnCompleteListener(t -> {
                    try {
                        pd.dismiss();
                    } catch (Exception ignored) {}

                    AdminDashboardActivity.addAdminLog("Technician " + currentTechName + " submitted a signed report for ticket #" + ticketNum);

                    try {
                        if (reportDialog != null && reportDialog.isShowing()) {
                            reportDialog.dismiss();
                        }
                    } catch (Exception ignored) {}

                    if (callback != null) {
                        callback.onReportSubmitted();
                    }

                    Toast.makeText(activity, "Report & Digital Signature Submitted Successfully!", Toast.LENGTH_LONG).show();
                });
            } else {
                try {
                    pd.dismiss();
                } catch (Exception ignored) {}
            }
        });
    }
}
