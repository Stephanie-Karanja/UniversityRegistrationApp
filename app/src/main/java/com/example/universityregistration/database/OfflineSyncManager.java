package com.example.universityregistration.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;
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

    // sync pending offline cached data to firebase
    public void syncPendingData() {
        if (!isNetworkAvailable()) return;

        Map<String, ?> allEntries = sharedPreferences.getAll();
        if (allEntries.isEmpty()) return;

        Map<String, Object> syncMap = new HashMap<>();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            try {
                Object val = entry.getValue();
                if (val instanceof String) {
                    syncMap.put(entry.getKey(), (String) val);
                }
            } catch (Exception e) {
                Log.e(TAG, "error parsing cached data: " + e.getMessage());
            }
        }

        if (!syncMap.isEmpty()) {
            firebaseHelper.syncCachedData(syncMap);
            sharedPreferences.edit().clear().apply();
            Log.d(TAG, "pending offline data synced to firebase.");
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
