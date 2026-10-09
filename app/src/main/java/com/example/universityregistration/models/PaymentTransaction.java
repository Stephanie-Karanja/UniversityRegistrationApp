package com.example.universityregistration.models;

public class PaymentTransaction {
    private String studentId;
    private String phoneNumber;
    private double amount;
    private String transactionId;
    private String status;

    public PaymentTransaction() {}

    public PaymentTransaction(String studentId, String phoneNumber, double amount, String transactionId, String status) {
        this.studentId = studentId;
        this.phoneNumber = phoneNumber;
        this.amount = amount;
        this.transactionId = transactionId;
        this.status = status;
    }

    // Getters and Setters
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}