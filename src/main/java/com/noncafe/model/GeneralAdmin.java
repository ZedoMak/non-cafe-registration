package com.noncafe.model;

import java.util.ArrayList;
import java.util.List;

public class GeneralAdmin extends User {
    private List<String> transactionHistory;

    public GeneralAdmin(String name, String id, String password) {
        super(name, id, password, null);
        this.transactionHistory = new ArrayList<>();
    }

    public List<String> getTransactionHistory() {
        if (transactionHistory == null) transactionHistory = new ArrayList<>();
        return transactionHistory;
    }

    public void addLog(String log) {
        getTransactionHistory().add(log);
    }
}