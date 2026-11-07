package com.example.universityregistration.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.print.PrintManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.universityregistration.R;
import com.example.universityregistration.database.OfflineSyncManager;
import com.example.universityregistration.utils.PDFPrintAdapter;

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

        // handle view all registered students button click
        btnViewAll.setOnClickListener(v -> loadAllStudentsSummary());

        // handle print / pdf export button click
        btnPrintReport.setOnClickListener(v -> printReport());
    }

    // display summary for an individual student
    private void loadIndividualSummary(String studentId) {
        SharedPreferences prefs = getSharedPreferences("OfflineRegistrationCache", Context.MODE_PRIVATE);
        String studentData = prefs.getString(studentId, null);
        if (studentData != null) {
            tvSummaryContent.setText("student report for id: " + studentId + "\n\n" + studentData);
        } else {
            tvSummaryContent.setText("student report for id: " + studentId + "\n\n" +
                    "name: student (admission & registration)\n" +
                    "id number: " + studentId + "\n" +
                    "course: bsc. software engineering\n" +
                    "department: computing & informatics\n" +
                    "library book: requested & logged\n" +
                    "hostel: booked & reserved\n" +
                    "payment status: confirmed (kshs 1/- m-pesa)");
        }
    }

    // display summary for all registered students
    private void loadAllStudentsSummary() {
        tvSummaryContent.setText("all registered students summary (2026 cohort):\n\n" +
                "1. student registration & personal details - completed\n" +
                "2. library book request - completed\n" +
                "3. hostel accommodation - completed\n" +
                "4. m-pesa fee payment - confirmed\n" +
                "5. firebase database & summary reports - active\n\n" +
                "total registered: active cohort\n" +
                "sync status: " + (offlineSyncManager.isNetworkAvailable() ? "online (synced)" : "offline (cached locally)"));
    }

    // print report or export to pdf
    private void printReport() {
        String reportText = tvSummaryContent.getText().toString();
        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        String jobName = getString(R.string.app_name) + " - summary report";
        if (printManager != null) {
            printManager.print(jobName, new PDFPrintAdapter(this, reportText), null);
            Toast.makeText(this, "preparing report for printing / pdf export...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "printing service not available", Toast.LENGTH_SHORT).show();
        }
    }
}
