package com.inmaytide.orbit.commons.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inmaytide.orbit.commons.metrics.configuration.JobParameter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.utils.DBConnectionManager;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author inmaytide
 * @since 2023/5/30
 */
public abstract class AbstractJob implements Job {

    private static final String SQL_GET_JOB_PARAMETER = "select name, activated, cron, fixed_time, fire_immediately_when_service_startup, others from job_parameter where name = ?";

    public abstract Logger getLogger();

    public abstract String getName();

    protected JobParameter parameter;

    @SuppressWarnings("unchecked")
    protected JobParameter getParameters() {
        if (parameter != null) {
            return parameter;
        }
        JobParameter parameter = new JobParameter(getName());
        try (Connection connection = DBConnectionManager.getInstance().getConnection("orbit");
             PreparedStatement statement = connection.prepareStatement(SQL_GET_JOB_PARAMETER)) {
            statement.setString(1, getName());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    parameter.setActivated(rs.getBoolean("activated"));
                    parameter.setCronExpression(rs.getString("cron"));
                    parameter.setFixedTime(rs.getBigDecimal("fixed_time"));
                    parameter.setFireImmediatelyWhenServiceStartup(rs.getBoolean("fire_immediately_when_service_startup"));
                    String others = rs.getString("others");
                    if (StringUtils.isNotBlank(others)) {
                        parameter.setOthers(new ObjectMapper().readValue(others, Map.class));
                    } else {
                        parameter.setOthers(Collections.emptyMap());
                    }
                } else {
                    getLogger().error("Parameters for the task named \"{}\" do not exist", getName());
                }
            }
        } catch (Exception e) {
            getLogger().error("Failed to load parameters for the task named \"{}\", Cause by: ", getName(), e);
        }
        return parameter;
    }

    /**
     * 定时任务 cron 表达式
     */
    public String getCronExpression() {
        return getParameters().getCronExpression();
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
    public boolean isActivated() {
        return getParameters().isActivated();
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
