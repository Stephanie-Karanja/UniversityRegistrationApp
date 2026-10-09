package com.example.universityregistration.models;

public class HostelBooking {
    private String studentId;
    private String location;
    private String roomType;
    private boolean isSharable;

    public HostelBooking() {}

    public HostelBooking(String studentId, String location, String roomType, boolean isSharable) {
        this.studentId = studentId;
        this.location = location;
        this.roomType = roomType;
        this.isSharable = isSharable;
    }

    // Getters and Setters
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }

    public boolean isSharable() { return isSharable; }
    public void setSharable(boolean sharable) { isSharable = sharable; }
}