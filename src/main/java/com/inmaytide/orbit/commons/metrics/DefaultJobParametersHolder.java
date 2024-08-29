package com.inmaytide.orbit.commons.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.apache.commons.lang3.StringUtils;
import org.quartz.utils.DBConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author inmaytide
 * @since 2023/8/3
 */
public class DefaultJobParametersHolder implements JobParametersHolder {

    private static final long CACHE_VALID_TIME_IN_SECONDS = 30 * 60;

    private static final Logger LOG = LoggerFactory.getLogger(DefaultJobParametersHolder.class);

    private static final String SQL_GET_JOB_PARAMETER = "select name, activated, cron, fixed_time, fire_immediately_when_service_startup, reinitialize_if_existing_at_service_startup, others from job_parameter where name = ?";

    private static final Map<String, JobParameter> CACHE = new ConcurrentHashMap<>();

    @Override
    public JobParameter get(String name) {
        JobParameter res = CACHE.get(name);
        if (res == null || Duration.between(res.getLoadTime(), Instant.now()).getSeconds() > CACHE_VALID_TIME_IN_SECONDS) {
            res = load(name);
        }
        return res;
    }

    private @NonNull JobParameter load(String name) {
        JobParameter parameter = new JobParameter(name);
        try (Connection connection = DBConnectionManager.getInstance().getConnection("orbit");
             PreparedStatement statement = connection.prepareStatement(SQL_GET_JOB_PARAMETER)) {
            statement.setString(1, name);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    parameter.setActivated(rs.getBoolean("activated"));
                    parameter.setCronExpression(rs.getString("cron"));
                    parameter.setFixedTime(rs.getBigDecimal("fixed_time"));
                    parameter.setFireImmediatelyWhenServiceStartup(rs.getBoolean("fire_immediately_when_service_startup"));
                    parameter.setReinitializeIfExistingAtServiceStartup(rs.getBoolean("reinitialize_if_existing_at_service_startup"));
                    String others = rs.getString("others");
                    if (StringUtils.isNotBlank(others)) {
                        parameter.setOthers(new ObjectMapper().readTree(others));
                    } else {
                        parameter.setOthers(JsonNodeFactory.instance.missingNode());
                    }
                } else {
                    LOG.error("Parameters for the task named \"{}\" do not exist", name);
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to load parameters for the task named \"{}\", Cause by: ", name, e);
        }
        CACHE.put(name, parameter);
        return parameter;
    }

}
