package com.inmaytide.orbit.commons.metrics;

import com.inmaytide.orbit.commons.metrics.configuration.MetricsProperties;
import org.apache.commons.lang3.StringUtils;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Automatic initialization of scheduled tasks based on the relevant configuration.
 *
 * @author inmaytide
 * @since 2023/5/30
 */
@Component
@DependsOn("applicationContextHolder")
public class ScheduledTasksInitializer implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduledTasksInitializer.class);

    private static final String JOB_GROUP = "metrics_jobs_group";

    private static final String TRIGGER_GROUP = "metrics_triggers_group";

    private final Scheduler scheduler;

    private final ResourceLoader resourceLoader;

    private final String scanPackages;

    public ScheduledTasksInitializer(Scheduler scheduler, ResourceLoader resourceLoader, MetricsProperties env) {
        this.scheduler = scheduler;
        this.resourceLoader = resourceLoader;
        this.scanPackages = env.getJobPackages();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        getJobClasses().forEach(this::createScheduledTask);
    }

    private void deleteScheduledTaskIfExist(TriggerKey triggerKey, JobDetail jobDetail) throws SchedulerException {
        if (scheduler.checkExists(triggerKey)) {
            scheduler.deleteJob(jobDetail.getKey());
        }
    }

    private JobDetail createJobDetail(AbstractJob job) {
        return JobBuilder.newJob(job.getClass())
                .withIdentity(job.getName(), JOB_GROUP)
                .build();
    }

    private Trigger createTrigger(AbstractJob job, ScheduleBuilder<?> scheduleBuilder) {
        return TriggerBuilder.newTrigger()
                .withIdentity(job.getName(), TRIGGER_GROUP)
                .withSchedule(scheduleBuilder)
                .startNow().build();
    }

    private Optional<ScheduleBuilder<?>> createScheduleBuilder(AbstractJob job) {
        if (StringUtils.isNotBlank(job.getCronExpression())) {
            return Optional.of(CronScheduleBuilder.cronSchedule(job.getCronExpression()));
        } else if (job.getFixedTime() != null) {
            return Optional.of(SimpleScheduleBuilder.repeatSecondlyForever(job.getFixedTime().intValue()));
        }
        return Optional.empty();
    }

    public void createScheduledTask(Class<?> jobClass) {
        try {
            AbstractJob job = (AbstractJob) jobClass.getDeclaredConstructor().newInstance();
            TriggerKey triggerKey = TriggerKey.triggerKey(job.getName(), TRIGGER_GROUP);
            JobDetail jobDetail = createJobDetail(job);
            if (!job.getActivated()) {
                LOG.warn("The scheduled task named \"{}[{}]\" is not active. Initialization is canceled, and any existing execution plans are cleared", job.getName(), jobClass.getName());
                deleteScheduledTaskIfExist(triggerKey, jobDetail);
                return;
            }
            if (!scheduler.checkExists(triggerKey)) {
                Optional<ScheduleBuilder<?>> builder = createScheduleBuilder(job);
                if (builder.isEmpty()) {
                    LOG.error("The scheduled task named \"{}[{}]\" failed to initialize. \"cron\" and \"fixed-time\" were not configured with valid values", job.getName(), jobClass.getName());
                    return;
                }
                scheduler.scheduleJob(jobDetail, createTrigger(job, builder.get()));
                LOG.info("The scheduled task named \"{}[{}]\" was initialized successfully", job.getName(), jobClass.getName());
            } else {
                LOG.info("The scheduled task named \"{}[{}]\" already exists", job.getName(), jobClass.getName());
            }
            if (job.isFireImmediatelyWhenServiceStartup()) {
                scheduler.triggerJob(scheduler.getTrigger(triggerKey).getJobKey());
            }
        } catch (Exception e) {
            LOG.error("The scheduled task \"{}\" failed to initialize, Cause by: ", jobClass.getName(), e);
        }
    }

    protected Set<Class<?>> getJobClasses() throws IOException {
        if (StringUtils.isBlank(scanPackages)) {
            LOG.info("The value of the \"metrics.job-packages\" property is empty, and no scheduled tasks have been initialized");
            return Collections.emptySet();
        }
        List<String> packages = Pattern.compile(",").splitAsStream(scanPackages)
                .map(StringUtils::trim)
                .distinct()
                .filter(StringUtils::isNotBlank)
                .toList();
        LOG.info("The value of the \"metrics.job-packages\" property is [{}], there are a total of {} packages that need to be scanned", scanPackages, packages.size());
        Set<Class<?>> classes = new HashSet<>();
        for (String packageName : packages) {
            classes.addAll(getJobClasses(packageName));
        }
        LOG.info("A total of {} scheduled tasks need to be initialized", classes.size());
        return classes;
    }

    protected List<Class<?>> getJobClasses(String packageName) throws IOException {
        LOG.debug("Start scanning class files under the \"{}\" package", packageName);
        List<Class<?>> classes = new ArrayList<>();
        ResourcePatternResolver resolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
        MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory();
        Resource[] resources = resolver.getResources("classpath*:" + packageName.replaceAll("[.]", "/") + "/**/*.class");
        LOG.debug("Found a total of {} class files under the \"{}\" package", resources.length, packageName);
        for (Resource resource : resources) {
            MetadataReader reader = metadataReaderFactory.getMetadataReader(resource);
            LOG.trace("Try to load class \"{}\"", reader.getClassMetadata().getClassName());
            try {
                Class<?> cls = ClassUtils.forName(reader.getClassMetadata().getClassName(), ClassUtils.getDefaultClassLoader());
                if (!cls.isInterface() && AbstractJob.class.isAssignableFrom(cls) && !Modifier.isAbstract(cls.getModifiers())) {
                    classes.add(cls);
                }
            } catch (NoClassDefFoundError | ClassNotFoundException | UnsupportedClassVersionError e) {
                LOG.trace("Failed to load class \"{}\"", reader.getClassMetadata().getClassName(), e);
            }
        }
        LOG.debug("Found a total of {} scheduled tasks under the \"{}}\" package and loaded successfully.", classes.size(), packageName);
        return classes;
    }

}
