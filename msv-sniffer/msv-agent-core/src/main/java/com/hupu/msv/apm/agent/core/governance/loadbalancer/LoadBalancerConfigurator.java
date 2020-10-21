package com.hupu.msv.apm.agent.core.governance.loadbalancer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hupu.msv.apm.agent.core.governance.ConfiguratorListener;
import com.hupu.msv.apm.agent.core.governance.GovernanceConfig;
import com.hupu.msv.apm.agent.core.governance.loadbalancer.common.LoadBalancerManager;
import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.hupu.msv.apm.network.governance.GovernanceConfigResponse;
import com.hupu.msv.apm.network.governance.LoadBalancer;
import com.hupu.msv.apm.network.governance.LoadBalancerConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: zhaoxudong
 * @date: 2020-02-03 16:34
 * @description:
 */
public class LoadBalancerConfigurator implements ConfiguratorListener {
    private static final ILog log = LogManager.getLogger(LoadBalancerConfigurator.class);
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    @Override
    public void loadConfig(GovernanceConfigResponse response) {
        if (!response.hasLoadBalancer()) {
            updateVersion(response);
            return;
        }
        LoadBalancer loadBalancer = response.getLoadBalancer();
        log.info("接收到负载均衡策略配置：{}", GSON.toJson(loadBalancer));
        if ("true".equalsIgnoreCase(loadBalancer.getEnabled())) {
            GovernanceConfig.LOAD_BALANCER_ENABLED = true;
        } else if ("false".equalsIgnoreCase(loadBalancer.getEnabled())) {
            GovernanceConfig.LOAD_BALANCER_ENABLED = false;
        }
        LoadBalancerManager.getLoadBalancerStrategy().loadConfig(loadBalancer.getConfigList());
        updateVersion(response);
    }

    private void updateVersion(GovernanceConfigResponse response) {
        GovernanceConfig.CURRENT_LOAD_BALANCER_VERSION = response.getGlobal().getVersion();
    }
}
