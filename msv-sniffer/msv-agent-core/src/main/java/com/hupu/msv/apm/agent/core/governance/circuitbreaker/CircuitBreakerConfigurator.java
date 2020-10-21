package com.hupu.msv.apm.agent.core.governance.circuitbreaker;


import com.hupu.msv.apm.agent.core.governance.ConfiguratorListener;
import com.hupu.msv.apm.agent.core.governance.GovernanceConfig;
import com.hupu.msv.apm.agent.core.governance.circuitbreaker.properties.CircuitBreakerPropertiesCacheFactory;
import com.hupu.msv.apm.agent.core.governance.circuitbreaker.strategy.CircuitBreakerStrategy;
import com.hupu.msv.apm.agent.core.governance.circuitbreaker.strategy.CircuitBreakerStrategyManager;
import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.hupu.msv.apm.network.governance.*;

import java.util.List;
import java.util.Objects;

/**
 * @author: chenbaochao
 * @date: 2019-10-29 17:29
 * @description:
 */
public class CircuitBreakerConfigurator implements ConfiguratorListener {

    private static final ILog logger = LogManager.getLogger(CircuitBreakerConfigurator.class);


    @Override
    public void loadConfig(GovernanceConfigResponse response) {
        if (!response.hasCircuitBreaker()) {
            updateVersion(response.getGlobal().getVersion());
            return;
        }

        CircuitBreaker circuitBreaker = response.getCircuitBreaker();

        boolean oldEnabled=GovernanceConfig.CIRCUIT_BREAKER_ENABLED;

        if ("true".equalsIgnoreCase(circuitBreaker.getEnabled())) {
            GovernanceConfig.CIRCUIT_BREAKER_ENABLED = true;
        } else if ("false".equalsIgnoreCase(circuitBreaker.getEnabled())) {
            GovernanceConfig.CIRCUIT_BREAKER_ENABLED  = false;
        }

        if(!Objects.equals(oldEnabled,GovernanceConfig.CIRCUIT_BREAKER_ENABLED)){
            CircuitBreakerPropertiesCacheFactory.INSTANCE.clearCache();
        }

        CircuitBreakerStrategyManager.INSTANCE.removeCircuitBreakerRuleCache(circuitBreaker.getConfigList());

        CircuitBreakerRuleFactory.INSTANCE.load(circuitBreaker.getConfigList());


        updateVersion(response.getGlobal().getVersion());

        logger.info("熔断规则加载完成");
    }

    private void updateVersion(long version) {
        GovernanceConfig.CURRENT_CIRCUIT_BREAKER_VERSION = version;
    }


}
