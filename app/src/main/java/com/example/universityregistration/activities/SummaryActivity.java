package com.example.universityregistration.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.print.PrintManager;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
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
    private HorizontalScrollView scrollTableContainer;
    private TableLayout tableAllStudents;
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
        scrollTableContainer = findViewById(R.id.scrollTableContainer);
        tableAllStudents = findViewById(R.id.tableAllStudents);

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

        // handle view all registered students button click (visual grid table)
        btnViewAll.setOnClickListener(v -> loadAllStudentsSummaryTable());

        // handle print / pdf export button click
        btnPrintReport.setOnClickListener(v -> printReport());
    }

    // display neat formatted individual summary for a student
    private void loadIndividualSummary(String searchKey) {
        tvSummaryContent.setVisibility(View.VISIBLE);
        scrollTableContainer.setVisibility(View.GONE);

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

    // display visual grid table for all registered students
    private void loadAllStudentsSummaryTable() {
        tvSummaryContent.setVisibility(View.GONE);
        scrollTableContainer.setVisibility(View.VISIBLE);
        tableAllStudents.removeAllViews();

        // 1. add table header row
        TableRow headerRow = new TableRow(this);
        headerRow.setBackgroundColor(getResources().getColor(R.color.primary_blue));
        headerRow.setPadding(12, 12, 12, 12);

        String[] headers = {"Student Name & Reg No", "Course & Department", "Library Book Details", "Hostel Booking", "Fee Paid / Balance"};
        for (String headerText : headers) {
            TextView tv = new TextView(this);
            tv.setText(headerText);
            tv.setTextColor(getResources().getColor(R.color.card_white));
            tv.setTypeface(null, Typeface.BOLD);
            tv.setTextSize(13f);
            tv.setPadding(16, 12, 16, 12);
            headerRow.addView(tv);
        }
        tableAllStudents.addView(headerRow);

        // 2. populate student rows from shared preferences
        SharedPreferences prefs = getSharedPreferences("OfflineRegistrationCache", Context.MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();
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

                TableRow row = new TableRow(this);
                if (count % 2 == 0) {
                    row.setBackgroundColor(Color.parseColor("#F7FAFC"));
                } else {
                    row.setBackgroundColor(Color.parseColor("#FFFFFF"));
                }
                row.setPadding(8, 8, 8, 8);

                String reg = json.optString("regNumber", "N/A");
                String crs = json.optString("course", "N/A");
                String dept = json.optString("department", "N/A");
                String book = json.optString("bookTitle", "None");
                String ret = json.optString("returnDate", "N/A");
                String hstl = json.optString("hostelLocation", "None");
                double paid = json.optDouble("amountPaid", 0.0);
                double total = json.optDouble("totalFee", 180000.0);
                double bal = Math.max(0.0, total - paid);

                addTableCell(row, fullName + "\n" + reg, true);
                addTableCell(row, crs + "\n(" + dept + ")", false);
                addTableCell(row, book + "\nDue: " + ret, false);
                addTableCell(row, hstl, false);
                addTableCell(row, "Paid: Kshs " + String.format(Locale.US, "%.2f", paid) + "\nBal: Kshs " + String.format(Locale.US, "%.2f", bal), false);

                tableAllStudents.addView(row);
            } catch (Exception e) {
                Log.e(TAG, "error parsing student record for table: " + e.getMessage());
            }
        }

        if (count == 0) {
            TableRow emptyRow = new TableRow(this);
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("no registered students found in database.");
            tvEmpty.setTextColor(getResources().getColor(R.color.text_muted));
            tvEmpty.setPadding(24, 24, 24, 24);
            emptyRow.addView(tvEmpty);
            tableAllStudents.addView(emptyRow);
        }
    }

    // helper method to add text cells to table rows
    private void addTableCell(TableRow row, String text, boolean isBold) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(getResources().getColor(R.color.text_dark));
        tv.setTextSize(12f);
        if (isBold) {
            tv.setTypeface(null, Typeface.BOLD);
        }
        tv.setPadding(16, 12, 16, 12);
        row.addView(tv);
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
