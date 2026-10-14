package com.blooddonor.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.blooddonor.config.SecurityConfig;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.jwt.secret=test-secret-123456789012345678901234567890")
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void registrationCreatesOnlyAUserAccount() throws Exception {
        when(authService.register(any())).thenReturn(new AuthResponse(
                "signed-token", 900, new AuthResponse.Account("user@example.com", "USER")));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com","password":"Strong-pass-123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.account.role").value("USER"))
                .andExpect(jsonPath("$.accessToken").value("signed-token"));

        verify(authService).register(any());
    }

    @Test
    void weakRegistrationPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com","password":"weak"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginIsPublicAndUsesAuthService() throws Exception {
        when(authService.login(any())).thenReturn(new AuthResponse(
                "signed-token", 900, new AuthResponse.Account("user@example.com", "USER")));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com","password":"Strong-pass-123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.email").value("user@example.com"));

        verify(authService).login(any());
    }

    @Test
    void accountDetailsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void accountDetailsIncludeTheAuthenticatedRole() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@example.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }
}