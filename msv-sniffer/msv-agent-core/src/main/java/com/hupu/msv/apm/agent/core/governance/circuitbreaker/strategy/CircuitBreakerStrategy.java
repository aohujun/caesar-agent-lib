package com.hupu.msv.apm.agent.core.governance.circuitbreaker.strategy;


import com.hupu.msv.apm.agent.core.governance.circuitbreaker.properties.CircuitBreakerProperties;
import com.hupu.msv.apm.agent.core.governance.circuitbreaker.properties.MatchedRuleFactory;

import java.util.Map;

/**
 * @author chenbaochao
 * @version 0.0.1
 * @date 2019/11/16 8:05 下午
 */
public interface CircuitBreakerStrategy {

    String name();

    boolean usingMatch();

}
