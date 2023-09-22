package com.inmaytide.orbit.commons.metrics;

import com.zaxxer.hikari.HikariDataSource;
import org.quartz.utils.ConnectionProvider;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author inmaytide
 * @since 2023/2/27
 */
public class HikariConnectionProvider implements ConnectionProvider {

    public String driver;

    public String URL;

    public String user;

    public String password;

    public int maxConnections;

    private HikariDataSource dataSource;

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void shutdown() throws SQLException {
        dataSource.close();
    }

    @Override
    public void initialize() throws SQLException {
        if (this.URL == null) {
            throw new SQLException("Unable to create database connection pool: Database URL cannot be null");
        }

        if (this.driver == null) {
            throw new SQLException("Unable to create database connection pool: Database driver class name cannot be null");
        }

        if (this.maxConnections < 0) {
            throw new SQLException("Unable to create database connection pool: Max connections must be greater than zero");
        }

        dataSource = new HikariDataSource();
        dataSource.setPoolName("orbit-metrics-quartz");
        dataSource.setJdbcUrl(URL);
        dataSource.setDriverClassName(driver);
        dataSource.setUsername(user);
        dataSource.setPassword(password);
        dataSource.setMaximumPoolSize(maxConnections);
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
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
