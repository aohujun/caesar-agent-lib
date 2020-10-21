package com.hupu.msv.apm.agent.core.governance.loadbalancer.common;

/**
 * @author: zhaoxudong
 * @date: 2020-02-04 14:17
 * @description: 负载均衡器常量
 */
public class LoadBalancerConstants {
    /**
     * 默认策略
     */
    public static final String RULE_DEFAULT = "default";

    /**
     * 随机策略
     */
    public static final String RULE_RANDOM = "random";

    /**
     * 轮询策略
     */
    public static final String RULE_ROUND = "round";

    /**
     * 响应时间权重策略
     */
    public static final String RULE_RESPONSE_TIME_WEIGHT = "weight";

    /**
     * 最佳可用策略(选出最空闲的实例)
     */
    public static final String RULE_BEST_AVAILABLE = "bestAvailable";

    /**
     * 重试策略
     */
    public static final String RULE_RETRY = "retry";

    /**
     * 可用性过滤策略
     */
    public static final String RULE_AVAILABILITY_FILTERING = "availabilityFiltering";
    /**
     * 复合判断server所在区域的性能和server的可用性选择server。不常用
     */
    public static final String RULE_ZONE_AVOIDANCE = "zoneAvoidance";

    /**
     * 最小并发算法增强版：通过轮询+最小并发算法，解决转发低延迟接口时负载不均衡的问题，最终达到高、低延迟接口均能负载均衡
     */
    public static final String RULE_BEST_AVAILABLE_ROUND = "bestAvailableAndRound";

    public static final String GLOBAL_KEY = "*";

}
