package com.example.universityregistration.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

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

    private static final int POLL_INTERVAL_MS = 8000;
    private static final int MAX_POLLS = 15; // about 2 minutes

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler main = new Handler(Looper.getMainLooper());

    public interface MpesaCallback {
        void onSuccess(String transactionId);
        void onError(String message);
    }

    // converts 07XXXXXXXX, 7XXXXXXXX, +2547XXXXXXXX to 2547XXXXXXXX (null if invalid)
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
        final String phone = normalizePhone(phoneNumber);
        if (phone == null) {
            cb.onError("Enter a valid number, e.g. 0712345678");
            return;
        }
        final String ref = (studentId == null || studentId.isEmpty()) ? "FEES" : studentId;

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
                push.put("AccountReference", ref.length() > 12 ? ref.substring(0, 12) : ref);
                push.put("TransactionDesc", "University fees");

                JSONObject r = post("/mpesa/stkpush/v1/processrequest", push, token);
                final String id = r.optString("CheckoutRequestID", "");
                if (id.isEmpty()) throw new Exception("No CheckoutRequestID: " + r);
                Log.d(TAG, "STK push accepted, id=" + id);

                JSONObject q = new JSONObject();
                q.put("BusinessShortCode", MpesaConfig.SHORTCODE);
                q.put("Password", password);
                q.put("Timestamp", ts);
                q.put("CheckoutRequestID", id);

                for (int i = 0; i < MAX_POLLS; i++) {
                    Thread.sleep(POLL_INTERVAL_MS);
                    try {
                        JSONObject res = post("/mpesa/stkpushquery/v1/query", q, token);
                        String code = res.optString("ResultCode", "");
                        final String desc = res.optString("ResultDesc", "Payment not completed");
                        Log.d(TAG, "query: code=" + code + " desc=" + desc);

                        if (code.equals("0")) {
                            main.post(() -> cb.onSuccess(id));
                            return;
                        }

                        // 4999 / "under processing" = customer still paying, keep waiting
                        String d = desc.toLowerCase(Locale.US);
                        boolean stillProcessing = code.equals("4999")
                                || d.contains("under processing")
                                || d.contains("being processed");

                        if (!code.isEmpty() && !stillProcessing) {
                            // definite failure: cancelled, wrong PIN, insufficient funds, etc.
                            main.post(() -> cb.onError(desc));
                            return;
                        }
                    } catch (Exception pending) {
                        // "transaction does not exist" or rate limit: not final, keep waiting
                        Log.d(TAG, "still pending: " + pending.getMessage());
                        if (String.valueOf(pending.getMessage()).contains("HTTP 429")) {
                            Thread.sleep(POLL_INTERVAL_MS); // extra back-off
                        }
                    }
                }
                main.post(() -> cb.onError(
                        "No confirmation received yet. Check your M-PESA messages before trying again."));
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
        try (OutputStream os = c.getOutputStream()) {
            os.write(body.toString().getBytes("UTF-8"));
        }
        return new JSONObject(read(c));
    }

    private static String read(HttpURLConnection c) throws Exception {
        c.setConnectTimeout(15000);
        c.setReadTimeout(20000);
        int code = c.getResponseCode();
        InputStream is = code >= 400 ? c.getErrorStream() : c.getInputStream();
        StringBuilder sb = new StringBuilder();
        if (is != null) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }
        }
        if (code >= 400) throw new Exception("HTTP " + code + " " + sb);
        return sb.toString();
    }
}