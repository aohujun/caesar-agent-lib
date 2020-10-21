package com.hupu.msv.apm.agent.core.governance;

/**
 * @author: zhaoxudong
 * @date: 2019-10-29 14:32
 * @description: 微服务治理插件配置
 */
public class GovernanceConfig {

    /**
     * 微服务id
     */
    public static volatile String APP_ID = "";

    /**
     * 实例ip
     */
    public static volatile String INSTANCE_IP = "";

    /**
     * 实例名字
     */
    public static volatile String INSTANCE_NAME = "";

    /**
     * 当前的全局配置信息版本
     */
    public static volatile long CURRENT_GLOBAL_VERSION = 0;

    /**
     * 当前的限流配置信息版本
     */
    public static volatile long CURRENT_TRAFFIC_VERSION = 0;

    /**
     * 当前的灰度配置信息版本
     */
    public static volatile long CURRENT_GRAY_VERSION = 0;

    /**
     * 当前的熔断配置信息版本
     */
    public static volatile long CURRENT_CIRCUIT_BREAKER_VERSION = 0;

    /**
     * 当前的降级配置信息版本
     */
    public static volatile long CURRENT_DEGRADE_VERSION = 0;

    /**
     * 当前的负载均衡配置信息版本
     */
    public static volatile long CURRENT_LOAD_BALANCER_VERSION = 0;

    /**
     * 当前的eureka client health check配置信息版本
     */
    public static volatile long CURRENT_EUREKA_HEALTH_CHECK_VERSION = 0;

    /**
     * 限流开关
     */
    public static volatile boolean TRAFFIC_ENABLED = false;

    /**
     * 灰度开关
     */
    public static volatile boolean GRAY_ENABLED = false;

    /**
     * 熔断开关
     */
    public static volatile boolean CIRCUIT_BREAKER_ENABLED = true;

    /**
     * 降级开关
     */
    public static volatile boolean DEGRADE_ENABLED = false;

    /**
     * 负载均衡开关
     */
    public static volatile boolean LOAD_BALANCER_ENABLED = true;

    /**
     * eureka client health check开关
     */
    public static volatile boolean EUREKA_HEALTH_CHECK_ENABLED = true;

}
