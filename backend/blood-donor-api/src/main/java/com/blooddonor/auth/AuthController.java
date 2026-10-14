package com.blooddonor.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody AuthRequests.Credentials credentials) {
        return authService.register(credentials);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequests.Credentials credentials) {
        return authService.login(credentials);
    }

    @GetMapping("/me")
    public AuthResponse.Account me(Authentication authentication) {
        return new AuthResponse.Account(authentication.getName(),
                authentication.getAuthorities().stream()
                        .filter(authority -> authority.getAuthority().startsWith("ROLE_"))
                        .map(authority -> authority.getAuthority().substring("ROLE_".length()))
                        .findFirst().orElse("USER"));
    }
}