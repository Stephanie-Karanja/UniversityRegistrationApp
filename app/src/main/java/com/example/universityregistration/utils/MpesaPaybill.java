package com.example.universityregistration.utils;

import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MpesaPaybill {
    private static final String API_BASE_URL = "https://universityregistrationapp.onrender.com";

    private MpesaPaybill() {}

    public static String normalizeKenyanPhone(String input) {
        String phoneNumber = input.replaceAll("[\\s-]", "");
        if (phoneNumber.startsWith("+")) {
            phoneNumber = phoneNumber.substring(1);
        }
        if (phoneNumber.startsWith("0")) {
            phoneNumber = "254" + phoneNumber.substring(1);
        }
        return phoneNumber.matches("254[17][0-9]{8}") ? phoneNumber : null;
    }

    public static JSONObject requestStkPush(String phoneNumber, int amount, String studentReference)
            throws IOException, JSONException {
        JSONObject payload = new JSONObject();
        payload.put("phoneNumber", phoneNumber);
        payload.put("amount", amount);
        payload.put("accountReference", studentReference);
        return sendRequest("POST", "/api/payments/stk-push", payload);
    }

    public static JSONObject checkPaymentStatus(String checkoutRequestId)
            throws IOException, JSONException {
        return sendRequest("GET", "/api/payments/" + Uri.encode(checkoutRequestId), null);
    }

    private static JSONObject sendRequest(String method, String path, JSONObject payload)
            throws IOException, JSONException {
        HttpURLConnection connection = (HttpURLConnection) new URL(API_BASE_URL + path).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);
        connection.setRequestProperty("Accept", "application/json");
        if (payload != null) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            try (OutputStream output = connection.getOutputStream()) {
                output.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }
        }

        int responseCode = connection.getResponseCode();
        InputStream responseStream = responseCode >= 200 && responseCode < 300
                ? connection.getInputStream() : connection.getErrorStream();
        StringBuilder responseBody = new StringBuilder();
        if (responseStream != null) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(responseStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBody.append(line);
                }
            }
        }
        connection.disconnect();

        JSONObject response = new JSONObject(responseBody.length() == 0 ? "{}" : responseBody.toString());
        if (responseCode < 200 || responseCode >= 300) {
            throw new IOException(response.optString("error", "Payment service request failed."));
        }
        return response;
    }
}