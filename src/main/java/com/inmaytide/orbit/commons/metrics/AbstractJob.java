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

    public String getCronExpression() {
        return ApplicationContextHolder.getInstance().getBean(MetricsProperties.class).getJobParam(getName()).getCorn();
    }

    public BigDecimal getFixedTime() {
        return ApplicationContextHolder.getInstance().getBean(MetricsProperties.class).getJobParam(getName()).getFixedTime();
    }

    public Boolean getActivated() {
        return ApplicationContextHolder.getInstance().getBean(MetricsProperties.class).getJobParam(getName()).getActivated();
    }

    public Map<String, Object> getOthers() {
        return ApplicationContextHolder.getInstance().getBean(MetricsProperties.class).getJobParam(getName()).getOthers();
    }

    public abstract Logger getLogger();

    public abstract String getName();

    public boolean fireImmediatelyWhenServiceStartup() {
        return false;
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
