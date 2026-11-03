package com.example.universityregistration.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.universityregistration.models.Student;
import com.example.universityregistrationapp.R;

public class RegistrationActivity extends AppCompatActivity {

    private EditText etFullNames, etIdNo, etRegNo;
    private RadioGroup rgGender;
    private Spinner spCourse, spDepartment;
    private Button btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        etFullNames = findViewById(R.id.etFullNames);
        etIdNo = findViewById(R.id.etIdNo);
        etRegNo = findViewById(R.id.etRegNo);
        rgGender = findViewById(R.id.rgGender);
        spCourse = findViewById(R.id.spCourse);
        spDepartment = findViewById(R.id.spDepartment);
        btnNext = findViewById(R.id.btnNext);

        ArrayAdapter<CharSequence> courseAdapter = ArrayAdapter.createFromResource(
                this, R.array.courses, android.R.layout.simple_spinner_item);
        courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCourse.setAdapter(courseAdapter);

        ArrayAdapter<CharSequence> deptAdapter = ArrayAdapter.createFromResource(
                this, R.array.departments, android.R.layout.simple_spinner_item);
        deptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDepartment.setAdapter(deptAdapter);

        btnNext.setOnClickListener(v -> validateAndProceed());
    }

    private void validateAndProceed() {
        String fullName = etFullNames.getText().toString().trim();
        String idNumber = etIdNo.getText().toString().trim();
        String regNumber = etRegNo.getText().toString().trim();

        if (fullName.isEmpty()) {
            etFullNames.setError("Full names are required");
            etFullNames.requestFocus();
            return;
        }
        if (!idNumber.matches("\\d{7,8}")) {
            etIdNo.setError("Enter a valid ID number (7-8 digits)");
            etIdNo.requestFocus();
            return;
        }
        if (regNumber.isEmpty()) {
            etRegNo.setError("Registration number is required");
            etRegNo.requestFocus();
            return;
        }

        int selectedId = rgGender.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(this, "Please select your gender", Toast.LENGTH_SHORT).show();
            return;
        }
        String gender = ((RadioButton) findViewById(selectedId)).getText().toString();

        if (spCourse.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select your course", Toast.LENGTH_SHORT).show();
            return;
        }
        String course = spCourse.getSelectedItem().toString();

        if (spDepartment.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select your department", Toast.LENGTH_SHORT).show();
            return;
        }
        String department = spDepartment.getSelectedItem().toString();

        Student student = new Student(fullName, idNumber, regNumber, gender, course, department);

        Intent intent = new Intent(RegistrationActivity.this, LibraryActivity.class);
        intent.putExtra("student", student);
        startActivity(intent);
    }
}