package com.example.universityregistration.database;

import android.content.Context;

public class OfflineSyncManager {
    private Context context;

    public OfflineSyncManager(Context context) {
        this.context = context;
    }

    public boolean isNetworkAvailable() {
        // Network state listener logic
        return false;
    }
}