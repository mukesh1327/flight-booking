package com.cloudxplorer.authservice.model;

import java.util.Set;

public class PrincipalInfo {
    private final String subject;
    private final String email;
    private final Set<String> roles;

    public PrincipalInfo(String subject, String email, Set<String> roles) {
        this.subject = subject;
        this.email = email;
        this.roles = roles;
    }

    public String getSubject() {
        return subject;
    }

    public String getEmail() {
        return email;
    }

    public Set<String> getRoles() {
        return roles;
    }
}
