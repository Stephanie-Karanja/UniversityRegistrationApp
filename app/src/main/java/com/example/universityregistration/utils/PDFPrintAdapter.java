package com.example.universityregistration.utils;

import android.content.Context;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.graphics.pdf.PdfDocument;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import java.io.FileOutputStream;
import java.io.IOException;

public class PDFPrintAdapter extends PrintDocumentAdapter {
    private Context context;
    private String reportContent;
    private int pageHeight;
    private int pageWidth;
    public PdfDocument pdfDocument;

    public PDFPrintAdapter(Context context, String reportContent) {
        this.context = context;
        this.reportContent = reportContent;
    }

    @Override
    public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                         CancellationSignal cancellationSignal, LayoutResultCallback callback,
                         Bundle extras) {
        pdfDocument = new PdfDocument();
        pageHeight = newAttributes.getMediaSize().getHeightMils() * 72 / 1000;
        pageWidth = newAttributes.getMediaSize().getWidthMils() * 72 / 1000;

        if (cancellationSignal.isCanceled()) {
            callback.onLayoutCancelled();
            return;
        }

        PrintDocumentInfo info = new PrintDocumentInfo.Builder("University_Registration_Summary.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build();

        callback.onLayoutFinished(info, true);
    }

    @Override
    public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
                        CancellationSignal cancellationSignal, WriteResultCallback callback) {
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);

        if (cancellationSignal.isCanceled()) {
            callback.onWriteCancelled();
            pdfDocument.close();
            pdfDocument = null;
            return;
        }

        drawPageContent(page.getCanvas());
        pdfDocument.finishPage(page);

        try {
            pdfDocument.writeTo(new FileOutputStream(destination.getFileDescriptor()));
            callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
        } catch (IOException e) {
            callback.onWriteFailed(e.toString());
        } finally {
            pdfDocument.close();
            pdfDocument = null;
        }
    }

    private void drawPageContent(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(16f);

        int x = 50;
        int y = 50;

        canvas.drawText("USIU - University Registration Summary Report", x, y, paint);
        paint.setTextSize(12f);
        y += 30;

        String[] lines = reportContent.split("\n");
        for (String line : lines) {
            canvas.drawText(line, x, y, paint);
            y += 20;
            if (y > pageHeight - 50) {
                break;
            }
        }
    }

    public static void generateStudentSummaryReport() {
        // Document printing / PDF export helper
    }
}
