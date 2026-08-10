package com.reclaim.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Custom DataSource configuration that normalises the DATABASE_URL
 * environment variable before passing it to HikariCP.
 *
 * Render (and Heroku) provide the Postgres URL in URI format:
 *   postgresql://user:pass@host/dbname   (or postgres://...)
 *
 * JDBC requires:
 *   jdbc:postgresql://user:pass@host/dbname
 *
 * This bean converts both forms automatically so the Render env var can
 * be pasted directly without any manual `jdbc:` prefix editing.
 */
@Configuration
public class DataSourceConfig {

    /**
     * Raw DATABASE_URL as set on Render / Heroku.
     * Falls back to the local JDBC URL for development.
     */
    @Value("${DATABASE_URL:jdbc:postgresql://localhost:5432/reclaim}")
    private String databaseUrl;

    @Value("${DB_USERNAME:postgres}")
    private String username;

    @Value("${DB_PASSWORD:postgres}")
    private String password;

    @Bean
    @Primary
    public DataSource dataSource() {
        String jdbcUrl = toJdbcUrl(databaseUrl);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setDriverClassName("org.postgresql.Driver");

        // Only set username/password when they are NOT already embedded in the URL.
        // Render's DATABASE_URL embeds credentials — HikariCP handles that fine.
        // The DB_USERNAME / DB_PASSWORD env vars are used for local dev.
        if (!jdbcUrl.contains("@")) {
            // Local JDBC URL without embedded credentials
            config.setUsername(username);
            config.setPassword(password);
        }

        config.setMaximumPoolSize(5);          // Keep low for free-tier Render
        config.setMinimumIdle(1);
        config.setConnectionTimeout(30_000);
        config.setIdleTimeout(600_000);
        config.setMaxLifetime(1_800_000);

        return new HikariDataSource(config);
    }

    /**
     * Convert postgresql:// or postgres:// URI → jdbc:postgresql:// JDBC URL.
     * Already-correct jdbc:postgresql:// URLs pass through unchanged.
     */
    static String toJdbcUrl(String url) {
        if (url == null) return null;
        if (url.startsWith("jdbc:")) return url;           // already JDBC
        if (url.startsWith("postgresql://")) {
            return "jdbc:" + url;
        }
        if (url.startsWith("postgres://")) {
            // postgres:// is an alias; JDBC driver only understands postgresql://
            return "jdbc:postgresql://" + url.substring("postgres://".length());
        }
        return url;
    }
}
