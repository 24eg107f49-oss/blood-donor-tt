package com.blooddonor.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.sql.ResultSet;

@Component
public class LegacyDonorSchemaCompatibility implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    public LegacyDonorSchemaCompatibility(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer requiredLegacyNameColumns = jdbcTemplate.execute((ConnectionCallback<Integer>) connection -> {
            try (ResultSet columns = connection.getMetaData().getColumns(
                    connection.getCatalog(), null, "donors", "name")) {
                while (columns.next()) {
                    return "NO".equalsIgnoreCase(columns.getString("IS_NULLABLE")) ? 1 : 0;
                }
                return 0;
            }
        });

        if (requiredLegacyNameColumns > 0) {
            jdbcTemplate.execute("ALTER TABLE donors MODIFY COLUMN name VARCHAR(100) NULL");
        }
    }
}