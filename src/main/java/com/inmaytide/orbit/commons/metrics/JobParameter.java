package com.inmaytide.orbit.commons.metrics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inmaytide.orbit.Version;
import org.apache.commons.lang3.StringUtils;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * @author inmaytide
 * @since 2023/7/26
 */
public class JobParameter implements Serializable {

    @Serial
    private static final long serialVersionUID = Version.SERIAL_VERSION_UID;

    /**
     * Job name (must match AbstractJob.name)
     */
    private String name;

    /**
     * Whether this job is enabled
     */
    private boolean activated = false;

    /**
     * Cron expression. Takes precedence over fixedTime if both present.
     */
    private String cronExpression;

    /**
     * Fixed interval (in minutes) to execute. Ignored if cronExpression is set.
     */
    private BigDecimal fixedTime;

    /**
     * Whether to run once when the service starts
     */
    private boolean fireOnceOnServiceStartup;

    /**
     * If job exists on service start, whether to reinitialize it
     */
    private boolean reinitializeIfExistsOnServiceStartup;

    /**
     * Additional parameters for job logic
     */
    private JsonNode others;

    /**
     * Load time of the configuration
     */
    private final Instant loadTime = Instant.now();


    // Constructors
    public JobParameter() {
    }

    public JobParameter(String name) {
        this.name = name;
    }

    // Builder
    public static Builder withName(String name) {
        return new Builder().name(name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public BigDecimal getFixedTime() {
        return fixedTime;
    }

    public void setFixedTime(BigDecimal fixedTime) {
        this.fixedTime = fixedTime;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public boolean isFireOnceOnServiceStartup() {
        return fireOnceOnServiceStartup;
    }

    public void setFireOnceOnServiceStartup(boolean fireOnceOnServiceStartup) {
        this.fireOnceOnServiceStartup = fireOnceOnServiceStartup;
    }

    public boolean isReinitializeIfExistsOnServiceStartup() {
        return reinitializeIfExistsOnServiceStartup;
    }

    public void setReinitializeIfExistsOnServiceStartup(boolean reinitializeIfExistsOnServiceStartup) {
        this.reinitializeIfExistsOnServiceStartup = reinitializeIfExistsOnServiceStartup;
    }

    public JsonNode getOthers() {
        return others;
    }

    public void setOthers(JsonNode others) {
        this.others = others;
    }

    public Instant getLoadTime() {
        return loadTime;
    }


    public static class Builder {
        private String name;
        private boolean activated = false;
        private String cronExpression;
        private BigDecimal fixedTime;
        private boolean fireOnceOnServiceStartup = false;
        private boolean reinitializeIfExistsOnServiceStartup = false;
        private final Map<String, Object> others = new HashMap<>();

        private static final ObjectMapper MAPPER = new ObjectMapper();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder active() {
            this.activated = true;
            return this;
        }

        public Builder deactivate() {
            this.activated = false;
            return this;
        }

        public Builder activated(Boolean activated) {
            this.activated = activated != null && activated;
            return this;
        }

        public Builder cronExpression(String cronExpression) {
            this.cronExpression = cronExpression;
            return this;
        }

        public Builder fixedTime(BigDecimal fixedTime) {
            this.fixedTime = fixedTime;
            return this;
        }

        public Builder fireOnceOnServiceStartup(boolean value) {
            this.fireOnceOnServiceStartup = value;
            return this;
        }

        public Builder reinitializeIfExistsOnServiceStartup(boolean value) {
            this.reinitializeIfExistsOnServiceStartup = value;
            return this;
        }

        public Builder other(String key, Object value) {
            this.others.put(key, value);
            return this;
        }

        public Builder others(String json) {
            if (StringUtils.isBlank(json)) {
                return this;
            }
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> parsed = MAPPER.readValue(json, HashMap.class);
                this.others.putAll(parsed);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse JSON string to others", e);
            }
            return this;
        }

        public JobParameter build() {
            if (StringUtils.isBlank(name)) {
                throw new IllegalArgumentException("JobParameter 'name' must not be blank.");
            }
            if (StringUtils.isBlank(cronExpression) && (fixedTime == null || fixedTime.doubleValue() <= 0)) {
                throw new IllegalArgumentException("Either 'cronExpression' or a valid 'fixedTime' must be provided.");
            }

            JobParameter param = new JobParameter(name);
            param.setActivated(activated);
            param.setCronExpression(cronExpression);
            param.setFixedTime(fixedTime);
            param.setFireOnceOnServiceStartup(fireOnceOnServiceStartup);
            param.setReinitializeIfExistsOnServiceStartup(reinitializeIfExistsOnServiceStartup);
            param.setOthers(MAPPER.valueToTree(this.others));
            return param;
        }
    }

}
