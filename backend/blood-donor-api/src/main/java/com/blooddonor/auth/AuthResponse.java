package com.blooddonor.auth;

public record AuthResponse(String accessToken, long expiresIn, Account account) {
    public record Account(String email, String role) {}
}