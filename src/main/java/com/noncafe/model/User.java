package com.noncafe.model;

import java.io.Serializable;

public abstract class User implements Serializable {
    private static final long serialVersionUID = 1L;
    protected String name;
    protected String id;
    protected String password;
    protected Campus campus;

    public User(String name, String id, String password, Campus campus) {
        this.name = name;
        this.id = id;
        this.password = password;
        this.campus = campus;
    }
    public String getName() { return name; }
    public String getId() { return id; }
    public String getPassword() { return password; }
    public Campus getCampus() { return campus; }
}