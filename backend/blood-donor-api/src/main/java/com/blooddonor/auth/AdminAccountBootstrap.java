package com.blooddonor.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.Locale;

@Component
public class AdminAccountBootstrap implements ApplicationRunner {
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminAccountBootstrap(AppUserRepository users,
                                 PasswordEncoder passwordEncoder,
                                 @Value("${app.admin.email:}") String adminEmail,
                                 @Value("${app.admin.password:}") String adminPassword) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail.trim().toLowerCase(Locale.ROOT);
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank() && adminPassword.isBlank()) return;
        if (adminEmail.isBlank() || adminPassword.isBlank() || adminPassword.length() > 1024) {
            throw new IllegalStateException("Configure ADMIN_EMAIL and a non-empty ADMIN_PASSWORD of at most 1024 characters");
        }

        AppUser admin = users.findByEmailIgnoreCase(adminEmail).orElse(null);
        if (admin == null) {
            admin = new AppUser(adminEmail, passwordEncoder.encode(adminPassword), UserRole.ADMIN);
        } else {
            admin.setRole(UserRole.ADMIN);
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        }
        users.save(admin);
    }

}