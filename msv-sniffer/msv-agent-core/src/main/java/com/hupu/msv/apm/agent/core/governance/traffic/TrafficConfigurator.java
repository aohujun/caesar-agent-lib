package com.hupu.msv.apm.agent.core.governance.traffic;

import com.hupu.msv.apm.agent.core.governance.ConfiguratorListener;
import com.hupu.msv.apm.agent.core.governance.GovernanceConfig;
import com.hupu.msv.apm.agent.core.governance.traffic.common.TrafficManager;
import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.hupu.msv.apm.network.governance.GovernanceConfigResponse;
import com.hupu.msv.apm.network.governance.Traffic;
import com.hupu.msv.apm.network.governance.TrafficConfig;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author: zhaoxudong
 * @date: 2019-10-29 15:38
 * @description: 限流配置管理
 */
public class TrafficConfigurator implements ConfiguratorListener {
    private static final ILog logger = LogManager.getLogger(TrafficConfigurator.class);
    /**
     * 规则map
     */
    private static Map<Long, TrafficConfig> map = new ConcurrentHashMap<>();

    @Override
    public void loadConfig(GovernanceConfigResponse response) {
        if (!response.hasTraffic()) {
            GovernanceConfig.CURRENT_TRAFFIC_VERSION = response.getGlobal().getVersion();
            return;
        }

        Traffic traffic = response.getTraffic();
        if ("true".equalsIgnoreCase(traffic.getEnabled())) {
            GovernanceConfig.TRAFFIC_ENABLED = true;
        } else if ("false".equalsIgnoreCase(traffic.getEnabled())) {
            GovernanceConfig.TRAFFIC_ENABLED = false;
        }
        loadConfig(traffic.getConfigList(), response.getGlobal().getVersion());
        logger.info("流控规则加载完成");
    }


    private void loadConfig(List<TrafficConfig> configList, long version) {
        if (configList == null || configList.size() == 0) {
            GovernanceConfig.CURRENT_TRAFFIC_VERSION = version;
            return;
        }
        try {
            configList.parallelStream().forEach(conf -> {
                map.put(conf.getId(), conf);
            });
            TrafficManager.INSTANCE.getTrafficStrategy().loadRule(map.values());
            GovernanceConfig.CURRENT_TRAFFIC_VERSION = version;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

}
