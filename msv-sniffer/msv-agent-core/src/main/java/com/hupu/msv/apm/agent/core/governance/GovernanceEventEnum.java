package com.hupu.msv.apm.agent.core.governance;

/**
 * @author: zhaoxudong
 * @date: 2019-11-01 17:07
 * @description:
 */
public enum GovernanceEventEnum {

    /**
     * 熔断
     */
    CIRCUITBREAKER("circuitBreaker"),

    /**
     * 降级
     */
    DEGRADE("degrade"),

    /**
     * 灰度，分流
     */
    GRAY("gray"),

    /**
     * 限流
     */
    TRAFFIC("traffic"),

    /**
     * 负载均衡器
     */
    LOADBALANCER("loadBalancer"),

    /**
     * eureka health check
     */
    EUREKA_HEALTH_CHECK("eurekaHealthCheck");

    private String name;

    GovernanceEventEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
