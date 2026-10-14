package com.blooddonor.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthRequests {
    private AuthRequests() {}

    public record Credentials(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 1024)
            String password) {
    }
}