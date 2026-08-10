package com.reclaim.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

/**
 * Custom DataSource configuration that normalises the DATABASE_URL
 * environment variable before passing it to HikariCP.
 *
 * Render provides the Postgres URL in URI format:
 *   postgresql://user:pass@host/dbname
 *
 * JDBC requires a clean URL without embedded credentials:
 *   jdbc:postgresql://host:port/dbname
 * plus username and password set separately on the connection pool.
 *
 * This bean parses the URI, extracts credentials, and builds the
 * correct JDBC URL automatically.
 */
@Configuration
public class DataSourceConfig {

    @Value("${DATABASE_URL:jdbc:postgresql://localhost:5432/reclaim}")
    private String databaseUrl;

    @Value("${DB_USERNAME:postgres}")
    private String username;

    @Value("${DB_PASSWORD:postgres}")
    private String password;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(30_000);
        config.setIdleTimeout(600_000);
        config.setMaxLifetime(1_800_000);

        if (databaseUrl.startsWith("jdbc:")) {
            // Already a proper JDBC URL (local dev)
            config.setJdbcUrl(databaseUrl);
            config.setUsername(username);
            config.setPassword(password);
        } else {
            // URI format from Render/Heroku: postgresql://user:pass@host/db
            // or postgres://user:pass@host/db
            // Parse it and extract credentials separately.
            parseUri(databaseUrl, config);
        }

        return new HikariDataSource(config);
    }

    /**
     * Parse a postgresql:// or postgres:// URI into JDBC URL + credentials.
     *
     * Input:  postgresql://user:pass@host:port/dbname
     * Output: jdbcUrl  = jdbc:postgresql://host:port/dbname
     *         username = user
     *         password = pass
     */
    private void parseUri(String uri, HikariConfig config) {
        try {
            // Normalise postgres:// → postgresql:// so URI can parse it
            String normalised = uri;
            if (normalised.startsWith("postgres://")) {
                normalised = "postgresql://" + normalised.substring("postgres://".length());
            }

            // Use java.net.URI to parse
            URI parsed = new URI(normalised);

            String host = parsed.getHost();
            int port = parsed.getPort();
            String path = parsed.getPath(); // e.g. "/dbname"

            String jdbcUrl = "jdbc:postgresql://" + host;
            if (port > 0) {
                jdbcUrl += ":" + port;
            }
            if (path != null) {
                jdbcUrl += path;
            }

            config.setJdbcUrl(jdbcUrl);

            // Extract user:pass from the userInfo part
            String userInfo = parsed.getUserInfo();
            if (userInfo != null) {
                int colon = userInfo.indexOf(':');
                if (colon >= 0) {
                    config.setUsername(userInfo.substring(0, colon));
                    config.setPassword(userInfo.substring(colon + 1));
                } else {
                    config.setUsername(userInfo);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse DATABASE_URL: " + e.getMessage(), e);
        }
    }
}
