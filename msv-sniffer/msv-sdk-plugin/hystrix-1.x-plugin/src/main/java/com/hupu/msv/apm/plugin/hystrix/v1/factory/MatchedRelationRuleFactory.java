package com.hupu.msv.apm.plugin.hystrix.v1.factory;

import com.hupu.msv.apm.agent.core.governance.circuitbreaker.CircuitBreakerRuleFactory;
import com.hupu.msv.apm.agent.core.governance.circuitbreaker.properties.MatchedRuleFactory;
import com.hupu.msv.apm.network.governance.CircuitBreakerConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

/**
 * @author chenbaochao
 * @version 0.0.1
 * @date 2019/11/17 4:59 下午
 */
public class MatchedRelationRuleFactory extends MatchedRuleFactory {

    private static MatchedRelationRuleFactory instance = new MatchedRelationRuleFactory();

    public static MatchedRelationRuleFactory getInstance() {
        return instance;
    }

    private MatchedRelationRuleFactory() {
    }




    public void addRule(String serviceName, String path, String commandKey) {
        String servicePath = CircuitBreakerRuleFactory.INSTANCE.generateKey(serviceName, path);
        getMatchedRelations().put(commandKey, servicePath);
    }



    /**
     * 根据command key获取熔断配置
     *
     * @param commandKey
     */
    public CircuitBreakerConfig getCircuitBreakerConfig(String commandKey) {
        String servicePath = getMatchedRelations().get(commandKey);
        return CircuitBreakerRuleFactory.INSTANCE.getCircuitBreakerConfig(servicePath);
    }

    public void addReturnMap(String commandKey,Class<?> aClass) {
        getReturnMap().put(commandKey, aClass);
    }





}
