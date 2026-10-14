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
        if (adminEmail.isBlank() || !isStrong(adminPassword)) {
            throw new IllegalStateException("Configure ADMIN_EMAIL and a 16-character mixed-case ADMIN_PASSWORD");
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

    private boolean isStrong(String password) {
        return password.length() >= 16 && password.length() <= 72
                && password.matches("(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).*");
    }
}