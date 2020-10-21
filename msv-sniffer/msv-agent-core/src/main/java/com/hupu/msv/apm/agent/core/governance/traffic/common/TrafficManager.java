package com.hupu.msv.apm.agent.core.governance.traffic.common;

import com.hupu.msv.apm.agent.core.governance.traffic.strategy.TrafficStrategy;
import com.hupu.msv.apm.agent.core.plugin.loader.AgentClassLoader;

import java.util.ServiceLoader;

/**
 * @author: zhaoxudong
 * @date: 2019-12-12 10:26
 * @description:
 */
public enum TrafficManager {
    INSTANCE;
    private static TrafficStrategy trafficStrategy;
    public TrafficStrategy getTrafficStrategy(){
        return trafficStrategy;
    }

    public void init(){
        for (TrafficStrategy strategy : ServiceLoader.load(TrafficStrategy.class, AgentClassLoader.getDefault())) {
            //目前只同时支持一种策略
            if (strategy != null) {
                trafficStrategy = strategy;
                return;
            }
        }
    }
}
