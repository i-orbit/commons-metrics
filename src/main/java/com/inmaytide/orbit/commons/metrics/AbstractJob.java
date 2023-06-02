package com.inmaytide.orbit.commons.metrics;

import com.inmaytide.orbit.commons.metrics.configuration.MetricsProperties;
import com.inmaytide.orbit.commons.utils.ApplicationContextHolder;
import org.apache.commons.lang3.time.StopWatch;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author inmaytide
 * @since 2023/5/30
 */
public abstract class AbstractJob implements Job {

    public abstract Logger getLogger();

    public abstract String getName();

    protected MetricsProperties.JobParam getParameters() {
        return ApplicationContextHolder.getInstance().getBean(MetricsProperties.class).getJobParam(getName());
    }

    /**
     * 定时任务 cron 表达式
     */
    public String getCronExpression() {
        return getParameters().getCron();
    }

    /**
     * 任务执行间隔时间, 与cron表达式共存时无效
     */
    public BigDecimal getFixedTime() {
        return getParameters().getFixedTime();
    }

    /**
     * 是否激活
     */
    public Boolean getActivated() {
        return getParameters().getActivated();
    }

    /**
     * 定时任务执行需要的其他参数配置
     */
    public Map<String, Object> getOthers() {
        return getParameters().getOthers();
    }

    /**
     * 服务启动后是否立即执行一次
     */
    public boolean isFireImmediatelyWhenServiceStartup() {
        return getParameters().isFireImmediatelyWhenServiceStartup();
    }

    @Override
    public void execute(JobExecutionContext context) {
        StopWatch stopWatch = StopWatch.createStarted();
        getLogger().info("To start executing a scheduled task named \"{}\"", getName());
        try {
            exec(context);
            getLogger().info("The scheduled task named \"{}\" has been executed successfully, taking {} seconds to complete.", getName(), stopWatch.getTime(TimeUnit.SECONDS));
        } catch (Exception e) {
            getLogger().error("An error occurred while executing the scheduled task named \"{}\", Cause by: ", getName(), e);
        } finally {
            stopWatch.stop();
        }
    }

    protected abstract void exec(JobExecutionContext context);
}
