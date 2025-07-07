package com.inmaytide.orbit.commons.metrics;

import org.quartz.Scheduler;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.quartz.AdaptableJobFactory;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import java.io.IOException;
import java.util.Properties;

/**
 * Quartz scheduler configuration with optional persistent job store support.
 * Jobs will be autowired with Spring dependencies.
 *
 * @author inmaytide
 * @since 2023/5/30
 */
@Configuration
public class SchedulerConfiguration {

    private final AutowireCapableBeanFactory autowireCapableBeanFactory;
    private final MetricsProperties properties;

    public SchedulerConfiguration(AutowireCapableBeanFactory autowireCapableBeanFactory,
                                  MetricsProperties properties) {
        this.autowireCapableBeanFactory = autowireCapableBeanFactory;
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean(JobParametersHolder.class)
    public JobParametersHolder jobParametersHolder() {
        return new JdbcJobParametersHolder();
    }

    /**
     * Job factory that supports Spring's dependency injection into Quartz jobs.
     */
    @Bean
    public AdaptableJobFactory adaptableJobFactory() {
        return new AdaptableJobFactory() {
            @Override
            protected @NonNull Object createJobInstance(@NonNull TriggerFiredBundle bundle) throws Exception {
                Object instance = super.createJobInstance(bundle);
                autowireCapableBeanFactory.autowireBean(instance);
                return instance;
            }
        };
    }

    /**
     * Creates the Quartz Scheduler Factory Bean with optional DB support.
     */
    @Bean(name = "schedulerFactory")
    public SchedulerFactoryBean schedulerFactoryBean() throws IOException {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setQuartzProperties(buildQuartzProperties());
        factory.setJobFactory(adaptableJobFactory());
        return factory;
    }

    /**
     * Main Scheduler bean.
     */
    @Bean(name = "scheduler")
    public Scheduler scheduler() throws IOException {
        return schedulerFactoryBean().getScheduler();
    }

    /**
     * Builds Quartz configuration properties with or without persistence.
     */
    private Properties buildQuartzProperties() throws IOException {
        Properties props = PropertiesLoaderUtils.loadAllProperties("quartz.properties");
        props.put("org.quartz.scheduler.instanceName", properties.getSchedulerInstanceName());

        if (properties.isPersist()) {
            MetricsProperties.DataSource ds = properties.getDataSource();
            props.put("org.quartz.dataSource.orbit.driver", ds.getDriver());
            props.put("org.quartz.dataSource.orbit.URL", ds.getUrl());
            props.put("org.quartz.dataSource.orbit.user", ds.getUser());
            props.put("org.quartz.dataSource.orbit.password", ds.getPassword());
            props.put("org.quartz.dataSource.orbit.maxConnections", String.valueOf(ds.getMaxConnections()));
        } else {
            // Switch to in-memory job store
            props.put("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
            // Remove DB-related properties if present
            props.keySet().removeIf(key -> key.toString().startsWith("org.quartz.jobStore")
                    || key.toString().startsWith("org.quartz.dataSource.orbit"));
        }

        return props;
    }
}
