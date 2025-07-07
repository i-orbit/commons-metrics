package com.inmaytide.orbit.commons.metrics;

import com.zaxxer.hikari.HikariDataSource;
import org.quartz.utils.ConnectionProvider;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Quartz connection provider using HikariCP.
 *
 * @author inmaytide
 * @since 2023/2/27
 */
public class HikariConnectionProvider implements ConnectionProvider {

    private String driver;
    private String url;
    private String username;
    private String password;
    private int maxConnections = 10; // default
    private HikariDataSource dataSource;

    @Override
    public void initialize() throws SQLException {
        if (url == null || url.isBlank()) {
            throw new SQLException("Unable to create database connection pool: JDBC URL cannot be null or blank");
        }

        if (driver == null || driver.isBlank()) {
            throw new SQLException("Unable to create database connection pool: Driver class cannot be null or blank");
        }

        if (maxConnections <= 0) {
            throw new SQLException("Unable to create database connection pool: maxConnections must be greater than zero");
        }

        dataSource = new HikariDataSource();
        dataSource.setPoolName("orbit-metrics-quartz");
        dataSource.setJdbcUrl(url);
        dataSource.setDriverClassName(driver);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setMaximumPoolSize(maxConnections);
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Connection pool has not been initialized.");
        }
        return dataSource.getConnection();
    }

    @Override
    public void shutdown() throws SQLException {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    // Getters and Setters

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }
}
