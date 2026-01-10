package com.noncafe.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Student extends User {
    private String department;
    private int year;
    private String bankAccount;
    private String phoneNumber;
    private StudentRegistrationStatus registrationStatus;
    private double balance;
    private List<String> transactionHistory;

    public Student(String name, String id, String password, Campus campus, String department, int year, String bankAccount, String phoneNumber) {
        super(name, id, password, campus);
        this.department = department;
        this.year = year;
        this.bankAccount = bankAccount;
        this.phoneNumber = phoneNumber;
        this.registrationStatus = StudentRegistrationStatus.UNREGISTERED;
        this.balance = 0.0;
        this.transactionHistory = new ArrayList<>();
    }

    public void registerForNonCafe() {
        if (this.registrationStatus == StudentRegistrationStatus.UNREGISTERED || this.registrationStatus == StudentRegistrationStatus.REJECTED) {
            this.registrationStatus = StudentRegistrationStatus.PENDING_APPROVAL;
            this.transactionHistory.add("Application submitted for Non-Cafe on " + LocalDate.now() + ". Status: PENDING.");
        }
    }

    public void unregisterFromNonCafe() {
        if (this.registrationStatus != StudentRegistrationStatus.UNREGISTERED) {
            this.registrationStatus = StudentRegistrationStatus.UNREGISTERED;
            this.transactionHistory.add("Unregistered from Non-Cafe on " + LocalDate.now());
        }
    }

    public void approveRegistration() {
        if (this.registrationStatus == StudentRegistrationStatus.PENDING_APPROVAL) {
            this.registrationStatus = StudentRegistrationStatus.APPROVED;
            this.transactionHistory.add("Registration Approved by Admin on " + LocalDate.now());
        }
    }

    public void rejectRegistration() {
        if (this.registrationStatus == StudentRegistrationStatus.PENDING_APPROVAL) {
            this.registrationStatus = StudentRegistrationStatus.REJECTED;
            this.transactionHistory.add("Registration Rejected by Admin on " + LocalDate.now());
        }
    }

    public void applyCost(double amount) {
        this.balance += amount;
        this.transactionHistory.add(String.format("Cost sharing applied: +%.2f | New Balance: %.2f", amount, balance));
    }

    // Getters
    public String getDepartment() { return department; }
    public int getYear() { return year; }
    public String getBankAccount() { return bankAccount; }
    public String getPhoneNumber() { return phoneNumber; }

    // Setters (Added for update)
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public boolean isTrulyRegistered() {
        return this.registrationStatus == StudentRegistrationStatus.APPROVED;
    }
    public StudentRegistrationStatus getRegistrationStatus() { return registrationStatus; }
    public double getBalance() { return balance; }
    public List<String> getTransactionHistory() { return transactionHistory; }
}