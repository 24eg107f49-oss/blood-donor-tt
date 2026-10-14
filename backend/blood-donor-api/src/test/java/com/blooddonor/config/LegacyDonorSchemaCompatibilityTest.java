package com.blooddonor.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LegacyDonorSchemaCompatibilityTest {
    @Test
    void makesRequiredLegacyNameColumnOptional() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.execute(org.mockito.ArgumentMatchers.<ConnectionCallback<Integer>>any())).thenAnswer(invocation -> {
            Connection connection = mock(Connection.class);
            DatabaseMetaData metadata = mock(DatabaseMetaData.class);
            ResultSet columns = mock(ResultSet.class);
            when(connection.getMetaData()).thenReturn(metadata);
            when(metadata.getColumns(null, null, "donors", "name")).thenReturn(columns);
            when(columns.next()).thenReturn(true);
            when(columns.getString("IS_NULLABLE")).thenReturn("NO");
            return invocation.<ConnectionCallback<Integer>>getArgument(0).doInConnection(connection);
        });

        new LegacyDonorSchemaCompatibility(jdbcTemplate).run(mock(ApplicationArguments.class));

        verify(jdbcTemplate).execute("ALTER TABLE donors MODIFY COLUMN name VARCHAR(100) NULL");
    }

    @Test
    void leavesCurrentSchemaUnchanged() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.execute(org.mockito.ArgumentMatchers.<ConnectionCallback<Integer>>any())).thenAnswer(invocation -> {
            Connection connection = mock(Connection.class);
            DatabaseMetaData metadata = mock(DatabaseMetaData.class);
            ResultSet columns = mock(ResultSet.class);
            when(connection.getMetaData()).thenReturn(metadata);
            when(metadata.getColumns(null, null, "donors", "name")).thenReturn(columns);
            when(columns.next()).thenReturn(false);
            return invocation.<ConnectionCallback<Integer>>getArgument(0).doInConnection(connection);
        });

        new LegacyDonorSchemaCompatibility(jdbcTemplate).run(mock(ApplicationArguments.class));

        verify(jdbcTemplate, never()).execute("ALTER TABLE donors MODIFY COLUMN name VARCHAR(100) NULL");
    }
}