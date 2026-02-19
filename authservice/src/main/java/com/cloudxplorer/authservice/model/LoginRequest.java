package com.cloudxplorer.authservice.model;

public class LoginRequest {
    private String code;
    private String codeVerifier;
    private String redirectUri;

    public LoginRequest() {
    }

    public LoginRequest(String code, String codeVerifier, String redirectUri) {
        this.code = code;
        this.codeVerifier = codeVerifier;
        this.redirectUri = redirectUri;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCodeVerifier() {
        return codeVerifier;
    }

    public void setCodeVerifier(String codeVerifier) {
        this.codeVerifier = codeVerifier;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }
}
