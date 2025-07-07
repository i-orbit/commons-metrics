package com.inmaytide.orbit.commons.metrics;

import com.inmaytide.orbit.commons.utils.CommonUtils;
import com.inmaytide.orbit.commons.utils.ReflectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

/**
 * Initializes and registers all scheduled jobs automatically after Spring Boot starts,
 * based on the configured job packages.
 *
 * <p>It supports automatic deletion/reinitialization and immediate fire behavior.</p>
 *
 * @author inmaytide
 * @since 2023/5/30
 */
@Component
@DependsOn({"applicationContextHolder", "jobParametersHolder"})
public class ScheduledTasksInitializer implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduledTasksInitializer.class);

    private static final String JOB_GROUP = "metrics_jobs_group";
    private static final String TRIGGER_GROUP = "metrics_triggers_group";

    private final Scheduler scheduler;
    private final String scanPackages;

    public ScheduledTasksInitializer(@Qualifier("scheduler") Scheduler scheduler, MetricsProperties env) {
        this.scheduler = scheduler;
        this.scanPackages = env.getJobPackages();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        for (Class<?> jobClass : getJobClasses()) {
            createScheduledTask(jobClass);
        }
    }

    private void createScheduledTask(Class<?> jobClass) {
        try {
            JobAdapter job = (JobAdapter) jobClass.getDeclaredConstructor().newInstance();
            String jobName = job.getName();
            TriggerKey triggerKey = TriggerKey.triggerKey(jobName, TRIGGER_GROUP);
            JobDetail jobDetail = createJobDetail(job);

            if (job.isDeactivated()) {
                LOG.warn("Task [{}] is deactivated. Skipping initialization and deleting any existing job.", jobName);
                deleteScheduledTaskIfExists(triggerKey, jobDetail);
                return;
            }

            if (job.isReinitializeIfExistsOnServiceStartup()) {
                LOG.info("Task [{}] is configured to reinitialize on startup. Deleting any existing job.", jobName);
                deleteScheduledTaskIfExists(triggerKey, jobDetail);
            }

            if (!scheduler.checkExists(triggerKey)) {
                Optional<ScheduleBuilder<?>> builder = createScheduleBuilder(job);
                if (builder.isEmpty()) {
                    LOG.error("Task [{}] initialization failed. Missing or invalid 'cron' and 'fixed-time' configuration.", jobName);
                    return;
                }
                scheduler.scheduleJob(jobDetail, createTrigger(job, builder.get()));
                LOG.info("Task [{}] initialized successfully.", jobName);
            } else {
                LOG.info("Task [{}] already exists. Skipping registration.", jobName);
            }

            if (job.isFireOnceOnServiceStartup()) {
                scheduler.triggerJob(jobDetail.getKey());
                LOG.info("Task [{}] triggered immediately after service startup.", jobName);
            }

        } catch (Exception e) {
            LOG.error("Failed to initialize task [{}]. Cause: {}", jobClass.getName(), e.getMessage(), e);
        }
    }

    private void deleteScheduledTaskIfExists(TriggerKey triggerKey, JobDetail jobDetail) throws SchedulerException {
        if (scheduler.checkExists(triggerKey)) {
            scheduler.deleteJob(jobDetail.getKey());
        }
    }

    private JobDetail createJobDetail(JobAdapter job) {
        return JobBuilder.newJob(job.getClass())
                .withIdentity(job.getName(), JOB_GROUP)
                .build();
    }

    private Trigger createTrigger(JobAdapter job, ScheduleBuilder<?> scheduleBuilder) {
        return TriggerBuilder.newTrigger()
                .withIdentity(job.getName(), TRIGGER_GROUP)
                .withSchedule(scheduleBuilder)
                .startNow()
                .build();
    }

    private Optional<ScheduleBuilder<?>> createScheduleBuilder(JobAdapter job) {
        if (StringUtils.isNotBlank(job.getCronExpression())) {
            return Optional.of(CronScheduleBuilder.cronSchedule(job.getCronExpression()));
        } else if (job.getFixedTime() != null && job.getFixedTime().intValue() > 0) {
            return Optional.of(SimpleScheduleBuilder.repeatSecondlyForever(job.getFixedTime().intValue()));
        }
        return Optional.empty();
    }

    protected Set<Class<?>> getJobClasses() throws IOException {
        if (StringUtils.isBlank(scanPackages)) {
            LOG.warn("No job packages configured via 'metrics.job-packages'. Skipping job scan.");
            return Collections.emptySet();
        }
        List<String> packages = CommonUtils.splitByCommas(scanPackages);
        LOG.info("Scanning {} package(s) from config: [{}]", packages.size(), scanPackages);
        Set<Class<?>> jobClasses = ReflectionUtils.findClasses(packages, JobAdapter.class, false, false);
        LOG.info("Found {} scheduled task(s) to initialize.", jobClasses.size());
        return jobClasses;
    }
}
