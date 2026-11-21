package com.example.universityregistration.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.universityregistration.R;
import com.example.universityregistration.models.HostelBooking;
import com.example.universityregistration.database.FirebaseHelper;
import org.json.JSONObject;
import java.util.Map;

public class HostelActivity extends AppCompatActivity {

    private static final String TAG = "HostelActivity";

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

        SharedPreferences prefs = getSharedPreferences("OfflineRegistrationCache", Context.MODE_PRIVATE);
        String studentId = getIntent().getStringExtra("STUDENT_ID");
        if (studentId == null || studentId.isEmpty()) {
            studentId = prefs.getString("LATEST_STUDENT_ID", "");
        }
        if (!studentId.isEmpty()) {
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

            // safely merge into existing student profile in local cache without losing personal details
            try {
                JSONObject json = getOrCreateStudentJson(prefs, sId);
                json.put("hostelLocation", location);
                json.put("hostelRoomType", roomType);
                json.put("isSharable", isSharable ? "sharable" : "non-sharable");
                String jsonStr = json.toString();

                String regNum = json.optString("regNumber", "");
                String realId = json.optString("idNumber", sId);
                SharedPreferences.Editor editor = prefs.edit()
                        .putString(sId, jsonStr)
                        .putString(realId, jsonStr)
                        .putString("STUDENT_" + realId, jsonStr);
                if (!regNum.isEmpty()) {
                    editor.putString(regNum, jsonStr);
                }
                editor.apply();
            } catch (Exception e) {
                Log.e(TAG, "error saving hostel booking to local prefs: " + e.getMessage());
            }

            Toast.makeText(this, "hostel accommodation booking saved successfully!", Toast.LENGTH_SHORT).show();

            // navigate to payment activity
            Intent intent = new Intent(HostelActivity.this, PaymentActivity.class);
            intent.putExtra("STUDENT_ID", sId);
            startActivity(intent);
        });
    }

    private JSONObject getOrCreateStudentJson(SharedPreferences prefs, String studentId) {
        String jsonStr = prefs.getString(studentId, null);
        if (jsonStr == null || !jsonStr.contains("fullName")) {
            jsonStr = prefs.getString("STUDENT_" + studentId, null);
        }
        if (jsonStr == null || !jsonStr.contains("fullName")) {
            String latestId = prefs.getString("LATEST_STUDENT_ID", "");
            if (!latestId.isEmpty()) {
                jsonStr = prefs.getString(latestId, null);
            }
        }
        if (jsonStr == null || !jsonStr.contains("fullName")) {
            Map<String, ?> all = prefs.getAll();
            for (Map.Entry<String, ?> entry : all.entrySet()) {
                if (entry.getValue() instanceof String) {
                    String str = (String) entry.getValue();
                    if (str.contains("fullName") && str.contains("regNumber")) {
                        jsonStr = str;
                        break;
                    }
                }
            }
        }
        try {
            if (jsonStr != null && !jsonStr.isEmpty()) {
                return new JSONObject(jsonStr);
            }
        } catch (Exception e) {
            Log.e(TAG, "error parsing student json: " + e.getMessage());
        }
        return new JSONObject();
    }
}
