package com.example.yazilimproje;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PdfReportHelper {

    public static Uri generateAnalysisPdf(Context context, AnalysisResult result) {
        PdfDocument document = null;
        OutputStream outputStream = null;

        try {
            document = new PdfDocument();

            PdfDocument.PageInfo pageInfo =
                    new PdfDocument.PageInfo.Builder(595, 842, 1).create();

            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            Paint paint = new Paint();
            paint.setAntiAlias(true);

            int y = 50;

            paint.setTextSize(18);
            paint.setFakeBoldText(true);
            canvas.drawText("İSUTS - İlaç Semptom Uyuşmazlık Raporu", 40, y, paint);

            y += 30;
            paint.setTextSize(11);
            paint.setFakeBoldText(false);
            canvas.drawText("Rapor Tarihi: " + now(), 40, y, paint);

            y += 30;
            paint.setTextSize(13);
            paint.setFakeBoldText(true);
            canvas.drawText("Risk Seviyesi: " + result.riskLevel.name(), 40, y, paint);

            y += 25;
            canvas.drawText("Başlık: " + safe(result.baslik), 40, y, paint);

            y += 35;
            paint.setFakeBoldText(true);
            canvas.drawText("Analiz Açıklaması", 40, y, paint);

            y += 20;
            paint.setFakeBoldText(false);
            y = drawMultiline(canvas, paint, safe(result.aciklama), 40, y, 82);

            y += 25;
            paint.setFakeBoldText(true);
            canvas.drawText("Analiz Edilen İlaçlar", 40, y, paint);

            y += 20;
            paint.setFakeBoldText(false);

            List<String> ilaclar = result.analizEdilenIlaclar;

            if (ilaclar != null && !ilaclar.isEmpty()) {
                for (String ilac : ilaclar) {
                    canvas.drawText("- " + safe(ilac), 50, y, paint);
                    y += 18;

                    if (y > 760) {
                        document.finishPage(page);
                        page = document.startPage(pageInfo);
                        canvas = page.getCanvas();
                        y = 50;
                    }
                }
            } else {
                canvas.drawText("- İlaç bilgisi bulunamadı.", 50, y, paint);
                y += 18;
            }

            y += 35;
            paint.setFakeBoldText(true);
            canvas.drawText("Yasal Uyarı", 40, y, paint);

            y += 20;
            paint.setFakeBoldText(false);
            drawMultiline(
                    canvas,
                    paint,
                    "Bu rapor tıbbi teşhis veya tedavi önerisi değildir. Kritik durumlarda hekime veya eczacıya danışılmalıdır.",
                    40,
                    y,
                    82
            );

            document.finishPage(page);

            String fileName = "ISUTS_Rapor_" + System.currentTimeMillis() + ".pdf";

            Uri pdfUri;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = context.getContentResolver();

                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                pdfUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

                if (pdfUri == null) {
                    return null;
                }

                outputStream = resolver.openOutputStream(pdfUri);
                document.writeTo(outputStream);

                return pdfUri;

            } else {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                );

                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs();
                }

                File file = new File(downloadsDir, fileName);
                outputStream = new FileOutputStream(file);
                document.writeTo(outputStream);

                pdfUri = FileProvider.getUriForFile(
                        context,
                        "com.example.yazilimproje.fileprovider",
                        file
                );

                return pdfUri;
            }

        } catch (Exception e) {
            return null;

        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (Exception ignored) {
            }

            try {
                if (document != null) {
                    document.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    private static int drawMultiline(Canvas canvas, Paint paint, String text, int x, int startY, int maxChars) {
        int y = startY;

        if (text == null || text.trim().isEmpty()) {
            canvas.drawText("Bilgi bulunamadı.", x, y, paint);
            return y + 18;
        }

        String[] lines = text.split("\n");

        for (String line : lines) {
            String current = line.trim();

            if (current.isEmpty()) {
                y += 14;
                continue;
            }

            while (current.length() > maxChars) {
                String part = current.substring(0, maxChars);
                int lastSpace = part.lastIndexOf(" ");

                if (lastSpace > 20) {
                    part = current.substring(0, lastSpace);
                    current = current.substring(lastSpace).trim();
                } else {
                    current = current.substring(maxChars).trim();
                }

                canvas.drawText(part, x, y, paint);
                y += 18;
            }

            canvas.drawText(current, x, y, paint);
            y += 18;
        }

        return y;
    }

    private static String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    private static String safe(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "Belirtilmedi";
        }

        return text;
    }
}