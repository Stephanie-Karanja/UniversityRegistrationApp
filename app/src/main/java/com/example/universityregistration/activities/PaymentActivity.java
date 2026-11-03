package com.example.universityregistration.activities;

import android.os.Bundle;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.universityregistrationapp.R;
import com.example.universityregistration.utils.MpesaPaybill;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PaymentActivity extends AppCompatActivity {
    private static final long STATUS_CHECK_INTERVAL_MS = 5000;
    private static final int MAX_AUTOMATIC_STATUS_CHECKS = 120;

    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private EditText studentReferenceInput;
    private EditText phoneInput;
    private EditText amountInput;
    private Button payButton;
    private Button checkStatusButton;
    private TextView paymentStatusText;
    private String checkoutRequestId;
    private int statusCheckCount;
    private final Runnable automaticStatusCheck = this::checkPaymentStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        studentReferenceInput = findViewById(R.id.studentReferenceInput);
        phoneInput = findViewById(R.id.phoneInput);
        amountInput = findViewById(R.id.amountInput);
        payButton = findViewById(R.id.payButton);
        checkStatusButton = findViewById(R.id.checkStatusButton);
        paymentStatusText = findViewById(R.id.paymentStatusText);

        payButton.setOnClickListener(view -> startPayment());
        checkStatusButton.setOnClickListener(view -> checkPaymentStatus());
    }

    private void startPayment() {
        String studentReference = studentReferenceInput.getText().toString().trim();
        String phoneNumber = MpesaPaybill.normalizeKenyanPhone(phoneInput.getText().toString());
        String amountText = amountInput.getText().toString().trim();

        if (studentReference.isEmpty()) {
            studentReferenceInput.setError("Enter your admission or student number");
            return;
        }
        if (phoneNumber == null) {
            phoneInput.setError("Enter a valid Kenyan mobile number, e.g. 0712345678");
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(amountText);
        } catch (NumberFormatException exception) {
            amountInput.setError("Enter the amount in whole Kenyan shillings");
            return;
        }
        if (amount < 1 || amount > 150000) {
            amountInput.setError("Enter an amount from KSh 1 to KSh 150,000");
            return;
        }

        setFormEnabled(false);
        payButton.setText("Sending prompt...");
        paymentStatusText.setText("Contacting M-PESA. Please wait...");
        networkExecutor.execute(() -> {
            try {
                JSONObject response = MpesaPaybill.requestStkPush(phoneNumber, amount, studentReference);
                String requestId = response.optString("checkoutRequestId");
                if (requestId.isEmpty()) {
                    throw new IllegalStateException("Payment service did not return a request ID.");
                }
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    checkoutRequestId = requestId;
                    statusCheckCount = 0;
                    payButton.setText("Prompt sent");
                    checkStatusButton.setVisibility(View.VISIBLE);
                    paymentStatusText.setText("M-PESA prompt sent to " + phoneNumber
                            + ". Enter your M-PESA PIN on your phone to approve it.");
                    mainHandler.postDelayed(automaticStatusCheck, STATUS_CHECK_INTERVAL_MS);
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    paymentStatusText.setText("Could not start payment: " + exception.getMessage());
                    setFormEnabled(true);
                    payButton.setText("Try payment again");
                });
            }
        });
    }

    private void checkPaymentStatus() {
        if (checkoutRequestId == null) return;
        mainHandler.removeCallbacks(automaticStatusCheck);
        checkStatusButton.setEnabled(false);
        networkExecutor.execute(() -> {
            try {
                JSONObject response = MpesaPaybill.checkPaymentStatus(checkoutRequestId);
                String status = response.optString("status", "PENDING");
                runOnUiThread(() -> handlePaymentStatus(status, response));
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    paymentStatusText.setText("Could not check payment status. Check your connection and try again.");
                    checkStatusButton.setEnabled(true);
                    checkStatusButton.setText("Retry status check");
                    checkStatusButton.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private void handlePaymentStatus(String status, JSONObject response) {
        if (isFinishing() || isDestroyed()) return;
        checkStatusButton.setEnabled(true);
        if ("PAID".equalsIgnoreCase(status)) {
            String receiptNumber = response.optString("receiptNumber", "");
            paymentStatusText.setText("Payment confirmed by Safaricom. Receipt: " + receiptNumber);
            Intent result = new Intent();
            result.putExtra("paymentConfirmed", true);
            result.putExtra("transactionId", receiptNumber);
            result.putExtra("studentReference", studentReferenceInput.getText().toString().trim());
            result.putExtra("amount", Integer.parseInt(amountInput.getText().toString().trim()));
            setResult(RESULT_OK, result);
            finish();
            return;
        }
        if ("FAILED".equalsIgnoreCase(status)) {
            paymentStatusText.setText("Payment was not completed. "
                    + response.optString("failureReason", "Please try again."));
            checkoutRequestId = null;
            checkStatusButton.setVisibility(View.GONE);
            setFormEnabled(true);
            payButton.setText("Try payment again");
            return;
        }

        statusCheckCount++;
        paymentStatusText.setText("Payment is pending. Approve the M-PESA prompt on your phone.");
        checkStatusButton.setText("Check payment status");
        if (statusCheckCount < MAX_AUTOMATIC_STATUS_CHECKS) {
            mainHandler.postDelayed(automaticStatusCheck, STATUS_CHECK_INTERVAL_MS);
        } else {
            paymentStatusText.append(" You can check again using the button.");
        }
    }

    private void setFormEnabled(boolean enabled) {
        studentReferenceInput.setEnabled(enabled);
        phoneInput.setEnabled(enabled);
        amountInput.setEnabled(enabled);
        payButton.setEnabled(enabled);
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacks(automaticStatusCheck);
        networkExecutor.shutdownNow();
        super.onDestroy();
    }
}