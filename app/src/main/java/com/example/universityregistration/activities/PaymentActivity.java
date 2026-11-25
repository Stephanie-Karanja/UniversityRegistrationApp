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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.universityregistration.R;
import com.example.universityregistration.models.PaymentTransaction;
import com.example.universityregistration.database.FirebaseHelper;
import org.json.JSONObject;
import java.util.Locale;
import java.util.Map;

public class PaymentActivity extends AppCompatActivity {

    private static final String TAG = "PaymentActivity";
    private static final int SMS_PERMISSION_CODE = 101;

    // declare ui views for m-pesa payment
    private EditText etPhoneNumber, etAmount;
    private TextView tvPaymentStatus;
    private FirebaseHelper firebaseHelper;
    private String studentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        // bind xml layout elements
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etAmount = findViewById(R.id.etAmount);
        Button btnPayMpesa = findViewById(R.id.btnPayMpesa);
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

        // handle m-pesa payment click
        btnPayMpesa.setOnClickListener(v -> {
            String phone = etPhoneNumber.getText().toString().trim();
            String amountStr = etAmount.getText().toString().trim();

            if (phone.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(this, "please enter phone number and fee amount", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountStr);
            showMpesaStkPushDialog(phone, amount);
        });

        // handle view summary click
        btnViewSummary.setOnClickListener(v -> {
            Intent intent = new Intent(PaymentActivity.this, SummaryActivity.class);
            intent.putExtra("STUDENT_ID", studentId);
            startActivity(intent);
        });
    }

    // display simple m-pesa stk push pop-up prompt dialog
    private void showMpesaStkPushDialog(String phone, double amount) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("m-pesa stk push");
        builder.setMessage("pay kshs " + amount + " to usiu paybill 123456 for phone " + phone + "?\n\nenter m-pesa pin:");

        final EditText etPin = new EditText(this);
        etPin.setHint("m-pesa pin (4 digits)");
        etPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        builder.setView(etPin);

        // handle positive send button click
        builder.setPositiveButton("send", (dialog, which) -> {
            String pin = etPin.getText().toString().trim();
            if (pin.length() < 4) {
                Toast.makeText(this, "invalid m-pesa pin. transaction cancelled.", Toast.LENGTH_SHORT).show();
                return;
            }

            String txId = "mpesa_confirmed_" + System.currentTimeMillis();
            tvPaymentStatus.setText("status: payment confirmed (m-pesa)");
            tvPaymentStatus.setTextColor(getResources().getColor(R.color.success_green));

            // calculate dynamic fee balance based on actual amount paid
            double totalFee = 180000.0;
            double feeBalance = Math.max(0.0, totalFee - amount);

            // save payment transaction to firebase
            PaymentTransaction tx = new PaymentTransaction(studentId, phone, amount, txId, "confirmed");
            firebaseHelper.savePaymentTransaction(tx);

            // safely update consolidated student profile in local preferences
            try {
                SharedPreferences prefs = getSharedPreferences("OfflineRegistrationCache", Context.MODE_PRIVATE);
                JSONObject json = getOrCreateStudentJson(prefs, studentId);
                json.put("totalFee", totalFee);
                json.put("amountPaid", amount);
                json.put("feeBalance", feeBalance);
                json.put("paymentMethod", "M-PESA");
                json.put("paymentStatus", "Confirmed");
                json.put("phoneNumber", phone);
                String jsonStr = json.toString();

                String regNum = json.optString("regNumber", "");
                String realId = json.optString("idNumber", studentId);
                SharedPreferences.Editor editor = prefs.edit()
                        .putString(studentId, jsonStr)
                        .putString(realId, jsonStr)
                        .putString("STUDENT_" + realId, jsonStr);
                if (!regNum.isEmpty()) {
                    editor.putString(regNum, jsonStr);
                }
                editor.apply();
            } catch (Exception e) {
                Log.e(TAG, "error saving payment transaction to local prefs: " + e.getMessage());
            }

            // send real m-pesa confirmation sms message to physical phone
            sendMpesaConfirmationSms(phone, amount, feeBalance, txId);

            Toast.makeText(this, "m-pesa payment confirmed! fee balance: kshs " + feeBalance, Toast.LENGTH_LONG).show();
        });

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
}
