package com.lawfirm.archive.model;

public class LoginResponse {
    private String token;
    
    public LoginResponse() {
    }
    
    public LoginResponse(String token) {
        this.token = token;
    }
    
    // getters & setters
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
} 