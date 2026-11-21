package com.example.universityregistration.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.universityregistration.R;
import com.example.universityregistration.models.LibraryRequest;
import com.example.universityregistration.database.FirebaseHelper;
import org.json.JSONObject;

public class LibraryActivity extends AppCompatActivity {

    private static final String TAG = "LibraryActivity";

    // ui views for library book request
    private EditText etStudentId;
    private Spinner spinnerIsbn, spinnerAuthor, spinnerTitle, spinnerDateBorrowed, spinnerDateReturn;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        // bind views from xml layout
        etStudentId = findViewById(R.id.etStudentId);
        spinnerIsbn = findViewById(R.id.spinnerIsbn);
        spinnerAuthor = findViewById(R.id.spinnerAuthor);
        spinnerTitle = findViewById(R.id.spinnerTitle);
        spinnerDateBorrowed = findViewById(R.id.spinnerDateBorrowed);
        spinnerDateReturn = findViewById(R.id.spinnerDateReturn);
        Button btnProceedHostel = findViewById(R.id.btnProceedHostel);
        Button btnBack = findViewById(R.id.btnBack);

        firebaseHelper = new FirebaseHelper();

        SharedPreferences prefs = getSharedPreferences("OfflineRegistrationCache", Context.MODE_PRIVATE);
        String studentId = getIntent().getStringExtra("STUDENT_ID");
        if (studentId == null || studentId.isEmpty()) {
            studentId = prefs.getString("LATEST_STUDENT_ID", "");
        }
        if (!studentId.isEmpty()) {
            etStudentId.setText(studentId);
        }

        // 10 university textbook examples
        String[] titles = {
                "Software Engineering: A Practitioner's Approach",
                "Introduction to International Relations: Theories and Approaches",
                "Introduction to Psychology",
                "Accounting Principles",
                "Principles of Corporate Finance",
                "Data Science for Business",
                "Artificial Intelligence: A Modern Approach",
                "International Business",
                "The Elements of Journalism",
                "Pharmacotherapy: A Pathophysiologic Approach"
        };

        String[] authors = {
                "Roger S. Pressman & Bruce R. Maxim",
                "Robert Jackson & Georg Sørensen",
                "James W. Kalat",
                "Jerry J. Weygandt, Paul D. Kimmel & Donald E. Kieso",
                "Richard A. Brealey, Stewart C. Myers & Franklin Allen",
                "Foster Provost & Tom Fawcett",
                "Stuart Russell & Peter Norvig",
                "Mike W. Peng & Klaus Meyer",
                "Bill Kovach & Tom Rosenstiel",
                "Joseph T. DiPiro et al."
        };

        String[] isbns = {
                "978-1259872976",
                "978-0198825548",
                "978-1305271555",
                "978-1119491002",
                "978-1260013900",
                "978-1449361327",
                "978-0134610993",
                "978-1473758438",
                "978-0451497024",
                "978-1260115680"
        };

        // populate spinners
        ArrayAdapter<String> titleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, titles);
        titleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTitle.setAdapter(titleAdapter);

        ArrayAdapter<String> authorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, authors);
        authorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAuthor.setAdapter(authorAdapter);

        ArrayAdapter<String> isbnAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, isbns);
        isbnAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerIsbn.setAdapter(isbnAdapter);

        // sync author and isbn when a book title is selected
        spinnerTitle.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                spinnerAuthor.setSelection(position);
                spinnerIsbn.setSelection(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // populate date borrowed spinner
        String[] borrowDates = {"01/10/2026", "02/10/2026", "03/10/2026", "04/10/2026"};
        ArrayAdapter<String> borrowAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, borrowDates);
        borrowAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDateBorrowed.setAdapter(borrowAdapter);

        // populate return date spinner
        String[] returnDates = {"01/12/2026", "15/12/2026", "31/12/2026", "15/01/2027"};
        ArrayAdapter<String> returnAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, returnDates);
        returnAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDateReturn.setAdapter(returnAdapter);

        // handle back button click
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // handle proceed button click
        btnProceedHostel.setOnClickListener(v -> {
            String sId = etStudentId.getText().toString().trim();
            String isbn = spinnerIsbn.getSelectedItem().toString();
            String author = spinnerAuthor.getSelectedItem().toString();
            String title = spinnerTitle.getSelectedItem().toString();
            String dateBorrowed = spinnerDateBorrowed.getSelectedItem().toString();
            String dateReturn = spinnerDateReturn.getSelectedItem().toString();

            // validate student id input
            if (sId.isEmpty()) {
                Toast.makeText(this, "please enter student id number manually", Toast.LENGTH_SHORT).show();
                return;
            }

            // create library request object and save to firebase
            LibraryRequest request = new LibraryRequest(sId, isbn, title, author, dateBorrowed, dateReturn);
            firebaseHelper.saveLibraryRequest(request);

            // update consolidated profile in local cache
            try {
                String existing = prefs.getString(sId, "{}");
                JSONObject json = new JSONObject(existing);
                json.put("bookTitle", title);
                json.put("returnDate", dateReturn);
                json.put("bookIsbn", isbn);
                json.put("bookAuthor", author);
                String jsonStr = json.toString();

                String regNum = json.optString("regNumber", "");
                SharedPreferences.Editor editor = prefs.edit().putString(sId, jsonStr);
                if (!regNum.isEmpty()) {
                    editor.putString(regNum, jsonStr);
                }
                editor.apply();
            } catch (Exception e) {
                Log.e(TAG, "error saving library request to local prefs: " + e.getMessage());
            }

            Toast.makeText(this, "library book request submitted successfully!", Toast.LENGTH_SHORT).show();

            // navigate to hostel activity
            Intent intent = new Intent(LibraryActivity.this, HostelActivity.class);
            intent.putExtra("STUDENT_ID", sId);
            startActivity(intent);
        });
    }
}
