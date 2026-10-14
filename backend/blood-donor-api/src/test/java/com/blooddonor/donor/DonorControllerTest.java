package com.blooddonor.donor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.blooddonor.config.CorsConfig;
import java.time.LocalDate;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DonorController.class)
@Import(CorsConfig.class)
@TestPropertySource(properties = "app.admin-api-key=test-admin-key")
class DonorControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DonorRepository repository;

    @Test
    void publicDirectoryOmitsPhoneNumbers() throws Exception {
        Donor donor = new Donor();
        donor.setId(1L);
        donor.setFullName("Taylor Donor");
        donor.setAge(30);
        donor.setBloodGroup("O+");
        donor.setPhone("+1 555 010 1000");
        donor.setCity("Boston");
        donor.setLastDonationDate(LocalDate.of(2025, 1, 1));
        donor.setPubliclyListed(true);
        when(repository.findByPubliclyListedTrueOrderByIdDesc()).thenReturn(java.util.List.of(donor));

        mockMvc.perform(get("/api/donors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Taylor Donor"))
                .andExpect(jsonPath("$[0].phone").doesNotExist())
                .andExpect(jsonPath("$[0].lastDonationDate").doesNotExist());
    }

    @Test
    void registrationValidatesInputAndSavesOptIn() throws Exception {
        when(repository.save(any(Donor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/donors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Taylor Donor","age":30,"bloodGroup":"O+","phone":"+1 555 010 1000","city":"Boston","available":true,"publiclyListed":true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publiclyListed").value(true))
                .andExpect(jsonPath("$.phone").doesNotExist());

        verify(repository).save(org.mockito.ArgumentMatchers.argThat(Donor::isPubliclyListed));
    }

    @Test
    void invalidRegistrationIsRejected() throws Exception {
        mockMvc.perform(post("/api/donors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Taylor Donor","age":17,"bloodGroup":"O+","phone":"+1 555 010 1000","city":"Boston"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteRequiresAdminKey() throws Exception {
        mockMvc.perform(delete("/api/donors/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void configuredAdminKeyAllowsDelete() throws Exception {
        when(repository.existsById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/donors/1").header("X-Admin-Key", "test-admin-key"))
                .andExpect(status().isNoContent());

        verify(repository).deleteById(1L);
    }

    @Test
    void corsAllowsViteDevOriginOnAlternatePort() throws Exception {
        mockMvc.perform(options("/api/donors")
                        .header("Origin", "http://localhost:5174")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5174"));
    }
}