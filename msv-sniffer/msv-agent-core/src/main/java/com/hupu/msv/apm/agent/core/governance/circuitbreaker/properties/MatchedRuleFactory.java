package com.hupu.msv.apm.agent.core.governance.circuitbreaker.properties;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class MatchedRuleFactory {

    private static Map<String,String> matchedRelations = new ConcurrentHashMap<>();
    private static Map<String,Class<?>> returnMap = new ConcurrentHashMap<>();


    public static Map<String, String> getMatchedRelations() {
        return matchedRelations;
    }

    public static Map<String, Class<?>> getReturnMap() {
        return returnMap;
    }
}
