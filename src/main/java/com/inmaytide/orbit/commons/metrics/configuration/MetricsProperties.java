package com.inmaytide.orbit.commons.metrics.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author inmaytide
 * @since 2023/5/30
 */
@ConfigurationProperties(prefix = "metrics")
public class MetricsProperties {

    private String schedulerInstanceName;

    private boolean persist;

    private DataSource dataSource;

    private String jobPackages;

    private List<JobParam> jobParams = new ArrayList<>();

    public String getSchedulerInstanceName() {
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

    public List<JobParam> getJobParams() {
        return jobParams;
    }

    public void setJobParams(List<JobParam> jobParams) {
        this.jobParams = jobParams;
    }

    public JobParam getJobParam(String name) {
        return getJobParams().stream()
                .filter(e -> Objects.equals(name, e.getName()))
                .findFirst()
                .orElse(new JobParam());
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

    public static class JobParam {

        private String name;

        private String cron;

        private BigDecimal fixedTime;

        private Boolean activated;

        private Boolean fireImmediatelyWhenServiceStartup;

        private Map<String, Object> others;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }

        public BigDecimal getFixedTime() {
            return fixedTime;
        }

        public void setFixedTime(BigDecimal fixedTime) {
            this.fixedTime = fixedTime;
        }

        public Boolean getActivated() {
            if (activated == null) {
                activated = false;
            }
            return activated;
        }

        public void setActivated(Boolean activated) {
            this.activated = activated;
        }

        public Map<String, Object> getOthers() {
            return others;
        }

        public void setOthers(Map<String, Object> others) {
            this.others = others;
        }

        public Boolean isFireImmediatelyWhenServiceStartup() {
            return fireImmediatelyWhenServiceStartup != null && fireImmediatelyWhenServiceStartup;
        }

        public void setFireImmediatelyWhenServiceStartup(Boolean fireImmediatelyWhenServiceStartup) {
            this.fireImmediatelyWhenServiceStartup = fireImmediatelyWhenServiceStartup;
        }
    }

}
