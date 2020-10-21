package com.hupu.msv.apm.plugin.ribbon.loadbalancer;

import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.IRule;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.hupu.msv.apm.agent.core.governance.loadbalancer.common.LoadBalancerConstants.GLOBAL_KEY;

/**
 * @author: zhaoxudong
 * @date: 2020-02-03 13:31
 * @description:
 */
public class LoadBalancerRuleHandler {
    private static final ILog log = LogManager.getLogger(LoadBalancerRuleHandler.class);
    public static Map<String, IRule> SERVER_RULE_MAP = new ConcurrentHashMap<>();
    /**
     * 重新设置负载均衡策略。
     * 特殊配置优先级高于全局配置
     *
     * @param balancer
     */
    public static void resetRule(BaseLoadBalancer balancer) {
        if (SERVER_RULE_MAP.isEmpty()) {
            return;
        }
        String serverName = balancer.getName();
        if (SERVER_RULE_MAP.get(serverName) == null) {
            //全局配置
            setGlobalRule(balancer);
            return;
        }
        //针对特定服务的配置
        if (!balancer.getRule().getClass().getName().equalsIgnoreCase(SERVER_RULE_MAP.get(serverName).getClass().getName())) {
            balancer.setRule(SERVER_RULE_MAP.get(serverName));
        }

    }

    private static void setGlobalRule(BaseLoadBalancer balancer) {
        if (SERVER_RULE_MAP.get(GLOBAL_KEY) == null) {
            return;
        }
        if (!balancer.getRule().getClass().getName().equalsIgnoreCase(SERVER_RULE_MAP.get(GLOBAL_KEY).getClass().getName())) {
            balancer.setRule(SERVER_RULE_MAP.get(GLOBAL_KEY));
        }

    }

}
