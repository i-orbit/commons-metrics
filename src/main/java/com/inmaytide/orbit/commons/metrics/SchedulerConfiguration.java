package com.inmaytide.orbit.commons.metrics;

import org.quartz.Scheduler;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.quartz.AdaptableJobFactory;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import java.io.IOException;
import java.util.Properties;

@Configuration
public class SchedulerConfiguration {
    private final AutowireCapableBeanFactory autowireCapableBeanFactory;

    private final MetricsProperties properties;

    public SchedulerConfiguration(AutowireCapableBeanFactory autowireCapableBeanFactory, MetricsProperties properties) {
        this.autowireCapableBeanFactory = autowireCapableBeanFactory;
        this.properties = properties;
    }

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

    @Bean(name = "schedulerFactory")
    public SchedulerFactoryBean schedulerFactoryBean() throws IOException {
        Properties props = PropertiesLoaderUtils.loadAllProperties("quartz.properties");
        props.put("org.quartz.scheduler.instanceName", properties.getSchedulerInstanceName());
        if (properties.isPersist()) {
            props.put("org.quartz.dataSource.orbit.driver", properties.getDataSource().getDriver());
            props.put("org.quartz.dataSource.orbit.URL", properties.getDataSource().getURL());
            props.put("org.quartz.dataSource.orbit.user", properties.getDataSource().getUser());
            props.put("org.quartz.dataSource.orbit.password", properties.getDataSource().getPassword());
            props.put("org.quartz.dataSource.orbit.maxConnections", String.valueOf(properties.getDataSource().getMaxConnections()));
        } else {
            props.put("org.quartz.jobStore.class", org.quartz.simpl.RAMJobStore.class.getName());
            props.remove("org.quartz.jobStore.tablePrefix");
            props.remove("org.quartz.jobStore.driverDelegateClass");
            props.remove("org.quartz.jobStore.dataSource");
            props.remove("org.quartz.jobStore.useProperties");
            props.remove("org.quartz.jobStore.isClustered");
            props.remove("org.quartz.jobStore.maxMisfiresToHandleAtATime");
            props.remove("org.quartz.dataSource.orbit.connectionProvider.class");
            props.remove("org.quartz.dataSource.orbit.driver");
            props.remove("org.quartz.dataSource.orbit.URL");
            props.remove("org.quartz.dataSource.orbit.user");
            props.remove("org.quartz.dataSource.orbit.password");
            props.remove("org.quartz.dataSource.orbit.maxConnections");
        }

        // 创建SchedulerFactoryBean
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setQuartzProperties(props);
        factory.setJobFactory(adaptableJobFactory());
        return factory;
    }

    @Bean(name = "scheduler")
    public Scheduler scheduler() throws IOException {
        return schedulerFactoryBean().getScheduler();
    }
}
