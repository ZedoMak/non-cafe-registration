package com.noncafe.model;

import java.io.Serializable;

public enum StudentRegistrationStatus implements Serializable {
    UNREGISTERED("Unregistered", "#34495e"),
    PENDING_APPROVAL("Pending Approval", "#f39c12"),
    APPROVED("Approved", "#2ecc71"),
    REJECTED("Rejected", "#e74c3c");

    private final String displayName;
    private final String colorCode;

    StudentRegistrationStatus(String displayName, String colorCode) {
        this.displayName = displayName;
        this.colorCode = colorCode;
    }

    public String getDisplayName() {
        return displayName;
    }
    public String getColorCode() {
        return colorCode;
    }
}