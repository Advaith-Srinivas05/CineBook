package com.CineBook.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CineBookDbProperties.class)
public class DatabaseBootstrapConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DatabaseBootstrapConfiguration.class);
    private static final Pattern DB_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]+$");
    private static final String DUPLICATE_DATABASE_SQLSTATE = "42P04";

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy(CineBookDbProperties dbProperties) {
        return flyway -> {
            ensureDatabaseExists(dbProperties);
            flyway.migrate();
        };
    }

    private void ensureDatabaseExists(CineBookDbProperties dbProperties) {
        String databaseName = dbProperties.getDatabaseName();
        validateDatabaseName(databaseName);

        try (Connection connection = DriverManager.getConnection(
                dbProperties.getAdminJdbcUrl(),
                dbProperties.getUsername(),
                dbProperties.getPassword())) {

            if (databaseExists(connection, databaseName)) {
                log.info("Database '{}' already exists", databaseName);
                return;
            }

            createDatabase(connection, databaseName);
            log.info("Database '{}' created", databaseName);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed while creating database '" + databaseName + "'", ex);
        }
    }

    private boolean databaseExists(Connection connection, String databaseName) throws SQLException {
        String sql = "SELECT 1 FROM pg_database WHERE datname = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, databaseName);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void createDatabase(Connection connection, String databaseName) throws SQLException {
        String createSql = "CREATE DATABASE \"" + databaseName + "\"";

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(createSql);
        } catch (SQLException ex) {
            // If another node creates it first, continue startup safely.
            if (DUPLICATE_DATABASE_SQLSTATE.equals(ex.getSQLState())) {
                log.info("Database '{}' was created concurrently", databaseName);
                return;
            }
            throw ex;
        }
    }

    private void validateDatabaseName(String databaseName) {
        if (databaseName == null || databaseName.isBlank()) {
            throw new IllegalArgumentException("Database name must not be blank");
        }

        if (!DB_NAME_PATTERN.matcher(databaseName).matches()) {
            throw new IllegalArgumentException(
                    "Database name contains invalid characters. Allowed: letters, digits, underscore.");
        }
    }
}
