package com.example.aquatech;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
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
            window.setWindowAnimations(android.R.style.Animation_Dialog);
        }

        ImageView ivPreview = reportDialog.findViewById(R.id.imgProofPreview);
        EditText etRemarks = reportDialog.findViewById(R.id.etReportRemarks);

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

                if (activeTaskSnapshot == null) {
                    Toast.makeText(activity, "No active ticket found to submit report for", Toast.LENGTH_SHORT).show();
                    return;
                }

                ProgressDialog pd = new ProgressDialog(activity);
                pd.setMessage("Uploading report...");
                pd.setCancelable(false);
                pd.show();

                String fileName = "report_" + System.currentTimeMillis() + ".jpg";
                StorageReference fileRef = FirebaseStorage.getInstance().getReference("Reports/" + fileName);

                fileRef.putFile(proofUri)
                        .addOnSuccessListener(ts -> ts.getStorage().getDownloadUrl().addOnSuccessListener(uri -> {
                            String remarksText = (etRemarks != null && etRemarks.getText() != null) ? etRemarks.getText().toString() : "";
                            saveReportToDatabase(activity, dbUrl, activeTaskSnapshot, currentTechUid, currentTechName, uri.toString(), remarksText, pd, reportDialog, callback);
                        }))
                        .addOnFailureListener(e -> {
                            try {
                                pd.dismiss();
                            } catch (Exception ignored) {}
                            Toast.makeText(activity, "Failed to upload report photo: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            });
        }

        View btnClose = reportDialog.findViewById(R.id.btnCloseReport);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> reportDialog.dismiss());
        }

        reportDialog.show();
    }

    private static void saveReportToDatabase(
            Activity activity,
            String dbUrl,
            DataSnapshot activeTaskSnapshot,
            String currentTechUid,
            String currentTechName,
            String downloadUrl,
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
                updates.put("submissionTimestamp", ServerValue.TIMESTAMP);

                requestsRef.child(ticketId).updateChildren(updates).addOnCompleteListener(t -> {
                    try {
                        pd.dismiss();
                    } catch (Exception ignored) {}

                    AdminDashboardActivity.addAdminLog("Technician " + currentTechName + " submitted a report for ticket #" + ticketNum);

                    try {
                        if (reportDialog != null && reportDialog.isShowing()) {
                            reportDialog.dismiss();
                        }
                    } catch (Exception ignored) {}

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        showSubmissionSuccessDialog(activity, ticketNum, callback);
                    }, 150);
                });
            } else {
                try {
                    pd.dismiss();
                } catch (Exception ignored) {}
            }
        });
    }

    public static void showSubmissionSuccessDialog(Activity activity, String ticketNum, OnReportSubmittedListener callback) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            if (callback != null) {
                callback.onReportSubmitted();
            }
            return;
        }

        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_report_submission_success);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.CENTER);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setWindowAnimations(android.R.style.Animation_Dialog);
        }

        View cardSuccess = dialog.findViewById(R.id.cardSuccessRoot);
        ImageView ivIcon = dialog.findViewById(R.id.ivSuccessIcon);
        TextView tvMsg = dialog.findViewById(R.id.tvReportSuccessMessage);

        if (tvMsg != null && ticketNum != null) {
            tvMsg.setText("Your service report for Ticket #" + ticketNum + " has been successfully submitted to dispatch. Please wait while the admin reviews and approves your submission.");
        }

        if (cardSuccess != null) {
            cardSuccess.setAlpha(0f);
            cardSuccess.setScaleX(0.85f);
            cardSuccess.setScaleY(0.85f);
            cardSuccess.animate().alpha(1f).scaleX(1.0f).scaleY(1.0f).setDuration(350).setInterpolator(new OvershootInterpolator(1.2f)).start();
        }

        if (ivIcon != null) {
            ivIcon.setScaleX(0.2f);
            ivIcon.setScaleY(0.2f);
            ivIcon.animate().scaleX(1.0f).scaleY(1.0f).setDuration(450).setStartDelay(100).setInterpolator(new OvershootInterpolator(1.5f)).start();
        }

        View btnDone = dialog.findViewById(R.id.btnDoneReportSuccess);
        if (btnDone != null) {
            btnDone.setOnClickListener(v -> {
                if (cardSuccess != null) {
                    cardSuccess.animate().alpha(0f).scaleX(0.85f).scaleY(0.85f).setDuration(250).withEndAction(() -> {
                        dialog.dismiss();
                        if (callback != null) {
                            callback.onReportSubmitted();
                        }
                    }).start();
                } else {
                    dialog.dismiss();
                    if (callback != null) {
                        callback.onReportSubmitted();
                    }
                }
            });
        }

        dialog.show();
    }
}
