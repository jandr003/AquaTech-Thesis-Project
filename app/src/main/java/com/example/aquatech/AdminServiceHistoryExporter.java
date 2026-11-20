package com.example.aquatech;

import android.app.Activity;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminServiceHistoryExporter {

    public static void exportToPdf(Activity activity, List<ServiceLogModel> targetList) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        if (targetList == null || targetList.isEmpty()) {
            Toast.makeText(activity, "No service history records available to export", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            View pdfView = LayoutInflater.from(activity).inflate(R.layout.layout_service_history_pdf_template, null);
            if (pdfView == null) {
                return;
            }

            TextView tvDate = pdfView.findViewById(R.id.pdfReportGeneratedDate);
            if (tvDate != null) {
                String dateStr = new SimpleDateFormat("MMMM dd, yyyy hh:mm a", Locale.getDefault()).format(new Date());
                tvDate.setText("Generated: " + dateStr);
            }

            TextView tvTotalRec = pdfView.findViewById(R.id.pdfReportTotalRecords);
            if (tvTotalRec != null) {
                tvTotalRec.setText("Total Records: " + targetList.size());
            }

            int completedCount = 0;
            int pendingCount = 0;
            for (ServiceLogModel item : targetList) {
                if (item != null) {
                    if ("Completed".equalsIgnoreCase(item.getStatus())) {
                        completedCount++;
                    } else if (!"Cancelled".equalsIgnoreCase(item.getStatus())) {
                        pendingCount++;
                    }
                }
            }

            TextView tvStatTotal = pdfView.findViewById(R.id.pdfStatTotalCount);
            if (tvStatTotal != null) {
                tvStatTotal.setText(String.valueOf(targetList.size()));
            }

            TextView tvStatCompleted = pdfView.findViewById(R.id.pdfStatCompletedCount);
            if (tvStatCompleted != null) {
                tvStatCompleted.setText(String.valueOf(completedCount));
            }

            TextView tvStatPending = pdfView.findViewById(R.id.pdfStatPendingCount);
            if (tvStatPending != null) {
                tvStatPending.setText(String.valueOf(pendingCount));
            }

            LinearLayout containerRows = pdfView.findViewById(R.id.pdfHistoryContainer);
            if (containerRows != null) {
                containerRows.removeAllViews();

                for (int i = 0; i < targetList.size(); i++) {
                    ServiceLogModel item = targetList.get(i);
                    if (item == null) {
                        continue;
                    }

                    View rowView = LayoutInflater.from(activity).inflate(R.layout.item_pdf_history_row, containerRows, false);
                    if (rowView != null) {
                        TextView tvTicket = rowView.findViewById(R.id.pdfRowTicket);
                        TextView tvCustomer = rowView.findViewById(R.id.pdfRowCustomer);
                        TextView tvTech = rowView.findViewById(R.id.pdfRowTech);
                        TextView tvRowDate = rowView.findViewById(R.id.pdfRowDate);
                        TextView tvStatus = rowView.findViewById(R.id.pdfRowStatus);

                        String sro = item.getSroNumber();
                        if (sro == null || sro.isEmpty()) {
                            sro = item.getTicketId();
                        }

                        if (tvTicket != null) {
                            tvTicket.setText(sro != null ? sro : "---");
                        }

                        if (tvCustomer != null) {
                            tvCustomer.setText(item.getCustomerName() != null ? item.getCustomerName() : "---");
                        }

                        if (tvTech != null) {
                            tvTech.setText(item.getTechName() != null ? item.getTechName() : "Unassigned");
                        }

                        if (tvRowDate != null) {
                            tvRowDate.setText(item.getDateTime() != null ? item.getDateTime() : "---");
                        }

                        if (tvStatus != null) {
                            String st = item.getStatus() != null ? item.getStatus() : "Open";
                            tvStatus.setText(st);

                            if ("Completed".equalsIgnoreCase(st)) {
                                tvStatus.setTextColor(Color.parseColor("#16A34A"));
                            } else if ("Cancelled".equalsIgnoreCase(st)) {
                                tvStatus.setTextColor(Color.parseColor("#DC2626"));
                            } else {
                                tvStatus.setTextColor(Color.parseColor("#D97706"));
                            }
                        }

                        containerRows.addView(rowView);
                    }
                }
            }

            pdfView.measure(
                    View.MeasureSpec.makeMeasureSpec(1200, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            );
            pdfView.layout(0, 0, pdfView.getMeasuredWidth(), pdfView.getMeasuredHeight());

            Bitmap bitmap = Bitmap.createBitmap(pdfView.getMeasuredWidth(), pdfView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            pdfView.draw(canvas);

            PdfDocument document = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);

            Canvas pageCanvas = page.getCanvas();
            pageCanvas.drawBitmap(bitmap, 0f, 0f, null);
            document.finishPage(page);

            savePdfToDownloads(activity, document);

        } catch (Exception e) {
            Toast.makeText(activity, "Error generating PDF report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public static void exportToCsv(Activity activity, List<ServiceLogModel> targetList) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        if (targetList == null || targetList.isEmpty()) {
            Toast.makeText(activity, "No service history records available to export", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder csvData = new StringBuilder();
        csvData.append("Ticket ID,Reference No (SRO),Customer Name,Customer Phone,Technician Name,Unit Model,Service Date,Status\n");

        for (ServiceLogModel item : targetList) {
            if (item == null) {
                continue;
            }

            csvData.append(escapeCsvField(item.getTicketId()))
                    .append(",")
                    .append(escapeCsvField(item.getSroNumber()))
                    .append(",")
                    .append(escapeCsvField(item.getCustomerName()))
                    .append(",")
                    .append(escapeCsvField(item.getCustomerPhone()))
                    .append(",")
                    .append(escapeCsvField(item.getTechName()))
                    .append(",")
                    .append(escapeCsvField(item.getUnitName()))
                    .append(",")
                    .append(escapeCsvField(item.getDateTime()))
                    .append(",")
                    .append(escapeCsvField(item.getStatus()))
                    .append("\n");
        }

        String fileName = "AquaTech_Service_History_" + System.currentTimeMillis() + ".csv";

        try {
            OutputStream os;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues cv = new ContentValues();
                cv.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                cv.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
                cv.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AquaTech");

                Uri uri = activity.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                if (uri != null) {
                    os = activity.getContentResolver().openOutputStream(uri);
                } else {
                    os = null;
                }
            } else {
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "AquaTech");
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                File file = new File(dir, fileName);
                os = new FileOutputStream(file);
            }

            if (os != null) {
                os.write(csvData.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();
                Toast.makeText(activity, "CSV Report exported to Downloads/AquaTech!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(activity, "Failed to create CSV export file", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(activity, "Export Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private static void savePdfToDownloads(Activity activity, PdfDocument document) {
        if (activity == null || document == null) {
            return;
        }

        String fileName = "AquaTech_Service_History_Report_" + System.currentTimeMillis() + ".pdf";
        try {
            OutputStream os;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues cv = new ContentValues();
                cv.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                cv.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                cv.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AquaTech");

                Uri uri = activity.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                if (uri != null) {
                    os = activity.getContentResolver().openOutputStream(uri);
                } else {
                    os = null;
                }
            } else {
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "AquaTech");
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                File file = new File(dir, fileName);
                os = new FileOutputStream(file);
            }

            if (os != null) {
                document.writeTo(os);
                os.flush();
                os.close();
                Toast.makeText(activity, "Official PDF Report exported to Downloads/AquaTech!", Toast.LENGTH_LONG).show();
            }

            document.close();
        } catch (Exception e) {
            Toast.makeText(activity, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private static String escapeCsvField(String data) {
        if (data == null) {
            return "\"\"";
        }

        String field = data.replace("\"", "\"\"");
        return "\"" + field + "\"";
    }
}
