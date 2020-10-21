package com.hupu.msv.apm.agent.core.governance.loadbalancer.common;

import com.hupu.msv.apm.agent.core.boot.AgentPackageNotFoundException;
import com.hupu.msv.apm.agent.core.governance.loadbalancer.strategy.LoadBalancerStrategy;
import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.hupu.msv.apm.agent.core.plugin.loader.AgentClassLoader;
import com.hupu.msv.apm.agent.core.plugin.loader.InterceptorInstanceLoader;

import java.util.ServiceLoader;

/**
 * @author: zhaoxudong
 * @date: 2020-02-04 13:16
 * @description:
 */
public enum LoadBalancerManager {
    INSTANCE;
    private static final ILog logger = LogManager.getLogger(LoadBalancerManager.class);
    private static LoadBalancerStrategy balancerStrategy;

    public static LoadBalancerStrategy getLoadBalancerStrategy() {
        return balancerStrategy;
    }

    public void init() {
        for (LoadBalancerStrategy strategy : ServiceLoader.load(LoadBalancerStrategy.class, AgentClassLoader.getDefault())) {
            //目前只同时支持一种策略
            if (strategy != null) {
                try {
                    balancerStrategy = InterceptorInstanceLoader.load(strategy.getClass().getName(), ClassLoader.getSystemClassLoader());
                } catch (IllegalAccessException | InstantiationException | ClassNotFoundException | AgentPackageNotFoundException e) {
                    logger.error(e.getMessage(), e);
                }
                return;
            }
        }
    }
}
