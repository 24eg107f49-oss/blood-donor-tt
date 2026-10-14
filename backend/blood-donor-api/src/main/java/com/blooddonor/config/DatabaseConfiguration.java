package com.blooddonor.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Configuration
public class DatabaseConfiguration {
    @Bean
    @Primary
    DataSource dataSource(Environment environment,
                          @Value("${DB_URL:}") String dbUrl,
                          @Value("${DATABASE_URL:}") String databaseUrl,
                          @Value("${MYSQL_URL:}") String mysqlUrl,
                          @Value("${DB_USERNAME:}") String dbUsername,
                          @Value("${MYSQLUSER:}") String mysqlUser,
                          @Value("${DB_PASSWORD:}") String dbPassword,
                          @Value("${MYSQLPASSWORD:}") String mysqlPassword,
                          @Value("${DB_HOST:}") String dbHost,
                          @Value("${MYSQLHOST:}") String mysqlHost,
                          @Value("${DB_PORT:}") String dbPort,
                          @Value("${MYSQLPORT:}") String mysqlPort,
                          @Value("${DB_NAME:}") String dbName,
                          @Value("${MYSQLDATABASE:}") String mysqlDatabase) {
        String suppliedUrl = firstNonBlank(dbUrl, databaseUrl, mysqlUrl,
                environment.getProperty("spring.datasource.url"));
        String username = firstNonBlank(dbUsername, mysqlUser,
                environment.getProperty("spring.datasource.username"));
        String password = firstNonBlank(dbPassword, mysqlPassword,
                environment.getProperty("spring.datasource.password"));

        boolean render = hasText(environment.getProperty("PORT"));
        String jdbcUrl;
        if (hasText(suppliedUrl)) {
            ParsedUrl parsed = parseUrl(suppliedUrl.trim());
            jdbcUrl = parsed.jdbcUrl();
            username = firstNonBlank(username, parsed.username());
            password = firstNonBlank(password, parsed.password());
        } else {
            String host = firstNonBlank(dbHost, mysqlHost, "localhost");
            if (render && host.equalsIgnoreCase("localhost")) {
                throw new IllegalStateException("Set DB_URL to a reachable PostgreSQL or MySQL URL in Render; localhost is inside the API container");
            }
            String port = firstNonBlank(dbPort, mysqlPort, "3306");
            String database = firstNonBlank(dbName, mysqlDatabase, "blood_donor");
            jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        }

        if (render && (!hasText(username) || !hasText(password))) {
            throw new IllegalStateException("Set DB_USERNAME and DB_PASSWORD (or MYSQLUSER and MYSQLPASSWORD) in Render");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(firstNonBlank(username, "root"));
        config.setPassword(firstNonBlank(password, "root"));
        String driver = environment.getProperty("spring.datasource.driver-class-name");
        if (hasText(driver)) config.setDriverClassName(driver);
        return new HikariDataSource(config);
    }

    ParsedUrl parseUrl(String value) {
        if (value.startsWith("jdbc:mysql://") || value.startsWith("jdbc:postgresql://")) {
            return new ParsedUrl(value, "", "");
        }
        if (value.startsWith("jdbc:h2:")) return new ParsedUrl(value, "", "");
        boolean postgres = value.startsWith("postgresql://") || value.startsWith("postgres://");
        boolean mysql = value.startsWith("mysql://");
        if (!postgres && !mysql) {
            throw new IllegalStateException("Database URL must use a MySQL or PostgreSQL JDBC URL; URL value is not logged");
        }

        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Database URL is malformed; URL value is not logged");
        }
        String host = uri.getHost();
        String path = uri.getRawPath();
        if (!hasText(host) || path == null || path.length() < 2) {
            throw new IllegalStateException("Database URL must include a host and database name; URL value is not logged");
        }
        String database = path.substring(1);
        String query = uri.getRawQuery();
        String jdbcUrl = (postgres ? "jdbc:postgresql://" : "jdbc:mysql://")
            + host + (uri.getPort() < 0 ? (postgres ? ":5432" : ":3306") : ":" + uri.getPort())
                + "/" + database + (hasText(query) ? "?" + query : "");
        String username = "";
        String password = "";
        String userInfo = uri.getRawUserInfo();
        if (hasText(userInfo)) {
            String[] credentials = userInfo.split(":", 2);
            username = decode(credentials[0]);
            if (credentials.length == 2) password = decode(credentials[1]);
        }
        return new ParsedUrl(jdbcUrl, username, password);
    }

    private String decode(String value) {
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (hasText(value)) return value.trim();
        }
        return "";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    record ParsedUrl(String jdbcUrl, String username, String password) {}
}