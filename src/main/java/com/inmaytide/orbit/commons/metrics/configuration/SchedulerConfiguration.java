package com.inmaytide.orbit.commons.metrics.configuration;

import org.quartz.Scheduler;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.quartz.AdaptableJobFactory;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import java.io.IOException;
import java.util.Properties;

@Configuration
@ConditionalOnProperty(name = "metrics.persist", havingValue = "true")
public class SchedulerConfiguration {
    private final AdaptableJobFactory adaptableJobFactory;

    private final MetricsProperties properties;

    public SchedulerConfiguration(AdaptableJobFactory adaptableJobFactory, MetricsProperties properties) {
        this.adaptableJobFactory = adaptableJobFactory;
        this.properties = properties;
    }

    @Bean(name = "schedulerFactory")
    public SchedulerFactoryBean schedulerFactoryBean() throws IOException {
        // 读取 Quartz 配置文件, 并替换数据库列链接相关配置
        PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
        propertiesFactoryBean.setLocation(new ClassPathResource("quartz.properties"));
        Properties props = propertiesFactoryBean.getObject();
        if (props == null) {
            throw new IllegalArgumentException();
        }
        props.put("org.quartz.scheduler.instanceName", properties.getSchedulerInstanceName());
        props.put("org.quartz.dataSource.orbit.driver", properties.getDataSource().getDriver());
        props.put("org.quartz.dataSource.orbit.URL", properties.getDataSource().getURL());
        props.put("org.quartz.dataSource.orbit.user", properties.getDataSource().getUser());
        props.put("org.quartz.dataSource.orbit.password", properties.getDataSource().getPassword());
        props.put("org.quartz.dataSource.orbit.maxConnections", properties.getDataSource().getMaxConnections());

        // 创建SchedulerFactoryBean
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setQuartzProperties(props);
        factory.setJobFactory(adaptableJobFactory);

        return factory;
    }

    @Bean(name = "scheduler")
    public Scheduler scheduler() throws IOException {
        return schedulerFactoryBean().getScheduler();
    }
}
