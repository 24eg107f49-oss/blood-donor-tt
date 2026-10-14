package com.blooddonor.config;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DatabaseConfigurationTest {
    private final DatabaseConfiguration configuration = new DatabaseConfiguration();

    @Test
    void convertsRenderPostgresConnectionStringWithoutLosingEncodedCredentials() {
        DatabaseConfiguration.ParsedUrl parsed = configuration.parseUrl(
                "postgresql://db_user:p%2Bss%40word@db.example:5432/lifelink?sslmode=require");

        assertThat(parsed.jdbcUrl()).isEqualTo("jdbc:postgresql://db.example:5432/lifelink?sslmode=require");
        assertThat(parsed.username()).isEqualTo("db_user");
        assertThat(parsed.password()).isEqualTo("p+ss@word");
    }

    @Test
    void preservesJdbcUrlsAndMySqlConnectionStrings() {
        assertThat(configuration.parseUrl("jdbc:postgresql://db.example/lifelink").jdbcUrl())
                .isEqualTo("jdbc:postgresql://db.example/lifelink");
        assertThat(configuration.parseUrl("mysql://user:pass@db.example/lifelink").jdbcUrl())
                .isEqualTo("jdbc:mysql://db.example:3306/lifelink");
    }
}