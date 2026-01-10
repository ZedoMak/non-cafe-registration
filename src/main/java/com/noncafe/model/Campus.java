package com.noncafe.model;

import java.io.Serializable;

public enum Campus implements Serializable {
    MAIN("Main Campus"),
    TECHNO("Techno Campus"),
    AGRI("Agri Campus"),
    REFERRAL("Referral Campus");
    private final String displayName;

    Campus(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}