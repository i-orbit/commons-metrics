package com.inmaytide.orbit.commons.metrics;

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
 * Loads job parameters from the database, with a 30-minute memory cache.
 *
 * @author inmaytide
 * @since 2023/8/3
 */
public class JdbcJobParametersHolder implements JobParametersHolder {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcJobParametersHolder.class);

    private static final long CACHE_VALID_SECONDS = 30 * 60;

    private static final String SQL_GET_JOB_PARAMETER = """
                SELECT
                    name,
                    activated,
                    cron,
                    fixed_time,
                    fire_once_on_service_startup,
                    reinitialize_if_exists_on_service_startup,
                    others
                FROM job_parameter
                WHERE name = ?
            """;

    private static final Map<String, JobParameter> CACHE = new ConcurrentHashMap<>();

    @Override
    public JobParameter get(@NonNull String name) {
        JobParameter cached = CACHE.get(name);
        if (cached == null) {
            LOG.info("No cached JobParameter for task '{}', loading from database.", name);
            return reload(name);
        } else if (isExpired(cached)) {
            LOG.info("Cached JobParameter for task '{}' expired, reloading.", name);
            return reload(name);
        }
        LOG.debug("Using cached JobParameter for task '{}'", name);
        return cached;
    }

    private boolean isExpired(JobParameter parameter) {
        return Duration.between(parameter.getLoadTime(), Instant.now()).getSeconds() > CACHE_VALID_SECONDS;
    }

    private JobParameter reload(String name) {
        try (Connection conn = DBConnectionManager.getInstance().getConnection("orbit");
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_JOB_PARAMETER)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    JobParameter parameter = JobParameter.withName(name)
                            .activated(rs.getBoolean("activated"))
                            .cronExpression(rs.getString("cron"))
                            .fixedTime(rs.getBigDecimal("fixed_time"))
                            .fireOnceOnServiceStartup(rs.getBoolean("fire_once_on_service_startup"))
                            .reinitializeIfExistsOnServiceStartup(rs.getBoolean("reinitialize_if_exists_on_service_startup"))
                            .others(rs.getString("others"))
                            .build();
                    CACHE.put(name, parameter);
                    LOG.info("Successfully loaded JobParameter for task '{}'", name);
                    return parameter;
                } else {
                    throw new IllegalStateException("No JobParameter found for task name: " + name);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load job parameter for task: " + name, e);
        }
    }
}
