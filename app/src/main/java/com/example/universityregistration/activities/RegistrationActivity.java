package com.example.universityregistration.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.universityregistration.R;
import com.example.universityregistration.models.Student;
import com.example.universityregistration.database.FirebaseHelper;
import com.example.universityregistration.database.OfflineSyncManager;
import org.json.JSONObject;

public class RegistrationActivity extends AppCompatActivity {

    private static final String TAG = "RegistrationActivity";

    // ui elements for student registration
    private EditText etFullName, etIdNumber, etRegNumber;
    private RadioGroup rgGender;
    private Spinner spinnerCourse, spinnerDepartment;
    private FirebaseHelper firebaseHelper;
    private OfflineSyncManager offlineSyncManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        // bind xml views
        etFullName = findViewById(R.id.etFullName);
        etIdNumber = findViewById(R.id.etIdNumber);
        etRegNumber = findViewById(R.id.etRegNumber);
        rgGender = findViewById(R.id.rgGender);
        spinnerCourse = findViewById(R.id.spinnerCourse);
        spinnerDepartment = findViewById(R.id.spinnerDepartment);
        Button btnNext = findViewById(R.id.btnNext);
        Button btnBack = findViewById(R.id.btnBack);

        // initialize database and offline sync helpers
        firebaseHelper = new FirebaseHelper();
        offlineSyncManager = new OfflineSyncManager(this);

        // populate course dropdown list with usiu courses
        String[] courses = {
                "Software Engineering",
                "International Relations",
                "Psychology",
                "Accounting",
                "Finance",
                "Data Science & Analytics",
                "Artificial Intelligence & Robotics",
                "International Business Administration",
                "Journalism",
                "Pharmacy"
        };
        ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, courses);
        courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCourse.setAdapter(courseAdapter);

        // populate department dropdown list with usiu departments
        String[] departments = {
                "Business Administration",
                "Finance & Accounting",
                "Computer Science & Technology",
                "Software Engineering",
                "Data Science & Artificial Intelligence",
                "Humanities & Social Sciences",
                "International Relations",
                "Communication & Journalism",
                "Pharmacy & Health Sciences",
                "Film, Animation & Creative Arts"
        };
        ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, departments);
        deptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDepartment.setAdapter(deptAdapter);

        // handle back button click
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // handle next button click to validate and proceed
        btnNext.setOnClickListener(v -> {
            String fullName = etFullName.getText().toString().trim();
            String idNumber = etIdNumber.getText().toString().trim();
            String regNumber = etRegNumber.getText().toString().trim();

            // validate required text fields
            if (fullName.isEmpty() || idNumber.isEmpty() || regNumber.isEmpty()) {
                Toast.makeText(this, "please fill in all registration fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // validate gender selection
            int selectedGenderId = rgGender.getCheckedRadioButtonId();
            if (selectedGenderId == -1) {
                Toast.makeText(this, "please select gender", Toast.LENGTH_SHORT).show();
                return;
            }
            RadioButton rbGender = findViewById(selectedGenderId);
            String gender = rbGender.getText().toString();

            // get selected course and department from spinners
            String course = spinnerCourse.getSelectedItem().toString();
            String department = spinnerDepartment.getSelectedItem().toString();

            // create student model object
            Student student = new Student(fullName, idNumber, regNumber, gender, course, department);

            // save data to firebase
            firebaseHelper.saveStudentRegistration(student);

            // save consolidated student profile to local shared preferences under ID and Reg Number
            try {
                SharedPreferences prefs = getSharedPreferences("OfflineRegistrationCache", Context.MODE_PRIVATE);
                JSONObject json = new JSONObject();
                json.put("fullName", fullName);
                json.put("idNumber", idNumber);
                json.put("regNumber", regNumber);
                json.put("gender", gender);
                json.put("course", course);
                json.put("department", department);
                json.put("totalFee", 180000.0);
                json.put("amountPaid", 0.0);
                json.put("feeBalance", 180000.0);
                json.put("paymentMethod", "pending");
                json.put("paymentStatus", "pending");
                json.put("bookTitle", "none requested");
                json.put("returnDate", "n/a");
                json.put("hostelLocation", "none booked");
                json.put("hostelRoomType", "n/a");

                String jsonStr = json.toString();
                prefs.edit()
                        .putString(idNumber, jsonStr)
                        .putString(regNumber, jsonStr)
                        .putString("STUDENT_" + idNumber, jsonStr)
                        .putString("LATEST_STUDENT_ID", idNumber)
                        .apply();

                offlineSyncManager.cacheDataLocally(idNumber, jsonStr);
            } catch (Exception e) {
                Log.e(TAG, "error saving student registration to local prefs: " + e.getMessage());
            }

            Toast.makeText(this, "student registered successfully!", Toast.LENGTH_SHORT).show();

            // navigate to library activity
            Intent intent = new Intent(RegistrationActivity.this, LibraryActivity.class);
            intent.putExtra("STUDENT_ID", idNumber);
            startActivity(intent);
        });
    }
}
