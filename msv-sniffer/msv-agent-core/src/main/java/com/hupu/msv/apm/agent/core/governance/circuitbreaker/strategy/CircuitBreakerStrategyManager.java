package com.hupu.msv.apm.agent.core.governance.circuitbreaker.strategy;

import com.hupu.msv.apm.agent.core.governance.circuitbreaker.CircuitBreakerConfigurator;
import com.hupu.msv.apm.agent.core.governance.circuitbreaker.CircuitBreakerRuleFactory;
import com.hupu.msv.apm.agent.core.governance.circuitbreaker.properties.CircuitBreakerProperties;
import com.hupu.msv.apm.agent.core.governance.circuitbreaker.properties.CircuitBreakerPropertiesCacheFactory;
import com.hupu.msv.apm.agent.core.governance.circuitbreaker.properties.MatchedRuleFactory;
import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.hupu.msv.apm.agent.core.plugin.loader.AgentClassLoader;
import com.hupu.msv.apm.network.governance.CircuitBreakerConfig;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * @author chenbaochao
 * @version 0.0.1
 * @date 2019/11/16 10:47 下午
 */
public enum CircuitBreakerStrategyManager {

    INSTANCE;

    private static final ILog log = LogManager.getLogger(CircuitBreakerStrategyManager.class);

    private CircuitBreakerStrategy circuitBreakerStrategy;

    public void init() {
        for (CircuitBreakerStrategy circuitBreakerStrategy : ServiceLoader.load(CircuitBreakerStrategy.class, AgentClassLoader.getDefault())) {
            //目前只同时支持一种熔断策略
            if (circuitBreakerStrategy != null) {
                this.circuitBreakerStrategy = circuitBreakerStrategy;
                log.info("加载到熔断策略：{}", circuitBreakerStrategy.name());
                return;
            }
        }
    }


    public void removeCircuitBreakerRuleCache(List<CircuitBreakerConfig> rules) {
        rules.parallelStream().forEach(rule -> {
            String[] paths = rule.getPath().split(",");
            for(String path:paths){
                CircuitBreakerConfig breakerConfig = CircuitBreakerConfig.newBuilder().mergeFrom(rule).setPath(path).build();
                String servicePathKey = CircuitBreakerRuleFactory.INSTANCE.generateKey(breakerConfig.getServiceName(),breakerConfig.getPath());
                if (circuitBreakerStrategy.usingMatch()) {
                    Map<String, String> matchedRelation = MatchedRuleFactory.getMatchedRelations();
                    if(matchedRelation.isEmpty()){
                        return;
                    }
                    matchedRelation.forEach((commandkey,servicePath)->{
                        if(servicePathKey.equals(servicePath)){
                            CircuitBreakerPropertiesCacheFactory.INSTANCE.removeCacheRule(commandkey);
                        }
                    });

                } else {
                    CircuitBreakerPropertiesCacheFactory.INSTANCE.removeCacheRule(servicePathKey);
                }
            }
        });

    }

}
