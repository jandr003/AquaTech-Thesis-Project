package com.example.aquatech;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentValues;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
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

        PdfDocument document = new PdfDocument();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        int pageWidth = 595;
        int pageHeight = 842;
        int margin = 35;
        int y = 45;

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        titlePaint.setColor(Color.parseColor("#1E3A8A"));
        titlePaint.setTextSize(20f);
        canvas.drawText("AQUATECH OFFICIAL SERVICE HISTORY", margin, y, titlePaint);
        y += 25;

        paint.setColor(Color.parseColor("#64748B"));
        paint.setTextSize(10.5f);
        String dateStr = new SimpleDateFormat("MMMM dd, yyyy hh:mm a", Locale.getDefault()).format(new Date());
        canvas.drawText("Generated on: " + dateStr + "  |  Total Records: " + targetList.size(), margin, y, paint);
        y += 25;

        paint.setColor(Color.parseColor("#F1F5F9"));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(margin, y, pageWidth - margin, y + 24, paint);

        paint.setColor(Color.parseColor("#1E293B"));
        paint.setTextSize(9.5f);
        paint.setFakeBoldText(true);
        canvas.drawText("SRO / TICKET", margin + 8, y + 16, paint);
        canvas.drawText("CUSTOMER", margin + 115, y + 16, paint);
        canvas.drawText("TECHNICIAN", margin + 245, y + 16, paint);
        canvas.drawText("SERVICE DATE", margin + 375, y + 16, paint);
        canvas.drawText("STATUS", margin + 475, y + 16, paint);
        y += 28;

        paint.setFakeBoldText(false);
        int limit = Math.min(targetList.size(), 28);
        for (int i = 0; i < limit; i++) {
            ServiceLogModel item = targetList.get(i);
            if (item == null) {
                continue;
            }

            String sro = item.getSroNumber() != null ? item.getSroNumber() : item.getTicketId();
            canvas.drawText(truncate(sro, 14), margin + 8, y, paint);
            canvas.drawText(truncate(item.getCustomerName(), 16), margin + 115, y, paint);
            canvas.drawText(truncate(item.getTechName(), 16), margin + 245, y, paint);
            canvas.drawText(truncate(item.getDateTime(), 14), margin + 375, y, paint);
            canvas.drawText(truncate(item.getStatus(), 10), margin + 475, y, paint);

            paint.setColor(Color.parseColor("#E2E8F0"));
            canvas.drawLine(margin, y + 5, pageWidth - margin, y + 5, paint);
            paint.setColor(Color.parseColor("#1E293B"));

            y += 22;
        }

        document.finishPage(page);
        savePdfToDownloads(activity, document);
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

            csvData.append(escapeCsvField(item.getTicketId())).append(",");
            csvData.append(escapeCsvField(item.getSroNumber())).append(",");
            csvData.append(escapeCsvField(item.getCustomerName())).append(",");
            csvData.append(escapeCsvField(item.getCustomerPhone())).append(",");
            csvData.append(escapeCsvField(item.getTechName())).append(",");
            csvData.append(escapeCsvField(item.getUnitName())).append(",");
            csvData.append(escapeCsvField(item.getDateTime())).append(",");
            csvData.append(escapeCsvField(item.getStatus())).append("\n");
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
                os = uri != null ? activity.getContentResolver().openOutputStream(uri) : null;
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
                os = uri != null ? activity.getContentResolver().openOutputStream(uri) : null;
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

    private static String truncate(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        return text.length() > maxLen ? text.substring(0, maxLen - 2) + ".." : text;
    }

    private static String escapeCsvField(String data) {
        if (data == null) {
            return "\"\"";
        }

        String field = data.replace("\"", "\"\"");
        return "\"" + field + "\"";
    }
}
