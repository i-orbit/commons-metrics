package com.inmaytide.orbit.commons.metrics;

import com.inmaytide.orbit.commons.utils.ApplicationContextHolder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Configuration properties for metrics module and Quartz scheduler integration.
 * <p>
 * This class maps properties defined with prefix {@code metrics.*} in application configuration files.
 * </p>
 *
 * <pre>{@code
 * metrics:
 *   scheduler-instance-name: myScheduler
 *   persist: true
 *   job-packages: com.example.jobs
 *   data-source:
 *     driver: com.mysql.cj.jdbc.Driver
 *     url: jdbc:mysql://localhost:3306/demo
 *     user: root
 *     password: secret
 *     max-connections: 10
 * }</pre>
 *
 * @author inmaytide
 * @since 2023/5/30
 */
@Component
@ConfigurationProperties(prefix = "metrics")
public class MetricsProperties {

    /**
     * Custom name for the Quartz scheduler instance.
     * If not set, defaults to "{spring.application.name}SchedulerInstance".
     */
    private String schedulerInstanceName;

    /**
     * Whether job execution metadata should be persisted (using JDBC).
     */
    private boolean persist;

    /**
     * Data source configuration for Quartz persistence.
     */
    private DataSource dataSource = new DataSource();

    /**
     * Packages to scan for job classes.
     */
    private String jobPackages;

    public String getSchedulerInstanceName() {
        return StringUtils.defaultIfBlank(
                schedulerInstanceName,
                ApplicationContextHolder.getInstance().getProperty("spring.application.name") + "SchedulerInstance"
        );
    }

    public void setSchedulerInstanceName(String schedulerInstanceName) {
        this.schedulerInstanceName = schedulerInstanceName;
    }

    public boolean isPersist() {
        return persist;
    }

    public void setPersist(boolean persist) {
        this.persist = persist;
    }

    public DataSource getDataSource() {
        return Objects.requireNonNullElseGet(dataSource, DataSource::new);
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String getJobPackages() {
        return jobPackages;
    }

    public void setJobPackages(String jobPackages) {
        this.jobPackages = jobPackages;
    }

    /**
     * Nested class representing datasource configuration for scheduler persistence.
     */
    public static class DataSource {

        private String driver;

        private String url;

        private String user;

        private String password;

        private Integer maxConnections;

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

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        /**
         * Password (use caution with logs/config exposure).
         */
        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public Integer getMaxConnections() {
            return maxConnections;
        }

        public void setMaxConnections(Integer maxConnections) {
            this.maxConnections = maxConnections;
        }
    }
}
