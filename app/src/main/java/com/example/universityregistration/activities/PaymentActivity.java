package com.example.universityregistration.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.InputType;
import android.util.Log;
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

    private static final String TAG = "PaymentActivity";
    private static final int SMS_PERMISSION_CODE = 101;

    // declare ui views for m-pesa payment
    private EditText etPhoneNumber, etAmount;
    private TextView tvPaymentStatus;
    private Button btnPayMpesa;
    private FirebaseHelper firebaseHelper;
    private String studentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        // bind xml layout elements
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etAmount = findViewById(R.id.etAmount);
        btnPayMpesa = findViewById(R.id.btnPayMpesa);
        Button btnViewSummary = findViewById(R.id.btnViewSummary);
        Button btnBack = findViewById(R.id.btnBack);
        tvPaymentStatus = findViewById(R.id.tvPaymentStatus);

        firebaseHelper = new FirebaseHelper();
        SharedPreferences prefs = getSharedPreferences("OfflineRegistrationCache", Context.MODE_PRIVATE);
        studentId = getIntent().getStringExtra("STUDENT_ID");
        if (studentId == null || studentId.isEmpty()) {
            studentId = prefs.getString("LATEST_STUDENT_ID", "");
        }

        // request sms permission if not granted
        checkSmsPermission();

        // handle back button click
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // handle m-pesa payment button click
        btnPayMpesa.setOnClickListener(v -> startPayment());

        // handle negative cancel button click
        builder.setNegativeButton("cancel", (dialog, which) -> {
            dialog.dismiss();
            Toast.makeText(this, "m-pesa payment cancelled", Toast.LENGTH_SHORT).show();
        });

        builder.show();
    }

    // send real sms text message directly to phone using android sms manager
    private void sendMpesaConfirmationSms(String phone, double amount, double feeBalance, String txId) {
        try {
            SmsManager smsManager;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                smsManager = getSystemService(SmsManager.class);
            } else {
                smsManager = SmsManager.getDefault();
            }

            String smsMessage = "M-PESA Confirmed. Ksh" + String.format(Locale.US, "%.2f", amount) +
                    " paid to USIU Paybill 123456. Transaction ID: " + txId +
                    ". Fee Balance: Ksh" + String.format(Locale.US, "%.2f", feeBalance) + ".";

            smsManager.sendTextMessage(phone, null, smsMessage, null, null);
            Log.d(TAG, "m-pesa confirmation sms sent to " + phone);
        } catch (Exception e) {
            Log.e(TAG, "error sending m-pesa confirmation sms: " + e.getMessage());
        }
    }

    private void checkSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
        }
    }

    // helper method to retrieve existing student JSON profile or search by id/reg number
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