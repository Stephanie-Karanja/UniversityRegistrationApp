package com.example.universityregistration.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.Executors;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MpesaPaybill {
    private static final String TAG = "MpesaPaybill";
    private static final String BASE = "https://sandbox.safaricom.co.ke";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler main = new Handler(Looper.getMainLooper());

    public interface MpesaCallback {
        void onSuccess(String transactionId);
        void onError(String message);
    }

    public static String normalizePhone(String in) {
        if (in == null) return null;
        String p = in.replaceAll("[\\s+\\-]", "");
        if (p.matches("0[17]\\d{8}")) return "254" + p.substring(1);
        if (p.matches("[17]\\d{8}")) return "254" + p;
        if (p.matches("254[17]\\d{8}")) return p;
        return null;
    }

    public static void triggerStkPush(String phoneNumber, double amount,
                                      String studentId, MpesaCallback cb) {
        String phone = normalizePhone(phoneNumber);
        if (phone == null) { cb.onError("Enter a valid number, e.g. 0712345678"); return; }

        executor.execute(() -> {
            try {
                String token = getToken();
                SimpleDateFormat f = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
                f.setTimeZone(TimeZone.getTimeZone("Africa/Nairobi"));
                String ts = f.format(new Date());
                String password = Base64.encodeToString(
                        (MpesaConfig.SHORTCODE + MpesaConfig.PASSKEY + ts).getBytes("UTF-8"),
                        Base64.NO_WRAP);

                JSONObject push = new JSONObject();
                push.put("BusinessShortCode", MpesaConfig.SHORTCODE);
                push.put("Password", password);
                push.put("Timestamp", ts);
                push.put("TransactionType", "CustomerPayBillOnline");
                push.put("Amount", Math.round(amount));
                push.put("PartyA", phone);
                push.put("PartyB", MpesaConfig.SHORTCODE);
                push.put("PhoneNumber", phone);
                push.put("CallBackURL", "https://example.com/callback");
                push.put("AccountReference", studentId.length() > 12 ? studentId.substring(0, 12) : studentId);
                push.put("TransactionDesc", "University fees");

                JSONObject r = post("/mpesa/stkpush/v1/processrequest", push, token);
                String id = r.optString("CheckoutRequestID", "");
                if (id.isEmpty()) throw new Exception("No CheckoutRequestID: " + r);

                JSONObject q = new JSONObject();
                q.put("BusinessShortCode", MpesaConfig.SHORTCODE);
                q.put("Password", password);
                q.put("Timestamp", ts);
                q.put("CheckoutRequestID", id);

                for (int i = 0; i < 24; i++) {
                    Thread.sleep(5000);
                    try {
                        JSONObject res = post("/mpesa/stkpushquery/v1/query", q, token);
                        String code = res.optString("ResultCode", "");
                        if (code.equals("0")) { main.post(() -> cb.onSuccess(id)); return; }
                        if (!code.isEmpty()) {
                            String desc = res.optString("ResultDesc", "Payment not completed");
                            main.post(() -> cb.onError(desc));
                            return;
                        }
                    } catch (Exception pending) {
                        // Safaricom returns an error while the customer is still entering the PIN
                        Log.d(TAG, "still pending: " + pending.getMessage());
                    }
                }
                main.post(() -> cb.onError("Payment timed out. Please try again."));
            } catch (Exception e) {
                Log.e(TAG, "payment error", e);
                main.post(() -> cb.onError("Could not start payment: " + e.getMessage()));
            }
        });
    }

    private static String getToken() throws Exception {
        String auth = Base64.encodeToString(
                (MpesaConfig.CONSUMER_KEY + ":" + MpesaConfig.CONSUMER_SECRET).getBytes("UTF-8"),
                Base64.NO_WRAP);
        HttpURLConnection c = (HttpURLConnection) new URL(
                BASE + "/oauth/v1/generate?grant_type=client_credentials").openConnection();
        c.setRequestProperty("Authorization", "Basic " + auth);
        return new JSONObject(read(c)).getString("access_token");
    }

    private static JSONObject post(String path, JSONObject body, String token) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(BASE + path).openConnection();
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("Authorization", "Bearer " + token);
        c.setRequestProperty("Content-Type", "application/json");
        try (OutputStream os = c.getOutputStream()) { os.write(body.toString().getBytes("UTF-8")); }
        return new JSONObject(read(c));
    }

    private static String read(HttpURLConnection c) throws Exception {
        c.setConnectTimeout(15000);
        c.setReadTimeout(20000);
        int code = c.getResponseCode();
        InputStream is = code >= 400 ? c.getErrorStream() : c.getInputStream();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        if (code >= 400) throw new Exception("HTTP " + code + " " + sb);
        return sb.toString();
    }
}