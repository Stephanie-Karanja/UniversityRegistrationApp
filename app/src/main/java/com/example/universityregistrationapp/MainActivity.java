package com.example.universityregistrationapp;
import com.example.universityregistration.R;
import android.os.Bundle;
import android.content.Intent;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.universityregistration.activities.PaymentActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        TextView registrationStatus = findViewById(R.id.registrationStatusText);
        ActivityResultLauncher<Intent> paymentLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                Intent data = result.getData();
                if (result.getResultCode() == RESULT_OK && data != null
                    && data.getBooleanExtra("paymentConfirmed", false)) {
                    String receipt = data.getStringExtra("transactionId");
                    registrationStatus.setText("Payment confirmed by Safaricom. Receipt: " + receipt);
                }
            });
        Button paymentButton = findViewById(R.id.paymentButton);
        paymentButton.setOnClickListener(view ->
            paymentLauncher.launch(new Intent(MainActivity.this, PaymentActivity.class)));
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}