package com.hupu.msv.apm.agent.core.governance.circuitbreaker.properties;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author chenbaochao
 * @version 0.0.1
 * @date 2019/11/17 5:26 下午
 */
public enum CircuitBreakerPropertiesCacheFactory {
    INSTANCE;

    private Map<String, CircuitBreakerProperties> cacheRules = new ConcurrentHashMap<>();

    public void addCacheRule(String key, CircuitBreakerProperties rule) {
        cacheRules.put(key, rule);
    }

    public void removeCacheRule(String key) {
        cacheRules.remove(key);
    }

    public boolean contains(String key) {
        return cacheRules.containsKey(cacheRules);
    }

    public CircuitBreakerProperties get(String key){
        return cacheRules.get(key);
    }

    public void clearCache(){
        cacheRules.clear();
    }

}
