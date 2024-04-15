package com.inmaytide.orbit.commons.metrics;

import com.inmaytide.orbit.commons.utils.ApplicationContextHolder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author inmaytide
 * @since 2023/5/30
 */
@Component
@ConfigurationProperties(prefix = "metrics")
public class MetricsProperties {

    private String schedulerInstanceName;

    private boolean persist;

    private DataSource dataSource;

    private String jobPackages;

    public String getSchedulerInstanceName() {
        if (StringUtils.isBlank(schedulerInstanceName)) {
            return ApplicationContextHolder.getInstance().getProperty("spring.application.name") + "SchedulerInstance";
        }
        return schedulerInstanceName;
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
        return dataSource;
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

    public static class DataSource {

        private String driver;

        private String URL;

        private String user;

        private String password;

        private Integer maxConnections;

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

        public Integer getMaxConnections() {
            return maxConnections;
        }

        public void setMaxConnections(Integer maxConnections) {
            this.maxConnections = maxConnections;
        }
    }

}
