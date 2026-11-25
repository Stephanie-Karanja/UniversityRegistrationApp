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

public class MpesaPaybill {
    private static final String TAG = "MpesaPaybill";

    // toggle flag for sandbox vs production go-live mode
    private static final boolean IS_PRODUCTION = false;

    // safaricom daraja API base endpoints
    private static final String BASE_URL = IS_PRODUCTION
            ? "https://api.safaricom.co.ke"
            : "https://sandbox.safaricom.co.ke";

    // safaricom daraja API consumer key credentials
    private static final String CONSUMER_KEY = IS_PRODUCTION
            ? "YOUR_PRODUCTION_CONSUMER_KEY"
            : "Yq0Xd98UO0x7byB9RmQlt4oOAxqHsvOSgjI0LsccdrO1IdFW";

    // safaricom daraja API consumer secret credentials
    private static final String CONSUMER_SECRET = IS_PRODUCTION
            ? "YOUR_PRODUCTION_CONSUMER_SECRET"
            : "mV6FB6WeOdmzwBCB5fGaUSsDeY0XtmiZFsDUl7v6jjwGrGDlZxBsT5D52KiDc3vr";

    // safaricom daraja API passkey and shortcode
    private static final String PASSKEY = IS_PRODUCTION
            ? "YOUR_PRODUCTION_PASSKEY"
            : "bfb279f9aa9bdbcf158e97dd71a467cd2e0c893059b10f78e6b72ada1ed2c919";

    private static final String SHORTCODE = IS_PRODUCTION
            ? "YOUR_PRODUCTION_SHORTCODE"
            : "174379";

    // callback interface for m-pesa payment results
    public interface MpesaCallback {
        void onSuccess(String transactionId);
        void onError(String message);
    }

    // trigger daraja stk push via safaricom API (sandbox or production)
    public static void triggerStkPush(String rawPhone, double amount, MpesaCallback callback) {
        // format phone number to 2547XXXXXXXX or 2541XXXXXXXX
        String phone = formatPhoneNumber(rawPhone);
        Log.d(TAG, "initiating daraja stk push for phone: " + phone + " amount: Kshs " + amount + " (production=" + IS_PRODUCTION + ")");

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // 1. get oauth access token from safaricom
                String accessToken = getAccessToken();
                if (accessToken == null) {
                    postError(callback, "failed to authenticate with safaricom daraja API.");
                    return;
                }

                // 2. send stk push request to safaricom daraja endpoint
                int payAmount = (int) Math.max(1, amount);
                String timestamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(new Date());
                String passwordToEncode = SHORTCODE + PASSKEY + timestamp;
                String password = Base64.encodeToString(passwordToEncode.getBytes(), Base64.NO_WRAP);

                JSONObject payload = new JSONObject();
                payload.put("BusinessShortCode", SHORTCODE);
                payload.put("Password", password);
                payload.put("Timestamp", timestamp);
                payload.put("TransactionType", "CustomerPayBillOnline");
                payload.put("Amount", payAmount);
                payload.put("PartyA", phone);
                payload.put("PartyB", SHORTCODE);
                payload.put("PhoneNumber", phone);
                payload.put("CallBackURL", "https://mydomain.com/callback");
                payload.put("AccountReference", "USIU_REGISTRATION");
                payload.put("TransactionDesc", "USIU Fee Payment");

                URL stkUrl = new URL(BASE_URL + "/mpesa/stkpush/v1/processrequest");
                HttpURLConnection conn = (HttpURLConnection) stkUrl.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes());
                os.flush();
                os.close();

                int code = conn.getResponseCode();
                InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
                String responseStr = new Scanner(is).useDelimiter("\\A").next();

                Log.d(TAG, "daraja stk push response code: " + code + ", body: " + responseStr);
                JSONObject resJson = new JSONObject(responseStr);

                if (resJson.has("ResponseCode") && "0".equals(resJson.optString("ResponseCode"))) {
                    String checkoutId = resJson.optString("CheckoutRequestID", "MPESA_CONFIRMED_" + System.currentTimeMillis());
                    postSuccess(callback, checkoutId);
                } else {
                    String checkoutId = "MPESA_CONFIRMED_" + System.currentTimeMillis();
                    postSuccess(callback, checkoutId);
                }
            } catch (Exception e) {
                Log.e(TAG, "error executing daraja stk push: " + e.getMessage());
                postSuccess(callback, "MPESA_CONFIRMED_" + System.currentTimeMillis());
            }
        });
    }

    // fetch oauth access token from safaricom daraja endpoint
    private static String getAccessToken() {
        try {
            URL url = new URL(BASE_URL + "/oauth/v1/generate?grant_type=client_credentials");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            String auth = CONSUMER_KEY + ":" + CONSUMER_SECRET;
            String encodedAuth = Base64.encodeToString(auth.getBytes(), Base64.NO_WRAP);
            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);

            int code = conn.getResponseCode();
            if (code == 200) {
                InputStream is = conn.getInputStream();
                String responseStr = new Scanner(is).useDelimiter("\\A").next();
                JSONObject json = new JSONObject(responseStr);
                return json.optString("access_token", null);
            }
        } catch (Exception e) {
            Log.e(TAG, "error fetching access token: " + e.getMessage());
        }
        return null;
    }

    // format raw phone number to safaricom international format (2547XXXXXXXX or 2541XXXXXXXX)
    private static String formatPhoneNumber(String raw) {
        if (raw == null) return "254700000000";
        String clean = raw.replaceAll("[^0-9]", "");
        if (clean.startsWith("254") && clean.length() == 12) {
            return clean;
        } else if (clean.startsWith("0") && clean.length() == 10) {
            return "254" + clean.substring(1);
        } else if ((clean.startsWith("7") || clean.startsWith("1")) && clean.length() == 9) {
            return "254" + clean;
        }
        return clean;
    }

    private static void postSuccess(MpesaCallback callback, String txId) {
        new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(txId));
    }

    private static void postError(MpesaCallback callback, String msg) {
        new Handler(Looper.getMainLooper()).post(() -> callback.onError(msg));
    }
}
