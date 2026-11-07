package com.example.universityregistration.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.universityregistration.R;
import com.example.universityregistration.models.LibraryRequest;
import com.example.universityregistration.database.FirebaseHelper;

public class LibraryActivity extends AppCompatActivity {

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

        // pre-fill student id if passed from previous activity
        String studentId = getIntent().getStringExtra("STUDENT_ID");
        if (studentId != null) {
            etStudentId.setText(studentId);
        }

        // populate isbn selection spinner
        String[] isbns = {"978-0131103627", "978-1484251266", "978-0321765723", "978-0201633610"};
        ArrayAdapter<String> isbnAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, isbns);
        isbnAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerIsbn.setAdapter(isbnAdapter);

        // populate author selection spinner
        String[] authors = {"cormen et al.", "bill phillips", "brian goetz", "erich gamma"};
        ArrayAdapter<String> authorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, authors);
        authorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAuthor.setAdapter(authorAdapter);

        // populate title selection spinner
        String[] titles = {"introduction to algorithms", "android programming", "java concurrency in practice", "design patterns"};
        ArrayAdapter<String> titleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, titles);
        titleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTitle.setAdapter(titleAdapter);

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

            Toast.makeText(this, "library book request submitted successfully!", Toast.LENGTH_SHORT).show();

            // navigate to hostel activity
            Intent intent = new Intent(LibraryActivity.this, HostelActivity.class);
            intent.putExtra("STUDENT_ID", sId);
            startActivity(intent);
        });
    }
}
