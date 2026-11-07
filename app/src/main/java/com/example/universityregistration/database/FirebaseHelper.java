package com.example.universityregistration.database;

import android.util.Log;
import com.example.universityregistration.models.Student;
import com.example.universityregistration.models.LibraryRequest;
import com.example.universityregistration.models.HostelBooking;
import com.example.universityregistration.models.PaymentTransaction;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Map;

public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";
    private DatabaseReference databaseReference;
    private FirebaseFirestore firestore;
    private static boolean persistenceEnabled = false;

    // initialize firebase database and enable offline persistence
    public FirebaseHelper() {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            if (!persistenceEnabled) {
                database.setPersistenceEnabled(true);
                persistenceEnabled = true;
            }
            databaseReference = database.getReference("UniversityRegistration");
            firestore = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "error initializing firebase: " + e.getMessage());
        }
    }

    // save student personal details to firebase
    public void saveStudentRegistration(Student student) {
        if (databaseReference != null) {
            databaseReference.child("students").child(student.getIdNumber()).setValue(student)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "student saved successfully to firebase"))
                    .addOnFailureListener(e -> Log.e(TAG, "failed to save student: " + e.getMessage()));
        }
    }

    // save library book request to firebase
    public void saveLibraryRequest(LibraryRequest request) {
        if (databaseReference != null) {
            databaseReference.child("library_requests").child(request.getStudentId()).setValue(request)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "library request saved successfully"))
                    .addOnFailureListener(e -> Log.e(TAG, "failed to save library request: " + e.getMessage()));
        }
    }

    // save hostel accommodation booking to firebase
    public void saveHostelBooking(HostelBooking booking) {
        if (databaseReference != null) {
            databaseReference.child("hostel_bookings").child(booking.getStudentId()).setValue(booking)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "hostel booking saved successfully"))
                    .addOnFailureListener(e -> Log.e(TAG, "failed to save hostel booking: " + e.getMessage()));
        }
    }

    // save payment transaction to firebase
    public void savePaymentTransaction(PaymentTransaction transaction) {
        if (databaseReference != null) {
            databaseReference.child("payments").child(transaction.getStudentId()).setValue(transaction)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "payment transaction saved successfully"))
                    .addOnFailureListener(e -> Log.e(TAG, "failed to save payment transaction: " + e.getMessage()));
        }
    }

    // sync cached offline data to firebase cloud database
    public void syncCachedData(Map<String, Object> offlineData) {
        if (databaseReference != null && offlineData != null) {
            databaseReference.updateChildren(offlineData)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "offline cached data synced to firebase successfully"))
                    .addOnFailureListener(e -> Log.e(TAG, "failed to sync offline data: " + e.getMessage()));
        }
    }
}
