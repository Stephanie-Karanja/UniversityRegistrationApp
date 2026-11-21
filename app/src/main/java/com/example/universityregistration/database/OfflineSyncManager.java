package com.example.universityregistration.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class OfflineSyncManager {
    private static final String TAG = "OfflineSyncManager";
    private static final String PREF_NAME = "OfflineRegistrationCache";
    private final Context context;
    private final SharedPreferences sharedPreferences;
    private final FirebaseHelper firebaseHelper;

    // initialize offline sync manager and register network callback
    public OfflineSyncManager(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.firebaseHelper = new FirebaseHelper();
        registerNetworkCallback();
    }

    // check if internet network is currently available
    public boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            Network network = cm.getActiveNetwork();
            if (network != null) {
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
                return capabilities != null && (
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                );
            }
        }
        return false;
    }

    // cache registration data locally when offline
    public void cacheDataLocally(String key, String jsonData) {
        sharedPreferences.edit().putString(key, jsonData).apply();
        Log.d(TAG, "data cached locally for key: " + key);
        if (isNetworkAvailable()) {
            syncPendingData();
        }
    }

    // sync pending offline cached data to firebase while preserving local records for reports
    public void syncPendingData() {
        if (!isNetworkAvailable()) return;

        Map<String, ?> allEntries = sharedPreferences.getAll();
        if (allEntries.isEmpty()) return;

        Map<String, Object> syncMap = new HashMap<>();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String key = entry.getKey();
            if ("LATEST_STUDENT_ID".equals(key) || key.startsWith("STUDENT_")) continue;

            try {
                Object val = entry.getValue();
                if (val instanceof String && ((String) val).startsWith("{")) {
                    JSONObject json = new JSONObject((String) val);
                    String idNum = json.optString("idNumber", key);

                    if (!idNum.isEmpty() && json.has("fullName")) {
                        // 1. push student clean map to students/idNum
                        Map<String, Object> sMap = new HashMap<>();
                        sMap.put("fullName", json.optString("fullName"));
                        sMap.put("idNumber", idNum);
                        sMap.put("regNumber", json.optString("regNumber"));
                        sMap.put("gender", json.optString("gender"));
                        sMap.put("course", json.optString("course"));
                        sMap.put("department", json.optString("department"));

                        syncMap.put("students/" + idNum, sMap);

                        // 2. push library request clean map if present
                        if (json.has("bookTitle")) {
                            Map<String, Object> lMap = new HashMap<>();
                            lMap.put("studentId", idNum);
                            lMap.put("bookTitle", json.optString("bookTitle"));
                            lMap.put("bookAuthor", json.optString("bookAuthor"));
                            lMap.put("bookIsbn", json.optString("bookIsbn"));
                            lMap.put("returnDate", json.optString("returnDate"));
                            syncMap.put("library_requests/" + idNum, lMap);
                        }

                        // 3. push hostel booking clean map if present
                        if (json.has("hostelLocation")) {
                            Map<String, Object> hMap = new HashMap<>();
                            hMap.put("studentId", idNum);
                            hMap.put("hostelLocation", json.optString("hostelLocation"));
                            hMap.put("hostelRoomType", json.optString("hostelRoomType"));
                            hMap.put("isSharable", json.optString("isSharable"));
                            syncMap.put("hostel_bookings/" + idNum, hMap);
                        }

                        // 4. push payment transaction clean map if present
                        if (json.has("amountPaid")) {
                            Map<String, Object> pMap = new HashMap<>();
                            pMap.put("studentId", idNum);
                            pMap.put("totalFee", json.optDouble("totalFee", 180000.0));
                            pMap.put("amountPaid", json.optDouble("amountPaid", 0.0));
                            pMap.put("feeBalance", json.optDouble("feeBalance", 180000.0));
                            pMap.put("paymentMethod", json.optString("paymentMethod", "M-PESA"));
                            pMap.put("status", json.optString("paymentStatus", "Confirmed"));
                            pMap.put("phoneNumber", json.optString("phoneNumber", ""));
                            syncMap.put("payments/" + idNum, pMap);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "error parsing cached data for clean sync: " + e.getMessage());
            }
        }

        if (!syncMap.isEmpty()) {
            firebaseHelper.syncCachedData(syncMap);
            Log.d(TAG, "pending data synced cleanly under organized Firebase branches.");
        }
    }

    // register network availability callback to auto-sync when online
    private void registerNetworkCallback() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkRequest request = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();
            cm.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    super.onAvailable(network);
                    Log.d(TAG, "network connection restored. triggering offline sync...");
                    syncPendingData();
                }
            });
        }
    }
}
