package com.example.universityregistration.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.universityregistration.R;
import com.example.universityregistration.database.FirebaseHelper;
import com.example.universityregistration.models.PaymentTransaction;
import com.example.universityregistration.utils.MpesaPaybill;

public class PaymentActivity extends AppCompatActivity {

    // ui views for m-pesa payment
    private EditText etPhoneNumber, etAmount;
    private TextView tvPaymentStatus;
    private Button btnPayMpesa;
    private FirebaseHelper firebaseHelper;
    private String studentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        // bind views from xml
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etAmount = findViewById(R.id.etAmount);
        btnPayMpesa = findViewById(R.id.btnPayMpesa);
        Button btnViewSummary = findViewById(R.id.btnViewSummary);
        Button btnBack = findViewById(R.id.btnBack);
        tvPaymentStatus = findViewById(R.id.tvPaymentStatus);

        firebaseHelper = new FirebaseHelper();
        studentId = getIntent().getStringExtra("STUDENT_ID");
        if (studentId == null) {
            studentId = "674735"; // default student id fallback
        }

        // handle back button click
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // handle m-pesa payment button click
        btnPayMpesa.setOnClickListener(v -> startPayment());

        // navigate to summary reports activity
        btnViewSummary.setOnClickListener(v -> {
            Intent intent = new Intent(PaymentActivity.this, SummaryActivity.class);
            intent.putExtra("STUDENT_ID", studentId);
            startActivity(intent);
        });
    }

    private void startPayment() {
        final String phone = etPhoneNumber.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();

        // validate phone and amount
        if (phone.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter phone number and fee amount", Toast.LENGTH_SHORT).show();
            return;
        }

        final double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            etAmount.setError("Enter a valid amount");
            return;
        }
        if (amount < 1 || amount > 150000) {
            etAmount.setError("Amount must be KSh 1 to 150,000");
            return;
        }

        // show progress while waiting for the student to enter their PIN
        btnPayMpesa.setEnabled(false);
        tvPaymentStatus.setTextColor(getResources().getColor(android.R.color.darker_gray));
        tvPaymentStatus.setText("Status: sending M-PESA prompt. Enter your PIN on your phone...");

        final String refId = studentId;

        MpesaPaybill.triggerStkPush(phone, amount, refId, new MpesaPaybill.MpesaCallback() {
            @Override
            public void onSuccess(String transactionId) {
                if (isFinishing() || isDestroyed()) return;

                btnPayMpesa.setEnabled(true);
                tvPaymentStatus.setText("Status: Payment accepted ✔");
                tvPaymentStatus.setTextColor(getResources().getColor(R.color.success_green));

                // save confirmed payment transaction to firebase
                PaymentTransaction tx = new PaymentTransaction(
                        refId, phone, amount, transactionId, "confirmed");
                firebaseHelper.savePaymentTransaction(tx);

                new AlertDialog.Builder(PaymentActivity.this)
                        .setTitle("Payment accepted")
                        .setMessage("Your payment of KSh " + (int) amount + " was received successfully.")
                        .setPositiveButton("OK", null)
                        .show();
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) return;

                btnPayMpesa.setEnabled(true);
                tvPaymentStatus.setText("Status: Payment failed - " + message);
                tvPaymentStatus.setTextColor(getResources().getColor(R.color.error_red));
            }
        });
    }
}