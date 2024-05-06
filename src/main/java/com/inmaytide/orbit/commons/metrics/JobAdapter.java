package com.inmaytide.orbit.commons.metrics;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.time.StopWatch;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * @author inmaytide
 * @since 2023/5/30
 */
public interface JobAdapter extends Job {

    Logger getLogger();

    String getName();

    default @NonNull JobParameter getParameters() {
        return JobParametersHolder.get(getName());
    }

    /**
     * 定时任务 cron 表达式
     */
    default String getCronExpression() {
        return getParameters().getCronExpression();
    }

    /**
     * 任务执行间隔时间, 与cron表达式共存时无效
     */
    default BigDecimal getFixedTime() {
        return getParameters().getFixedTime();
    }

    /**
     * 是否激活
     */
    default boolean isActivated() {
        return getParameters().isActivated();
    }

    /**
     * 定时任务执行需要的其他参数配置
     */
    default JsonNode getOthers() {
        return getParameters().getOthers();
    }

    /**
     * 服务启动后是否立即执行一次
     */
    default boolean isFireImmediatelyWhenServiceStartup() {
        return getParameters().isFireImmediatelyWhenServiceStartup();
    }

    default boolean isReinitializeIfExistingAtServiceStartup() {
        return getParameters().isReinitializeIfExistingAtServiceStartup();
    }

    @Override
    default void execute(JobExecutionContext context) {
        if (!isActivated()) {
            getLogger().info("Scheduled task named \"{}\" was inactivated", getName());
            return;
        }
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

    void exec(JobExecutionContext context);
}
