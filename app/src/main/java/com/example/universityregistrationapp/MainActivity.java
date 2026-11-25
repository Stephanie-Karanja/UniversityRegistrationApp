package com.example.universityregistrationapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.example.universityregistration.R;
import com.example.universityregistration.activities.RegistrationActivity;
import com.example.universityregistration.activities.SummaryActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // bind main menu navigation buttons
        Button btnStartRegistration = findViewById(R.id.btnStartRegistration);
        Button btnViewSummary = findViewById(R.id.btnViewSummary);

        // launch student registration workflow
        btnStartRegistration.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, RegistrationActivity.class)));

        // launch summary reports
        btnViewSummary.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SummaryActivity.class)));
    }
}
