package com.hupu.msv.apm.plugin.spring.cloud.netflix.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hupu.msv.apm.agent.core.context.ContextManager;
import com.hupu.msv.apm.agent.core.context.trace.AbstractSpan;
import com.hupu.msv.apm.agent.core.governance.GovernanceConfig;
import com.hupu.msv.apm.agent.core.governance.GovernanceEventEnum;
import com.hupu.msv.apm.agent.core.governance.gray.common.GrayConstant;
import com.hupu.msv.apm.agent.core.governance.gray.common.GrayRouteManager;
import com.hupu.msv.apm.agent.core.governance.gray.strategy.predicate.GrayRule;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import com.hupu.msv.apm.plugin.governance.common.GovernanceReportUtil;
import com.hupu.msv.apm.plugin.gray.common.strategy.predicate.impl.GrayMatchService;
import com.netflix.zuul.context.RequestContext;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class PreDecorationFilterInterceptor implements InstanceMethodsAroundInterceptor {
    private static Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {

        if (!GovernanceConfig.GRAY_ENABLED) {
            return;
        }

        RequestContext ctx = RequestContext.getCurrentContext();
        AbstractSpan abstractTracingSpan = ContextManager.activeSpan();
        try {

            GrayRule grayRule = GrayMatchService.INSTANCE.matchedServiceList(ctx.getRequest());
            if (grayRule == null) {
                ContextManager.getRuntimeContext().put(GrayConstant.GRAY_RULE, GrayConstant.EMPTY_GRAY_RULE);
            } else {
                ContextManager.getRuntimeContext().put(GrayConstant.GRAY_RULE, grayRule.getGrayTag());
                RequestContext.getCurrentContext().addZuulRequestHeader(GrayConstant.GRAY_RULE, gson.toJson(grayRule.getGrayTag()));
                Map<String, String> eventMap = new HashMap<String, String>();
                eventMap.put(GrayConstant.GRAY_RULE, "strategy=" + gson.toJson(grayRule));
                abstractTracingSpan.log(System.currentTimeMillis(), eventMap);
                GovernanceReportUtil.recordCount(GovernanceEventEnum.GRAY, String.valueOf(grayRule.getId()));
            }

        } catch (Exception e) {
            ContextManager.getRuntimeContext().put(GrayConstant.GRAY_RULE, GrayConstant.EMPTY_GRAY_RULE);
            Map<String, String> eventMap = new HashMap<String, String>();
            eventMap.put("predicate-gray-rule", "predicate gray rule error");
            abstractTracingSpan.log(System.currentTimeMillis(), eventMap);
            abstractTracingSpan.log(e);
        }

    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {
        ContextManager.activeSpan().errorOccurred().log(t);
    }
}
