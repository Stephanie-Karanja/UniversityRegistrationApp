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
import com.example.universityregistration.models.HostelBooking;
import com.example.universityregistration.database.FirebaseHelper;

public class HostelActivity extends AppCompatActivity {

    // ui views for hostel accommodation booking
    private EditText etStudentId;
    private Spinner spinnerLocation, spinnerRoomType, spinnerSharable;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hostel);

        // bind xml layout elements
        etStudentId = findViewById(R.id.etStudentId);
        spinnerLocation = findViewById(R.id.spinnerLocation);
        spinnerRoomType = findViewById(R.id.spinnerRoomType);
        spinnerSharable = findViewById(R.id.spinnerSharable);
        Button btnProceedPayment = findViewById(R.id.btnProceedPayment);
        Button btnBack = findViewById(R.id.btnBack);

        firebaseHelper = new FirebaseHelper();

        // retrieve student id from previous intent
        String studentId = getIntent().getStringExtra("STUDENT_ID");
        if (studentId != null) {
            etStudentId.setText(studentId);
        }

        // populate hostel location spinner
        String[] locations = {"phase 1 hostel", "phase 2 hostel", "phase 3 hostel", "graduate court"};
        ArrayAdapter<String> locAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, locations);
        locAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLocation.setAdapter(locAdapter);

        // populate room type spinner
        String[] roomTypes = {"bedsitter", "1 bedroom", "2 bedroom shared", "studio"};
        ArrayAdapter<String> roomAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roomTypes);
        roomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRoomType.setAdapter(roomAdapter);

        // populate sharable status spinner
        String[] sharableOptions = {"non-sharable (single)", "sharable (double occupancy)"};
        ArrayAdapter<String> sharableAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sharableOptions);
        sharableAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSharable.setAdapter(sharableAdapter);

        // handle back button click
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // handle proceed button click
        btnProceedPayment.setOnClickListener(v -> {
            String sId = etStudentId.getText().toString().trim();
            String location = spinnerLocation.getSelectedItem().toString();
            String roomType = spinnerRoomType.getSelectedItem().toString();
            boolean isSharable = spinnerSharable.getSelectedItem().toString().contains("sharable (double");

            // validate student id
            if (sId.isEmpty()) {
                Toast.makeText(this, "please enter student id number manually", Toast.LENGTH_SHORT).show();
                return;
            }

            // create hostel booking object and save to firebase
            HostelBooking booking = new HostelBooking(sId, location, roomType, isSharable);
            firebaseHelper.saveHostelBooking(booking);

            Toast.makeText(this, "hostel accommodation booking saved successfully!", Toast.LENGTH_SHORT).show();

            // navigate to payment activity
            Intent intent = new Intent(HostelActivity.this, PaymentActivity.class);
            intent.putExtra("STUDENT_ID", sId);
            startActivity(intent);
        });
    }
}
