package com.example.universityregistration.models;

public class Student {
    private String fullName;
    private String idNumber;
    private String regNumber;
    private String gender;
    private String course;
    private String department;
    private boolean isPaymentConfirmed;

    public Student() {}

    public Student(String fullName, String idNumber, String regNumber, String gender, String course, String department) {
        this.fullName = fullName;
        this.idNumber = idNumber;
        this.regNumber = regNumber;
        this.gender = gender;
        this.course = course;
        this.department = department;
        this.isPaymentConfirmed = false;
    }

    // Getters and Setters
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }

    public String getRegNumber() { return regNumber; }
    public void setRegNumber(String regNumber) { this.regNumber = regNumber; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public boolean isPaymentConfirmed() { return isPaymentConfirmed; }
    public void setPaymentConfirmed(boolean paymentConfirmed) { isPaymentConfirmed = paymentConfirmed; }
}