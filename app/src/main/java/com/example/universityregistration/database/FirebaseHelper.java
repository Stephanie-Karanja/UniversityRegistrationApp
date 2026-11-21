package com.example.universityregistration.database;

import android.util.Log;
import com.example.universityregistration.models.Student;
import com.example.universityregistration.models.LibraryRequest;
import com.example.universityregistration.models.HostelBooking;
import com.example.universityregistration.models.PaymentTransaction;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";
    private DatabaseReference databaseReference;
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
        } catch (Exception e) {
            Log.e(TAG, "error initializing firebase: " + e.getMessage());
        }
    }

    // save student personal details to /UniversityRegistration/students/{idNumber}
    public void saveStudentRegistration(Student student) {
        if (databaseReference != null && student != null) {
            Map<String, Object> map = new HashMap<>();
            map.put("fullName", student.getFullName());
            map.put("idNumber", student.getIdNumber());
            map.put("regNumber", student.getRegNumber());
            map.put("gender", student.getGender());
            map.put("course", student.getCourse());
            map.put("department", student.getDepartment());

            databaseReference.child("students").child(student.getIdNumber()).setValue(map)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "student saved to /students/" + student.getIdNumber()))
                    .addOnFailureListener(e -> Log.e(TAG, "failed to save student: " + e.getMessage()));
        }
    }

    // save library book request to /UniversityRegistration/library_requests/{studentId}
    public void saveLibraryRequest(LibraryRequest request) {
        if (databaseReference != null && request != null) {
            Map<String, Object> map = new HashMap<>();
            map.put("studentId", request.getStudentId());
            map.put("bookIsbn", request.getIsbn());
            map.put("bookTitle", request.getTitle());
            map.put("bookAuthor", request.getAuthor());
            map.put("dateBorrowed", request.getDateBorrowed());
            map.put("returnDate", request.getDateReturn());

            databaseReference.child("library_requests").child(request.getStudentId()).setValue(map)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "library request saved to /library_requests/" + request.getStudentId()))
                    .addOnFailureListener(e -> Log.e(TAG, "failed to save library request: " + e.getMessage()));
        }
    }

    // save hostel accommodation booking to /UniversityRegistration/hostel_bookings/{studentId}
    public void saveHostelBooking(HostelBooking booking) {
        if (databaseReference != null && booking != null) {
            Map<String, Object> map = new HashMap<>();
            map.put("studentId", booking.getStudentId());
            map.put("hostelLocation", booking.getLocation());
            map.put("hostelRoomType", booking.getRoomType());
            map.put("isSharable", booking.isSharable() ? "Sharable" : "Non-Sharable");

            databaseReference.child("hostel_bookings").child(booking.getStudentId()).setValue(map)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "hostel booking saved to /hostel_bookings/" + booking.getStudentId()))
                    .addOnFailureListener(e -> Log.e(TAG, "failed to save hostel booking: " + e.getMessage()));
        }
    }

    // save payment transaction to /UniversityRegistration/payments/{studentId}
    public void savePaymentTransaction(PaymentTransaction transaction) {
        if (databaseReference != null && transaction != null) {
            Map<String, Object> map = new HashMap<>();
            map.put("studentId", transaction.getStudentId());
            map.put("phoneNumber", transaction.getPhoneNumber());
            map.put("amountPaid", transaction.getAmount());
            map.put("totalFee", 180000.0);
            map.put("feeBalance", Math.max(0.0, 180000.0 - transaction.getAmount()));
            map.put("transactionId", transaction.getTransactionId());
            map.put("paymentMethod", "M-PESA");
            map.put("status", transaction.getStatus());

            databaseReference.child("payments").child(transaction.getStudentId()).setValue(map)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "payment saved to /payments/" + transaction.getStudentId()))
                    .addOnFailureListener(e -> Log.e(TAG, "failed to save payment: " + e.getMessage()));
        }
    }

    // sync cached offline data cleanly under organized child nodes
    public void syncCachedData(Map<String, Object> offlineData) {
        if (databaseReference != null && offlineData != null) {
            databaseReference.updateChildren(offlineData)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "offline cached data synced cleanly to firebase"))
                    .addOnFailureListener(e -> Log.e(TAG, "failed to sync offline data: " + e.getMessage()));
        }
    }
}
