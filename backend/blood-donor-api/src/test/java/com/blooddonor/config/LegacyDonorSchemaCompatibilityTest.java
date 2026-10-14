package com.blooddonor.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LegacyDonorSchemaCompatibilityTest {
    @Test
    void makesRequiredLegacyNameColumnOptional() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);

        new LegacyDonorSchemaCompatibility(jdbcTemplate).run(mock(ApplicationArguments.class));

        verify(jdbcTemplate).execute("ALTER TABLE donors MODIFY COLUMN name VARCHAR(100) NULL");
    }

    @Test
    void leavesCurrentSchemaUnchanged() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(0);

        new LegacyDonorSchemaCompatibility(jdbcTemplate).run(mock(ApplicationArguments.class));

        verify(jdbcTemplate, never()).execute(anyString());
    }
}