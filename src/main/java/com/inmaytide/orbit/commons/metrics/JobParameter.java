package com.inmaytide.orbit.commons.metrics;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * @author inmaytide
 * @since 2023/7/26
 */
public class JobParameter implements Serializable {

    @Serial
    private static final long serialVersionUID = -5778856660833466483L;

    /**
     * 任务名称, 与 AbstractJob 中的 name 属性对应
     */
    private String name;

    /**
     * 任务是否激活，参数加载失败时为 false
     */
    private boolean activated = false;

    /**
     * cron 表达式, 与 fixedTime 同时有值, cron有效
     */
    private String cronExpression;

    /**
     * 每 n 分钟执行一次, 与 cronExpression 同时有值时无效
     */
    private BigDecimal fixedTime;

    /**
     * 是否在服务启动后执行一次
     */
    private boolean fireImmediatelyWhenServiceStartup = false;

    /**
     * 在服务启动时，如果已存在则重新初始化
     */
    private boolean reinitializeIfExistingAtServiceStartup = false;

    /**
     * 其他任务执行时必要的配置参数
     */
    private JsonNode others;

    private final Instant loadTime;

    public JobParameter() {
        this.loadTime = Instant.now();
    }

    public JobParameter(String name) {
        this.name = name;
        this.loadTime = Instant.now();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public BigDecimal getFixedTime() {
        return fixedTime;
    }

    public void setFixedTime(BigDecimal fixedTime) {
        this.fixedTime = fixedTime;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public boolean isFireImmediatelyWhenServiceStartup() {
        return fireImmediatelyWhenServiceStartup;
    }

    public void setFireImmediatelyWhenServiceStartup(boolean fireImmediatelyWhenServiceStartup) {
        this.fireImmediatelyWhenServiceStartup = fireImmediatelyWhenServiceStartup;
    }

    public boolean isReinitializeIfExistingAtServiceStartup() {
        return reinitializeIfExistingAtServiceStartup;
    }

    public void setReinitializeIfExistingAtServiceStartup(boolean reinitializeIfExistingAtServiceStartup) {
        this.reinitializeIfExistingAtServiceStartup = reinitializeIfExistingAtServiceStartup;
    }

    public JsonNode getOthers() {
        return others;
    }

    public void setOthers(JsonNode others) {
        this.others = others;
    }

    public Instant getLoadTime() {
        return loadTime;
    }

}
