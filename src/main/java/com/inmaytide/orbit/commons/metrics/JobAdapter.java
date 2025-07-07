package com.inmaytide.orbit.commons.metrics;

import com.fasterxml.jackson.databind.JsonNode;
import com.inmaytide.orbit.commons.utils.ApplicationContextHolder;
import com.inmaytide.orbit.commons.utils.NamedStopWatch;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;

/**
 * Base interface for scheduled jobs in Quartz.
 * <p>
 * Provides a standard structure for retrieving configuration parameters,
 * logging execution status, and measuring task duration.
 * </p>
 *
 * <p>
 * Implement this interface instead of {@link Job} directly to simplify
 * job development and enforce unified logging and parameter access.
 * </p>
 *
 * @author inmaytide
 * @since 2023/5/30
 */
public interface JobAdapter extends Job {

    /**
     * Returns the logger to be used for job execution logging.
     *
     * @return SLF4J logger instance
     */
    Logger getLogger();

    /**
     * Returns the unique name of the job.
     *
     * @return job name
     */
    String getName();

    /**
     * Retrieves the job parameter object from the Spring context.
     *
     * @return job parameter loaded from {@link JobParametersHolder}
     */
    default @NonNull JobParameter getParameters() {
        return ApplicationContextHolder.getInstance()
                .getBean(JobParametersHolder.class)
                .get(getName());
    }

    /**
     * Returns the cron expression configured for this job.
     *
     * @return cron expression as string, or null if not set
     */
    default String getCronExpression() {
        return getParameters().getCronExpression();
    }

    /**
     * Returns the fixed time interval for job execution (in minutes).
     * Ignored if cron expression is set.
     *
     * @return fixed interval as {@link BigDecimal}
     */
    default BigDecimal getFixedTime() {
        return getParameters().getFixedTime();
    }

    /**
     * Checks if the job is currently activated and should be executed.
     *
     * @return true if activated; false otherwise
     */
    default boolean isActivated() {
        return getParameters().isActivated();
    }

    /**
     * Checks if the job is deactivated.
     *
     * @return true if deactivated; false otherwise
     */
    default boolean isDeactivated() {
        return !isActivated();
    }

    /**
     * Returns additional job parameters as a JSON node.
     * Typically used for customized configurations.
     *
     * @return {@link JsonNode} containing additional parameters
     */
    default JsonNode getOthers() {
        return getParameters().getOthers();
    }

    /**
     * Indicates whether the job should be triggered once immediately
     * when the service starts.
     *
     * @return true if enabled; false otherwise
     */
    default boolean isFireOnceOnServiceStartup() {
        return getParameters().isFireOnceOnServiceStartup();
    }

    /**
     * Indicates whether the job should be re-initialized if it already exists
     * when the service starts.
     *
     * @return true if reinitialization is required; false otherwise
     */
    default boolean isReinitializeIfExistsOnServiceStartup() {
        return getParameters().isReinitializeIfExistsOnServiceStartup();
    }

    /**
     * Executes the Quartz job with standardized logging and stopwatch timing.
     * <p>
     * If the job is deactivated, it will not execute {@link #exec(JobExecutionContext, NamedStopWatch)}.
     * </p>
     *
     * @param context Quartz job context
     */
    @Override
    default void execute(JobExecutionContext context) {
        if (isDeactivated()) {
            getLogger().info("Scheduled task [{}] is deactivated and will not be executed.", getName());
            return;
        }
        NamedStopWatch stopWatch = NamedStopWatch.createStarted(getName());
        getLogger().info("Scheduled task [{}] execution started.", getName());
        try {
            exec(context, stopWatch);
        } catch (Exception e) {
            getLogger().error("Error while executing scheduled task [{}]. Cause: {}", getName(), e.getMessage(), e);
        } finally {
            stopWatch.stop(getLogger());
        }
    }

    /**
     * Abstract method to implement job-specific logic.
     * Executed only if the job is activated.
     *
     * @param context   Quartz job context
     * @param stopWatch stopwatch to record duration of execution
     * @throws Exception any job execution errors
     */
    void exec(JobExecutionContext context, NamedStopWatch stopWatch) throws Exception;
}
