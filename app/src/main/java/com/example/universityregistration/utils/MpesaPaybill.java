package com.example.universityregistration.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class MpesaPaybill {
    private static final String TAG = "MpesaPaybill";

    // callback interface for m-pesa payment results
    public interface MpesaCallback {
        void onSuccess(String transactionId);
        void onError(String message);
    }

    // simulate m-pesa stk push prompt and payment processing
    public static void triggerStkPush(String phoneNumber, double amount, MpesaCallback callback) {
        Log.d(TAG, "initiating m-pesa stk push to: " + phoneNumber + " for amount: kshs " + amount);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (phoneNumber != null && phoneNumber.length() >= 10) {
                String mockTxId = "mpesa_confirmed_" + System.currentTimeMillis();
                Log.d(TAG, "m-pesa transaction successful: " + mockTxId);
                callback.onSuccess(mockTxId);
            } else {
                callback.onError("invalid phone number format for m-pesa stk push.");
            }
        }, 1500);
    }
}
