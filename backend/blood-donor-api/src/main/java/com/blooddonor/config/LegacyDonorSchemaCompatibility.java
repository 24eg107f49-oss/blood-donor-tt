package com.blooddonor.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class LegacyDonorSchemaCompatibility implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    public LegacyDonorSchemaCompatibility(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer requiredLegacyNameColumns = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'donors'
                  AND column_name = 'name'
                  AND is_nullable = 'NO'
                """, Integer.class);

        if (requiredLegacyNameColumns != null && requiredLegacyNameColumns > 0) {
            jdbcTemplate.execute("ALTER TABLE donors MODIFY COLUMN name VARCHAR(100) NULL");
        }
    }
}