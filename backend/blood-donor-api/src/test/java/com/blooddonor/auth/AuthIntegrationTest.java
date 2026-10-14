package com.blooddonor.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-integration;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.jwt.secret=test-secret-123456789012345678901234567890",
        "app.admin.email=admin@example.com",
        "app.admin.password=Strong-Admin-Password-123!"
})
class AuthIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void userCanRegisterLoginAndManageOnlyTheirPrivateDonorProfile() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        String password = "Strong-User-Password-123!";
        String userToken = register(email, password);

        mockMvc.perform(get("/api/donors/admin").header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"));

        String loginToken = login(email, password);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Incorrect-Password-123!"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized());

        MvcResult created = mockMvc.perform(post("/api/donors")
                        .header("Authorization", bearer(loginToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Private Profile","age":30,"bloodGroup":"O+","phone":"+1 555 010 1000","city":"Boston","available":true,"publiclyListed":false}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerEmail").doesNotExist())
                .andReturn();
        Integer donorId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/donors/mine").header("Authorization", bearer(loginToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(donorId));

        String otherToken = register("other-" + UUID.randomUUID() + "@example.com", password);
        mockMvc.perform(delete("/api/donors/{id}", donorId)
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/donors/{id}", donorId)
                        .header("Authorization", bearer(loginToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void bootstrappedAdminCanReadAllDonors() throws Exception {
        String adminToken = login("admin@example.com", "Strong-Admin-Password-123!");

        mockMvc.perform(get("/api/donors/admin").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
    }

    private String register(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}