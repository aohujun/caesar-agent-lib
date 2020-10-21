package com.hupu.msv.apm.agent.core.governance.circuitbreaker;

import com.hupu.msv.apm.agent.core.governance.GovernanceConstants;
import com.hupu.msv.apm.network.governance.CircuitBreakerConfig;
import com.hupu.msv.apm.network.governance.CircuitBreakerConfigOrBuilder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.hupu.msv.apm.agent.core.governance.GovernanceConstants.*;

/**
 * @author chenbaochao
 * @version 0.0.1
 * @date 2019/11/16 6:50 下午
 */
public enum CircuitBreakerRuleFactory {

    INSTANCE;
    /**
     * 每次接收到的增量规则
     * key为服务名+"#"+接口path
     */
    private Map<String, CircuitBreakerConfig> rules = new ConcurrentHashMap<>();


    public void load(List<CircuitBreakerConfig> configs) {
        for (CircuitBreakerConfig config : configs) {
//            if (config.getEnabled() == RULE_ENABLED_OPEN
//                    && config.getDelFlag() == RULE_DEL_FLAG_NORMAL) {
            String[] paths = config.getPath().split(",");
            for (String path : paths) {
                CircuitBreakerConfig breakerConfig = CircuitBreakerConfig.newBuilder().mergeFrom(config).setPath(path).build();
                rules.put(generateKey(breakerConfig.getServiceName(), breakerConfig.getPath()), breakerConfig);
            }
//            }
        }
    }

    public Map<String, CircuitBreakerConfig> getRules() {
        return rules;
    }

    public String generateKey(String serviceName, String path) {
        return serviceName + "#" + path;
    }


    public CircuitBreakerConfig getCircuitBreakerConfig(String servicePath) {
        if (servicePath == null) {
            return null;
        }
        CircuitBreakerConfig breakerConfig = rules.get(servicePath);
        if (breakerConfig!=null && breakerConfig.getEnabled() == RULE_ENABLED_OPEN && breakerConfig.getDelFlag() == RULE_DEL_FLAG_NORMAL) {
            return breakerConfig;
        }
        return null;
    }

}
