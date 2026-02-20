package com.cloudxplorer.authservice.model;

public class UserRecord {
    private final String userId;
    private final String name;
    private final String username;
    private final String email;
    private final String phone;

    public UserRecord(String userId, String name, String username, String email, String phone) {
        this.userId = userId;
        this.name = name;
        this.username = username;
        this.email = email;
        this.phone = phone;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }
}
