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
public class JobsInitializer implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(JobsInitializer.class);

    private static final String JOB_GROUP = "metrics_jobs_group";

    private static final String TRIGGER_GROUP = "metrics_triggers_group";

    private final Scheduler scheduler;

    private final ResourceLoader resourceLoader;

    private final String scanPackages;

    public JobsInitializer(Scheduler scheduler, ResourceLoader resourceLoader, MetricsProperties env) {
        this.scheduler = scheduler;
        this.resourceLoader = resourceLoader;
        this.scanPackages = env.getJobPackages();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        getJobClasses().forEach(this::createJob);
    }

    public void createJob(Class<?> jobClass) {
        try {
            AbstractJob job = (AbstractJob) jobClass.getDeclaredConstructor().newInstance();
            TriggerKey triggerKey = TriggerKey.triggerKey(job.getName(), TRIGGER_GROUP);
            if (!job.getActivated()) {
                if (scheduler.checkExists(triggerKey)) {
                    JobDetail jobDetail = JobBuilder.newJob(job.getClass())
                            .withIdentity(job.getName(), JOB_GROUP)
                            .build();
                    scheduler.deleteJob(jobDetail.getKey());
                }
                return;
            }
            if (!scheduler.checkExists(triggerKey)) {
                JobDetail jobDetail = JobBuilder.newJob(job.getClass())
                        .withIdentity(job.getName(), JOB_GROUP)
                        .build();
                Trigger trigger = null;
                if (StringUtils.isNotBlank(job.getCronExpression())) {
                    CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(job.getCronExpression());
                    trigger = TriggerBuilder.newTrigger()
                            .withIdentity(job.getName(), TRIGGER_GROUP)
                            .withSchedule(scheduleBuilder)
                            .startNow().build();
                } else if (job.getFixedTime() != null) {
                    SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.repeatSecondlyForever(job.getFixedTime().intValue());
                    trigger = TriggerBuilder.newTrigger()
                            .withIdentity(job.getName(), TRIGGER_GROUP)
                            .withSchedule(scheduleBuilder)
                            .startNow().build();
                }
                if (trigger != null) {
                    scheduler.scheduleJob(jobDetail, trigger);
                    LOG.info("定时任务 \"{}[{}]\" 初始化成功", job.getName(), jobClass.getName());
                } else {
                    LOG.error("定时任务 \"{}[{}]\" 初始化失败, \"cron\" 和 \"fixed-time\"未配置有效值", job.getName(), jobClass.getName());
                }
            } else {
                LOG.info("定时任务 \"{}[{}]\" 已存在", job.getName(), jobClass.getName());
            }
            // 启动是否立即触发一次
            if (job.fireImmediatelyWhenServiceStartup()) {
                scheduler.triggerJob(scheduler.getTrigger(triggerKey).getJobKey());
            }
        } catch (Exception e) {
            LOG.error("定时任务 [{}] 初始化失败, Cause by: ", jobClass.getName(), e);
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
        LOG.debug("开始扫描 [{}] 包下的 Class", packageName);
        List<Class<?>> classes = new ArrayList<>();
        ResourcePatternResolver resolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
        MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory();
        Resource[] resources = resolver.getResources("classpath*:" + packageName.replaceAll("[.]", "/") + "/**/*.class");
        LOG.debug("在 [{}] 包下共找到 [{}] 个 Class 文件", packageName, resources.length);
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
        LOG.debug("在 [{}] 包下共找到 [{}] 个定时任务并加载成功", packageName, classes.size());
        return classes;
    }

}
