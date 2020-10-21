package com.hupu.msv.apm.plugin.hystrix.v1;

import com.hupu.msv.apm.agent.core.governance.circuitbreaker.properties.CircuitBreakerProperties;
import com.hupu.msv.apm.agent.core.governance.circuitbreaker.properties.MatchedRuleFactory;
import com.hupu.msv.apm.agent.core.governance.circuitbreaker.strategy.CircuitBreakerStrategy;
import com.hupu.msv.apm.plugin.hystrix.v1.factory.MatchedRelationRuleFactory;

import java.util.Map;

/**
 * @author chenbaochao
 * @version 0.0.1
 * @date 2019/11/17 4:51 下午
 */
public class HystrixCircuitBreakerStrategy implements CircuitBreakerStrategy {


    @Override
    public String name() {
        return "hystrix";
    }

    @Override
    public boolean usingMatch() {
        return true;
    }


}
