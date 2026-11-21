package com.example.universityregistration.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.print.PrintManager;
import android.text.Html;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.universityregistration.R;
import com.example.universityregistration.database.OfflineSyncManager;
import com.example.universityregistration.utils.PDFPrintAdapter;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SummaryActivity extends AppCompatActivity {

    private static final String TAG = "SummaryActivity";

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

        // load student id if passed via intent or latest registered student
        SharedPreferences prefs = getSharedPreferences("OfflineRegistrationCache", Context.MODE_PRIVATE);
        String passedId = getIntent().getStringExtra("STUDENT_ID");
        if (passedId == null || passedId.isEmpty()) {
            passedId = prefs.getString("LATEST_STUDENT_ID", "");
        }

        if (!passedId.isEmpty()) {
            etSearchStudentId.setText(passedId);
            loadIndividualSummary(passedId);
        } else {
            loadAllStudentsSummaryTable();
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
    private void loadIndividualSummary(String searchKey) {
        SharedPreferences prefs = getSharedPreferences("OfflineRegistrationCache", Context.MODE_PRIVATE);
        JSONObject json = findStudentJsonRecord(prefs, searchKey);

        if (json == null) {
            tvSummaryContent.setText(Html.fromHtml("<b><font color='#E53E3E'>no student registration record found for search query: " + searchKey + "</font></b><br/><br/>please register a student first or enter a valid student id / reg number.<br/><br/><center><font color='#718096'><i>USIU Africa - Education to take you places</i></font></center>", Html.FROM_HTML_MODE_LEGACY));
            return;
        }

        try {
            String fullName = json.optString("fullName", "N/A");
            String idNumber = json.optString("idNumber", searchKey);
            String regNumber = json.optString("regNumber", "N/A");
            String course = json.optString("course", "N/A");
            String department = json.optString("department", "N/A");
            String bookTitle = json.optString("bookTitle", "none requested");
            String returnDate = json.optString("returnDate", "n/a");
            String hostelLocation = json.optString("hostelLocation", "none booked");
            String roomType = json.optString("hostelRoomType", "n/a");
            double totalFee = json.optDouble("totalFee", 180000.0);
            double amountPaid = json.optDouble("amountPaid", 0.0);
            double feeBalance = Math.max(0.0, totalFee - amountPaid);
            String paymentMethod = json.optString("paymentMethod", "pending");
            String paymentStatus = json.optString("paymentStatus", "pending");

            String htmlReport = "<b>STUDENT REGISTRATION SUMMARY REPORT</b><br/><br/>" +
                    "<b>1. Personal Details:</b><br/>" +
                    "&nbsp;&nbsp;&bull; Full Name: <b>" + fullName + "</b><br/>" +
                    "&nbsp;&nbsp;&bull; ID Number: " + idNumber + "<br/>" +
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
                    "&nbsp;&nbsp;&bull; Total Course Fee: <b>Kshs " + String.format(Locale.US, "%.2f", totalFee) + "/-</b><br/>" +
                    "&nbsp;&nbsp;&bull; Amount Paid (" + paymentMethod + "): <b>Kshs " + String.format(Locale.US, "%.2f", amountPaid) + "/-</b><br/>" +
                    "&nbsp;&nbsp;&bull; Fee Balance: <font color='#E53E3E'><b>Kshs " + String.format(Locale.US, "%.2f", feeBalance) + "/-</b></font><br/>" +
                    "&nbsp;&nbsp;&bull; Status: <font color='#38A169'><b>" + paymentStatus + "</b></font><br/><br/>" +
                    "<center><font color='#718096'><i>USIU Africa - Education to take you places</i></font></center>";

            tvSummaryContent.setText(Html.fromHtml(htmlReport, Html.FROM_HTML_MODE_LEGACY));
        } catch (Exception e) {
            Log.e(TAG, "error parsing student json summary: " + e.getMessage());
            tvSummaryContent.setText("error displaying student record details.");
        }
    }

    // display tabular summary for all registered students
    private void loadAllStudentsSummaryTable() {
        SharedPreferences prefs = getSharedPreferences("OfflineRegistrationCache", Context.MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();

        StringBuilder sb = new StringBuilder();
        sb.append("<center><b>ALL REGISTERED STUDENTS REPORT (2026 COHORT)</b></center><br/>");
        sb.append("<table border='1' width='100%' style='border-collapse:collapse;'>");
        sb.append("<tr bgcolor='#1A365D'><th><font color='#FFFFFF'>Name / Reg No</font></th><th><font color='#FFFFFF'>Course & Dept</font></th><th><font color='#FFFFFF'>Library Book</font></th><th><font color='#FFFFFF'>Hostel</font></th><th><font color='#FFFFFF'>Paid / Balance</font></th></tr>");

        Set<String> processedStudentIds = new HashSet<>();
        int count = 0;

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if ("LATEST_STUDENT_ID".equals(entry.getKey())) continue;
            try {
                Object val = entry.getValue();
                if (!(val instanceof String) || !((String) val).startsWith("{")) continue;

                JSONObject json = new JSONObject((String) val);
                String idNum = json.optString("idNumber", "");
                String fullName = json.optString("fullName", "");
                if (idNum.isEmpty() || fullName.isEmpty() || processedStudentIds.contains(idNum)) {
                    continue;
                }
                processedStudentIds.add(idNum);
                count++;

                String reg = json.optString("regNumber", "N/A");
                String crs = json.optString("course", "N/A");
                String dept = json.optString("department", "N/A");
                String book = json.optString("bookTitle", "None");
                String ret = json.optString("returnDate", "N/A");
                String hstl = json.optString("hostelLocation", "None");
                double paid = json.optDouble("amountPaid", 0.0);
                double total = json.optDouble("totalFee", 180000.0);
                double bal = Math.max(0.0, total - paid);

                sb.append("<tr>")
                        .append("<td>").append(fullName).append("<br/><b>").append(reg).append("</b></td>")
                        .append("<td>").append(crs).append("<br/><i>").append(dept).append("</i></td>")
                        .append("<td>").append(book).append("<br/>Due: ").append(ret).append("</td>")
                        .append("<td>").append(hstl).append("</td>")
                        .append("<td>Paid: Kshs ").append(String.format(Locale.US, "%.2f", paid)).append("<br/>Bal: Kshs ").append(String.format(Locale.US, "%.2f", bal)).append("</td>")
                        .append("</tr>");
            } catch (Exception e) {
                Log.e(TAG, "error parsing student record for table: " + e.getMessage());
            }
        }

        if (count == 0) {
            sb.append("<tr><td colspan='5' align='center'><i>no registered students found in database.</i></td></tr>");
        }

        sb.append("</table><br/>");
        sb.append("<b>Sync Status:</b> ").append(offlineSyncManager.isNetworkAvailable() ? "<font color='#38A169'>Online (Synced to Cloud)</font>" : "<font color='#E53E3E'>Offline (Cached Locally)</font>").append("<br/><br/>");
        sb.append("<center><font color='#718096'><i>USIU Africa - Education to take you places</i></font></center>");

        tvSummaryContent.setText(Html.fromHtml(sb.toString(), Html.FROM_HTML_MODE_LEGACY));
    }

    // find student record json matching ID or Reg Number from local preferences
    private JSONObject findStudentJsonRecord(SharedPreferences prefs, String query) {
        if (query == null || query.trim().isEmpty()) return null;

        String jsonStr = prefs.getString(query, null);
        if (jsonStr == null) {
            jsonStr = prefs.getString("STUDENT_" + query, null);
        }
        if (jsonStr == null) {
            Map<String, ?> all = prefs.getAll();
            for (Map.Entry<String, ?> entry : all.entrySet()) {
                if (entry.getValue() instanceof String) {
                    String str = (String) entry.getValue();
                    if (str.startsWith("{") && (str.contains("\"idNumber\":\"" + query) || str.contains("\"regNumber\":\"" + query))) {
                        jsonStr = str;
                        break;
                    }
                }
            }
        }
        if (jsonStr == null) {
            String latestId = prefs.getString("LATEST_STUDENT_ID", "");
            if (!latestId.isEmpty()) {
                jsonStr = prefs.getString(latestId, null);
            }
        }

        try {
            if (jsonStr != null && jsonStr.startsWith("{")) {
                return new JSONObject(jsonStr);
            }
        } catch (Exception e) {
            Log.e(TAG, "error finding student record: " + e.getMessage());
        }
        return null;
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
