package com.example.universityregistrationapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.universityregistration.R;
import com.example.universityregistration.activities.RegistrationActivity;
import com.example.universityregistration.activities.SummaryActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button btnStartRegistration = findViewById(R.id.btnStartRegistration);
        Button btnViewSummary = findViewById(R.id.btnViewSummary);

        btnStartRegistration.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, RegistrationActivity.class)));
        btnViewSummary.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SummaryActivity.class)));
    }
}
