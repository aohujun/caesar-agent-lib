package com.hupu.msv.apm.agent.core.governance.gray.common;


import com.hupu.msv.apm.agent.core.context.ContextManager;
import com.hupu.msv.apm.agent.core.governance.gray.config.GrayCommonConfig;
import com.hupu.msv.apm.agent.core.governance.gray.config.GrayTag;
import com.hupu.msv.apm.agent.core.governance.gray.config.GrayRuleConfig;
import com.hupu.msv.apm.agent.core.governance.gray.strategy.predicate.GrayRouteStrategy;
import com.hupu.msv.apm.agent.core.plugin.loader.AgentClassLoader;

import java.util.*;

/**
 * @description:
 * @author: Aile
 * @create: 2019/11/06 19:09
 */
public enum GrayRouteManager {
    INSTANCE;


    private static HashMap<String, String> instanceMetadata = new HashMap<>();

    private static GrayRouteStrategy grayRouteStrategy = null;

    private static final GrayCommonConfig grayCommonConfig = new GrayCommonConfig();
    /**
     * 透传header是否覆盖流量染色
     */
    private volatile Boolean cover = false;

    public GrayRouteStrategy getGrayRouteStrategy() {
        return grayRouteStrategy;
    }

    public GrayCommonConfig getGrayCommonConfig() {
        return grayCommonConfig;
    }

    public Set<String> getExclusiveInstance(String serviceId) {
        return grayCommonConfig.getExclusiveInstanceMap().get(serviceId);
    }

    public LinkedHashMap<Long, GrayRuleConfig> getGrayRuleConfigMap() {
        return grayCommonConfig.getGrayRuleConfigMap();
    }
    public void setCover(Boolean cover){
       this.cover = cover;
    }
    public Boolean getCover() {
        return cover;
    }

    /**
     * 获取上下文的路由规则
     *
     * @param serviceId
     * @return
     */
    public GrayTag getContextGrayRule(String serviceId) {
        Object rule = ContextManager.getRuntimeContext().get(GrayConstant.GRAY_RULE);
        if (rule == null || Objects.equals(GrayConstant.EMPTY_GRAY_RULE, rule)) {
            return null;
        }
        HashMap<String, GrayTag> grayRule = (HashMap<String, GrayTag>) ContextManager.getRuntimeContext().get(GrayConstant.GRAY_RULE);
        if (!grayRule.containsKey(serviceId)) {
            return null;
        }
        return grayRule.get(serviceId);
    }

    public void init() {
        for (GrayRouteStrategy strategy : ServiceLoader.load(GrayRouteStrategy.class, AgentClassLoader.getDefault())) {
            //目前只同时支持一种策略
            if (strategy != null) {
                grayRouteStrategy = strategy;
                return;
            }
        }
    }

    public static HashMap<String, String> getInstanceMetadata() {
        return instanceMetadata;
    }

    public static void setInstanceMetadata(HashMap<String, String> instanceMetadata) {
        GrayRouteManager.instanceMetadata = instanceMetadata;
    }
}
