package com.hupu.msv.apm.agent.core.governance.degrade;


import com.hupu.msv.apm.agent.core.governance.ConfiguratorListener;
import com.hupu.msv.apm.agent.core.governance.GovernanceConfig;
import com.hupu.msv.apm.agent.core.governance.traffic.TrafficConfigurator;
import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.hupu.msv.apm.network.governance.Degrade;
import com.hupu.msv.apm.network.governance.DegradeConfig;
import com.hupu.msv.apm.network.governance.GovernanceConfigResponse;
import com.hupu.msv.apm.network.governance.Traffic;

/**
 * @author: zhaoxudong
 * @date: 2019-10-29 17:30
 * @description:
 */
public class DegradeConfigurator implements ConfiguratorListener {
    private static final ILog logger = LogManager.getLogger(DegradeConfigurator.class);
    @Override
    public void loadConfig(GovernanceConfigResponse response) {
        if (!response.hasDegrade()) {
            updateVersion(response);
            return;
        }

        Degrade degrade = response.getDegrade();
        if ("true".equalsIgnoreCase(degrade.getEnabled())) {
            GovernanceConfig.DEGRADE_ENABLED = true;
        } else if ("false".equalsIgnoreCase(degrade.getEnabled())) {
            GovernanceConfig.DEGRADE_ENABLED = false;
        }
        DegradeRuleFactory.INSTANCE.load(degrade.getConfigList());
        updateVersion(response);
        logger.info("降级规则加载完毕");
    }

    private void updateVersion(GovernanceConfigResponse response) {
        GovernanceConfig.CURRENT_DEGRADE_VERSION = response.getGlobal().getVersion();
    }
}
