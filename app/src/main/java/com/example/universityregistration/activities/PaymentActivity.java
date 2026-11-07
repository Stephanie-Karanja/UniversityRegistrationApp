package com.example.universityregistration.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.universityregistration.R;
import com.example.universityregistration.models.PaymentTransaction;
import com.example.universityregistration.database.FirebaseHelper;
import com.example.universityregistration.utils.MpesaPaybill;

public class PaymentActivity extends AppCompatActivity {

    // ui views for m-pesa payment
    private EditText etPhoneNumber, etAmount;
    private TextView tvPaymentStatus;
    private FirebaseHelper firebaseHelper;
    private String studentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        // bind views from xml
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etAmount = findViewById(R.id.etAmount);
        Button btnPayMpesa = findViewById(R.id.btnPayMpesa);
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
        btnPayMpesa.setOnClickListener(v -> {
            String phone = etPhoneNumber.getText().toString().trim();
            String amountStr = etAmount.getText().toString().trim();

            // validate phone and amount
            if (phone.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(this, "please enter phone number and fee amount", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountStr);
            Toast.makeText(this, "sending m-pesa stk push to " + phone + "...", Toast.LENGTH_SHORT).show();

            // trigger m-pesa paybill simulation
            MpesaPaybill.triggerStkPush(phone, amount, new MpesaPaybill.MpesaCallback() {
                @Override
                public void onSuccess(String transactionId) {
                    tvPaymentStatus.setText("status: payment confirmed (m-pesa)");
                    tvPaymentStatus.setTextColor(getResources().getColor(R.color.success_green));

                    // save confirmed payment transaction to firebase
                    PaymentTransaction tx = new PaymentTransaction(studentId, phone, amount, transactionId, "confirmed");
                    firebaseHelper.savePaymentTransaction(tx);

                    Toast.makeText(PaymentActivity.this, "m-pesa payment confirmed! registration status updated.", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onError(String message) {
                    tvPaymentStatus.setText("status: payment failed");
                    tvPaymentStatus.setTextColor(getResources().getColor(R.color.error_red));
                    Toast.makeText(PaymentActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
        });

        // navigate to summary reports activity
        btnViewSummary.setOnClickListener(v -> {
            Intent intent = new Intent(PaymentActivity.this, SummaryActivity.class);
            intent.putExtra("STUDENT_ID", studentId);
            startActivity(intent);
        });
    }
}
