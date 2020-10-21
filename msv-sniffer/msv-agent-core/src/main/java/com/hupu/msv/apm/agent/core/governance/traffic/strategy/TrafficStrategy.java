package com.hupu.msv.apm.agent.core.governance.traffic.strategy;

import com.hupu.msv.apm.network.governance.TrafficConfig;

import java.util.Collection;

/**
 * @author: zhaoxudong
 * @date: 2019-12-12 10:28
 * @description:
 */
public interface TrafficStrategy {
    /**
     * 加载规则
     * @param trafficRules
     */
    void loadRule(Collection<TrafficConfig> trafficRules);
}
