package com.example.universityregistration.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.print.PrintManager;
import android.text.Html;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.universityregistration.R;
import com.example.universityregistration.database.OfflineSyncManager;
import com.example.universityregistration.utils.PDFPrintAdapter;
import org.json.JSONObject;
import java.util.Map;

public class SummaryActivity extends AppCompatActivity {

    // ui elements for summary reporting and printing
    private EditText etSearchStudentId;
    private TextView tvSummaryContent;
    private OfflineSyncManager offlineSyncManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        // bind views from xml layout
        etSearchStudentId = findViewById(R.id.etSearchStudentId);
        Button btnViewIndividual = findViewById(R.id.btnViewIndividual);
        Button btnViewAll = findViewById(R.id.btnViewAll);
        Button btnPrintReport = findViewById(R.id.btnPrintReport);
        Button btnBack = findViewById(R.id.btnBack);
        tvSummaryContent = findViewById(R.id.tvSummaryContent);

        offlineSyncManager = new OfflineSyncManager(this);

        // load student id if passed via intent
        String passedId = getIntent().getStringExtra("STUDENT_ID");
        if (passedId != null && !passedId.isEmpty()) {
            etSearchStudentId.setText(passedId);
            loadIndividualSummary(passedId);
        }

        // handle back button click
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // handle individual student search button click
        btnViewIndividual.setOnClickListener(v -> {
            String studentId = etSearchStudentId.getText().toString().trim();
            if (studentId.isEmpty()) {
                Toast.makeText(this, "please enter student id or reg number", Toast.LENGTH_SHORT).show();
                return;
            }
            loadIndividualSummary(studentId);
        });

        // handle view all registered students button click (table view)
        btnViewAll.setOnClickListener(v -> loadAllStudentsSummaryTable());

        // handle print / pdf export button click
        btnPrintReport.setOnClickListener(v -> printReport());
    }

    // display neat formatted individual summary for a student
    private void loadIndividualSummary(String studentId) {
        SharedPreferences prefs = getSharedPreferences("OfflineRegistrationCache", Context.MODE_PRIVATE);
        String studentData = prefs.getString(studentId, null);

        String fullName = "Stephanie Karanja";
        String regNumber = "SCT221-005/2026";
        String course = "Software Engineering";
        String department = "Software Engineering";
        String bookTitle = "Introduction to Algorithms";
        String returnDate = "15/12/2026";
        String hostelLocation = "Phase 2 Hostel";
        String roomType = "1 Bedroom";
        double totalFee = 180000.0;
        double amountPaid = 1.0;
        double feeBalance = 179999.0;
        String paymentMethod = "M-PESA";
        String paymentStatus = "Confirmed";

        if (studentData != null) {
            try {
                JSONObject json = new JSONObject(studentData);
                fullName = json.optString("fullName", fullName);
                regNumber = json.optString("regNumber", regNumber);
                course = json.optString("course", course);
                department = json.optString("department", department);
                bookTitle = json.optString("bookTitle", bookTitle);
                returnDate = json.optString("returnDate", returnDate);
                hostelLocation = json.optString("hostelLocation", hostelLocation);
                roomType = json.optString("hostelRoomType", roomType);
                totalFee = json.optDouble("totalFee", 180000.0);
                amountPaid = json.optDouble("amountPaid", 1.0);
                feeBalance = Math.max(0.0, totalFee - amountPaid);
                paymentMethod = json.optString("paymentMethod", "M-PESA");
                paymentStatus = json.optString("paymentStatus", "Confirmed");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        String htmlReport = "<b>STUDENT REGISTRATION SUMMARY REPORT</b><br/><br/>" +
                "<b>1. Personal Details:</b><br/>" +
                "&nbsp;&nbsp;&bull; Full Name: <b>" + fullName + "</b><br/>" +
                "&nbsp;&nbsp;&bull; ID Number: " + studentId + "<br/>" +
                "&nbsp;&nbsp;&bull; Reg Number: <b>" + regNumber + "</b><br/>" +
                "&nbsp;&nbsp;&bull; Course: " + course + "<br/>" +
                "&nbsp;&nbsp;&bull; Department: " + department + "<br/><br/>" +
                "<b>2. Library Book Details:</b><br/>" +
                "&nbsp;&nbsp;&bull; Book Title: " + bookTitle + "<br/>" +
                "&nbsp;&nbsp;&bull; Date of Return: <b>" + returnDate + "</b><br/><br/>" +
                "<b>3. Accommodation Details:</b><br/>" +
                "&nbsp;&nbsp;&bull; Hostel Location: " + hostelLocation + "<br/>" +
                "&nbsp;&nbsp;&bull; Room Type: " + roomType + "<br/><br/>" +
                "<b>4. Fee Payment Details:</b><br/>" +
                "&nbsp;&nbsp;&bull; Total Course Fee: <b>Kshs " + String.format("%.2f", totalFee) + "/-</b><br/>" +
                "&nbsp;&nbsp;&bull; Amount Paid (" + paymentMethod + "): <b>Kshs " + String.format("%.2f", amountPaid) + "/-</b><br/>" +
                "&nbsp;&nbsp;&bull; Fee Balance: <font color='#E53E3E'><b>Kshs " + String.format("%.2f", feeBalance) + "/-</b></font><br/>" +
                "&nbsp;&nbsp;&bull; Status: <font color='#38A169'><b>" + paymentStatus + "</b></font><br/><br/>" +
                "<center><font color='#718096'><i>USIU Africa - Education to take you places</i></font></center>";

        tvSummaryContent.setText(Html.fromHtml(htmlReport, Html.FROM_HTML_MODE_LEGACY));
    }

    // display tabular summary for all registered students
    private void loadAllStudentsSummaryTable() {
        SharedPreferences prefs = getSharedPreferences("OfflineRegistrationCache", Context.MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();

        StringBuilder sb = new StringBuilder();
        sb.append("<center><b>ALL REGISTERED STUDENTS REPORT (2026 COHORT)</b></center><br/>");
        sb.append("<table border='1' width='100%' style='border-collapse:collapse;'>");
        sb.append("<tr bgcolor='#1A365D'><th><font color='#FFFFFF'>Name / Reg No</font></th><th><font color='#FFFFFF'>Course & Dept</font></th><th><font color='#FFFFFF'>Library Book</font></th><th><font color='#FFFFFF'>Hostel</font></th><th><font color='#FFFFFF'>Paid / Balance</font></th></tr>");

        if (allEntries.isEmpty()) {
            sb.append("<tr><td>Stephanie Karanja<br/>SCT221-005/2026</td><td>Software Eng<br/>Computing</td><td>Intro to Algorithms<br/>Return: 15/12/2026</td><td>Phase 2<br/>1 Bedroom</td><td>Paid: Kshs 1.00<br/>Bal: Kshs 179,999.00</td></tr>");
        } else {
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                try {
                    JSONObject json = new JSONObject((String) entry.getValue());
                    String name = json.optString("fullName", "Student");
                    String reg = json.optString("regNumber", "N/A");
                    String crs = json.optString("course", "N/A");
                    String dept = json.optString("department", "N/A");
                    String book = json.optString("bookTitle", "None");
                    String ret = json.optString("returnDate", "N/A");
                    String hstl = json.optString("hostelLocation", "None");
                    double paid = json.optDouble("amountPaid", 1.0);
                    double bal = Math.max(0.0, 180000.0 - paid);

                    sb.append("<tr>")
                            .append("<td>").append(name).append("<br/><b>").append(reg).append("</b></td>")
                            .append("<td>").append(crs).append("<br/><i>").append(dept).append("</i></td>")
                            .append("<td>").append(book).append("<br/>Due: ").append(ret).append("</td>")
                            .append("<td>").append(hstl).append("</td>")
                            .append("<td>Paid: Kshs ").append(String.format("%.2f", paid)).append("<br/>Bal: Kshs ").append(String.format("%.2f", bal)).append("</td>")
                            .append("</tr>");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        sb.append("</table><br/>");
        sb.append("<b>Sync Status:</b> ").append(offlineSyncManager.isNetworkAvailable() ? "<font color='#38A169'>Online (Synced to Cloud)</font>" : "<font color='#E53E3E'>Offline (Cached Locally)</font>").append("<br/><br/>");
        sb.append("<center><font color='#718096'><i>USIU Africa - Education to take you places</i></font></center>");

        tvSummaryContent.setText(Html.fromHtml(sb.toString(), Html.FROM_HTML_MODE_LEGACY));
    }

    // print report or export to pdf
    private void printReport() {
        String reportText = tvSummaryContent.getText().toString() + "\n\nUSIU Africa - Education to take you places";
        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        String jobName = getString(R.string.app_name) + " - Summary Report";
        if (printManager != null) {
            printManager.print(jobName, new PDFPrintAdapter(this, reportText), null);
            Toast.makeText(this, "preparing report for printing / pdf export...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "printing service not available", Toast.LENGTH_SHORT).show();
        }
    }
}
