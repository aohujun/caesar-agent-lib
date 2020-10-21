package com.hupu.msv.apm.agent.core.governance.gray.strategy.predicate;

import com.hupu.msv.apm.agent.core.governance.gray.config.GrayRuleConfig;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

/**
 * @description: 路由规则接口
 * @author: Aile
 * @create: 2019/11/04 18:51
 */
public interface GrayRouteStrategy {

    /**
     * 根据请求，匹配出符合条件的路由列表
     *
     * @param args
     * @return
     */
//    GrayRule matchedServiceList(Object args);


    /**
     * 刷新配置
     *
     * @param strategyMap
     */
    void refreshRouteTable(LinkedHashMap<String, GrayRuleConfig> strategyMap);

    void initRouteTable(LinkedHashMap<Long, GrayRuleConfig> strategyMap);

    LinkedHashMap<String, GrayRule> getFuzzyPathPredicate();

    HashMap<String, HashMap<String, GrayRule>> getPath2param();

    HashMap<String, GrayRule> getStrictPathPredicate();

    HashSet<String> getIgnoreHeader();

}
