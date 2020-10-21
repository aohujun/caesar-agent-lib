package com.hupu.msv.apm.agent.core.governance.loadbalancer.strategy;

import com.hupu.msv.apm.network.governance.LoadBalancerConfig;

import java.util.List;

/**
 * @author: zhaoxudong
 * @date: 2020-02-04 13:17
 * @description:
 */
public interface LoadBalancerStrategy {
    void loadConfig(List<LoadBalancerConfig> configList);
}
