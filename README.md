# University Registration App

An Android application built for first year student registration, library book requests, hostel selection, fee payments, and offline data synchronization.

## Overview

This repository contains the initial architecture and base structure for the APT 3060 Mobile Programming project. The baseline code, package layout, and data models have been generated to allow team members to work on their respective activities concurrently without merge conflicts.

## Work Completed So Far

* Set up the base Android Studio Java project structure.
* Organized the code into modular packages: `activities`, `database`, `models`, and `utils`.
* Created starter Activity classes for all 4 core application modules and summary views.


* Created data model classes for Students, Library Requests, Hostel Bookings, and Payment Transactions.


* Established placeholder utility classes for Firebase database sync, offline data caching, M-PESA payment logic, and document printing.



## Project Structure

```text
app/src/main/java/com/example/universityregistration/
├── activities/
│   ├── RegistrationActivity.java
│   ├── LibraryActivity.java
│   ├── HostelActivity.java
│   ├── PaymentActivity.java
│   └── SummaryActivity.java
├── database/
│   ├── FirebaseHelper.java
│   └── OfflineSyncManager.java
├── models/
│   ├── Student.java
│   ├── LibraryRequest.java
│   ├── HostelBooking.java
│   └── PaymentTransaction.java
└── utils/
    ├── MpesaPaybill.java
    └── PDFPrintAdapter.java

```

## Team Role Assignments

### Member 1: Student Registration (1st Activity)

* **Tasks:** Build the user interface for capturing Full Names, ID Number, Registration Number, Gender, Course, and Department. Implement form input validation and bind data to the `Student` model.


### Member 2: Library Book Request (2nd Activity)

* **Tasks:** Build the interface for requesting library books using Book ISBN, Author, Title, Date Borrowed, Date of Return, and Student ID Number. Implement date pickers and selection validation.


### Member 3: Hostel Selection (3rd Activity)

* **Tasks:** Build the interface for choosing hostel details including Hostel Location, Room Type, Sharable status, and Student ID Number. Link input selections to the `HostelBooking` model.


### Member 4: MPESA Payment Integration (4th Activity)

* **Tasks:** Implement phone number and amount entry for fee payments. Integrate the MPESA Daraja API prompt and ensure registration is confirmed only after successful payment verification.


### Member 5: Firebase Setup and Offline Synchronization

* **Tasks:** Configure Firebase Realtime Database or Cloud Firestore. Implement offline caching using Room Database or Firebase persistence, along with background network sync when connectivity returns.


### Member 6: Application Navigation, Summary Reports, and QA

* **Tasks:** Build app navigation between activities. Implement logic to view and print registration summaries for individual students and all registered students. Lead testing for zero errors before submission.


## Guidelines for Team Members

1. Clone the repository to your local computer.
2. Create a new branch for your task before making changes (e.g., `git checkout -b feature/your-task-name`).
3. Only work on the files assigned to your module to avoid code conflicts.
4. Push your branch to GitHub and open a Pull Request when your module is completed.
