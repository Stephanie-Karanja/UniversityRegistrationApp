package com.example.universityregistration.models;

public class LibraryRequest {
    private String studentId;
    private String isbn;
    private String title;
    private String author;
    private String dateBorrowed;
    private String dateReturn;

    public LibraryRequest() {}

    public LibraryRequest(String studentId, String isbn, String title, String author, String dateBorrowed, String dateReturn) {
        this.studentId = studentId;
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.dateBorrowed = dateBorrowed;
        this.dateReturn = dateReturn;
    }

    // Getters and Setters
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getDateBorrowed() { return dateBorrowed; }
    public void setDateBorrowed(String dateBorrowed) { this.dateBorrowed = dateBorrowed; }

    public String getDateReturn() { return dateReturn; }
    public void setDateReturn(String dateReturn) { this.dateReturn = dateReturn; }
}