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
        SchemaCheck schema = jdbcTemplate.execute((ConnectionCallback<SchemaCheck>) connection -> {
            try (ResultSet columns = connection.getMetaData().getColumns(
                    connection.getCatalog(), null, "donors", "name")) {
                while (columns.next()) {
                    return new SchemaCheck(connection.getMetaData().getDatabaseProductName(),
                            "NO".equalsIgnoreCase(columns.getString("IS_NULLABLE")));
                }
                return new SchemaCheck(connection.getMetaData().getDatabaseProductName(), false);
            }
        });

        if (schema.requiredLegacyNameColumn()) {
            if (schema.databaseProduct().toLowerCase(java.util.Locale.ROOT).contains("postgres")) {
                jdbcTemplate.execute("ALTER TABLE donors ALTER COLUMN name DROP NOT NULL");
            } else {
                jdbcTemplate.execute("ALTER TABLE donors MODIFY COLUMN name VARCHAR(100) NULL");
            }
        }
    }

    record SchemaCheck(String databaseProduct, boolean requiredLegacyNameColumn) {}
}