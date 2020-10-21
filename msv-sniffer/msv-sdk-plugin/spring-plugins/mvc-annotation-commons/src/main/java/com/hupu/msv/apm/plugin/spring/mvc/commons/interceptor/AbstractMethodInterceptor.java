/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.hupu.msv.apm.plugin.spring.mvc.commons.interceptor;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.util.TimeUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hupu.msv.apm.agent.core.conf.Config;
import com.hupu.msv.apm.agent.core.context.CarrierItem;
import com.hupu.msv.apm.agent.core.context.ContextCarrier;
import com.hupu.msv.apm.agent.core.context.ContextManager;
import com.hupu.msv.apm.agent.core.context.tag.Tags;
import com.hupu.msv.apm.agent.core.context.trace.AbstractSpan;
import com.hupu.msv.apm.agent.core.context.trace.SpanLayer;
import com.hupu.msv.apm.agent.core.governance.GovernanceConfig;
import com.hupu.msv.apm.agent.core.governance.GovernanceEventEnum;
import com.hupu.msv.apm.agent.core.governance.GovernanceEventStatusEnum;
import com.hupu.msv.apm.agent.core.governance.degrade.DegradeRuleFactory;
import com.hupu.msv.apm.agent.core.governance.gray.common.GrayConstant;
import com.hupu.msv.apm.agent.core.governance.gray.common.GrayRouteManager;
import com.hupu.msv.apm.agent.core.governance.gray.config.GrayTag;
import com.hupu.msv.apm.agent.core.governance.gray.strategy.predicate.GrayRule;
import com.hupu.msv.apm.agent.core.governance.traffic.common.TrafficConstants;
import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import com.hupu.msv.apm.agent.core.util.CaesarAgentUtil;
import com.hupu.msv.apm.agent.core.util.MethodUtil;
import com.hupu.msv.apm.network.governance.DegradeConfig;
import com.hupu.msv.apm.network.governance.Result;
import com.hupu.msv.apm.network.trace.component.ComponentsDefine;
import com.hupu.msv.apm.plugin.governance.common.GovernanceReportUtil;
import com.hupu.msv.apm.plugin.gray.common.strategy.predicate.impl.GrayMatchService;
import com.hupu.msv.apm.plugin.spring.mvc.commons.EnhanceRequireObjectCache;
import com.hupu.msv.apm.plugin.spring.mvc.commons.exception.IllegalMethodStackDepthException;
import com.hupu.msv.apm.plugin.spring.mvc.commons.exception.ServletResponseNotFoundException;

import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.*;

import static com.hupu.msv.apm.plugin.spring.mvc.commons.Constants.*;

/**
 * the abstract method interceptor
 */
public abstract class AbstractMethodInterceptor implements InstanceMethodsAroundInterceptor {
    private static final ILog logger = LogManager.getLogger(AbstractMethodInterceptor.class);
    private static boolean IS_SERVLET_GET_STATUS_METHOD_EXIST;
    private static final String SERVLET_RESPONSE_CLASS = "javax.servlet.http.HttpServletResponse";
    private static final String GET_STATUS_METHOD = "getStatus";

    private static final Gson DEFAULT_GSON = new GsonBuilder().create();
    private static final Gson DISABLE_HTML_ESCAPING_GSON = new GsonBuilder().disableHtmlEscaping().create();

    static {
        IS_SERVLET_GET_STATUS_METHOD_EXIST = MethodUtil.isMethodExist(AbstractMethodInterceptor.class.getClassLoader(), SERVLET_RESPONSE_CLASS, GET_STATUS_METHOD);
    }


    public abstract String getRequestURL(Method method);

    public abstract String getAcceptedMethodTypes(Method method);

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {
        Boolean forwardRequestFlag = (Boolean) ContextManager.getRuntimeContext().get(FORWARD_REQUEST_FLAG);
        /**
         * Spring MVC plugin do nothing if current request is forward request.
         * Ref: https://github.com/apache/skywalking/pull/1325
         */
        if (forwardRequestFlag != null && forwardRequestFlag) {
            return;
        }

        String operationName;
        if (Config.Plugin.SpringMVC.USE_QUALIFIED_NAME_AS_ENDPOINT_NAME) {
            operationName = MethodUtil.generateOperationName(method);
        } else {
            EnhanceRequireObjectCache pathMappingCache = (EnhanceRequireObjectCache) objInst.getSkyWalkingDynamicField();
            String requestURL = pathMappingCache.findPathMapping(method);
            if (requestURL == null) {
                requestURL = getRequestURL(method);
                pathMappingCache.addPathMapping(method, requestURL);
                requestURL = getAcceptedMethodTypes(method) + pathMappingCache.findPathMapping(method);
            }
            operationName = requestURL;
        }

//        HttpServletRequest request = (HttpServletRequest) ContextManager.getRuntimeContext().get(REQUEST_KEY_IN_RUNTIME_CONTEXT);
        HttpServletRequest request = (HttpServletRequest) ContextManager.getRuntimeContext().get(GrayConstant.HTTP_REQUEST);
        if (request != null && StringUtils.isEmpty(operationName)) {
            operationName = (String) request.getAttribute("org.springframework.web.servlet.HandlerMapping.bestMatchingPattern");
        }
        if (request != null) {
            StackDepth stackDepth = (StackDepth) ContextManager.getRuntimeContext().get(CONTROLLER_METHOD_STACK_DEPTH);

            if (stackDepth == null) {
                ContextCarrier contextCarrier = new ContextCarrier();
                CarrierItem next = contextCarrier.items();
                while (next.hasNext()) {
                    next = next.next();
                    next.setHeadValue(request.getHeader(next.getHeadKey()));
                }
                AbstractSpan span = ContextManager.createEntrySpan(operationName, contextCarrier);
                Tags.URL.set(span, request.getRequestURL().toString());
                Tags.HTTP.METHOD.set(span, request.getMethod());
                span.setComponent(ComponentsDefine.SPRING_MVC_ANNOTATION);
                SpanLayer.asHttp(span);

                stackDepth = new StackDepth();
                ContextManager.getRuntimeContext().put(CONTROLLER_METHOD_STACK_DEPTH, stackDepth);
            } else {
                AbstractSpan span =
                        ContextManager.createLocalSpan(buildOperationName(objInst, method), CaesarAgentUtil.isSampled(ContextManager.getGlobalTraceId()));
                span.setComponent(ComponentsDefine.SPRING_MVC_ANNOTATION);
            }

            stackDepth.increment();
        }
        //限流逻辑
        if (GovernanceConfig.TRAFFIC_ENABLED && request != null) {
            if (isTraffic(result, operationName, request)) {
                GovernanceReportUtil.recordCount(GovernanceEventEnum.TRAFFIC, operationName);
                GovernanceReportUtil.recordGauge(GovernanceEventEnum.TRAFFIC, operationName, GovernanceEventStatusEnum.OPEN);
                return;
            } else {
                GovernanceReportUtil.recordGauge(GovernanceEventEnum.TRAFFIC, operationName, GovernanceEventStatusEnum.CLOSE);
            }
        }

        // 降级逻辑
        if (GovernanceConfig.DEGRADE_ENABLED && request != null) {
            if (isDegrade(result, operationName)) {
                GovernanceReportUtil.recordCount(GovernanceEventEnum.DEGRADE, operationName);
                GovernanceReportUtil.recordGauge(GovernanceEventEnum.DEGRADE, operationName, GovernanceEventStatusEnum.OPEN);
                return;
            } else {
                GovernanceReportUtil.recordGauge(GovernanceEventEnum.DEGRADE, operationName, GovernanceEventStatusEnum.CLOSE);
            }
        }

        //灰度路由
        if (request != null) {
            setGrayRouteStrategy(request);
        }

//        if (request != null) {
//            String fstress_task = request.getHeader(GrayConstant.FSTRESS_TASK);
//            if (fstress_task != null) {
//                ContextManager.getRuntimeContext().put(GrayConstant.FSTRESS_TASK, fstress_task);
//            }
//        }

    }

    /**
     * 降级逻辑
     *
     * @param result
     * @param operationName
     * @return
     */
    private boolean isDegrade(MethodInterceptResult result, String operationName) {
        DegradeConfig config = DegradeRuleFactory.INSTANCE.getRule(operationName);
        if (config == null) {
            //没有规则，不降级
            return false;
        }
        Result res = config.getResult();
        result.defineReturnValue(null);
        HttpServletResponse response = (HttpServletResponse) ContextManager.getRuntimeContext().get(RESPONSE_KEY_IN_RUNTIME_CONTEXT);
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(res == null || res.getHttpCode() <= 0 ? 200 : res.getHttpCode());
        PrintWriter out = null;
        try {
            out = response.getWriter();
        } catch (IOException e) {
            e.printStackTrace();
        }
        out.print(res == null || res.getResponse() == null ? "{}" : res.getResponse());
        out.flush();
        out.close();
        return true;
    }

    /**
     * 限流逻辑
     *
     * @param result
     * @param operationName
     * @param request
     * @return
     * @throws IOException
     */
    private boolean isTraffic(MethodInterceptResult result, String operationName, HttpServletRequest request) throws IOException {
        Entry entry = null;
        try {
            String ip = request.getHeader("ip");
            String app = request.getHeader("referer");
            String inst = request.getHeader("inst");
            //校验黑白名单规则
            String ipOrigin = StringUtils.isEmpty(ip) ? null : "ip::" + ip;
            String appOrigin = StringUtils.isEmpty(app) ? null : "app::" + app.toLowerCase();
            String instOrigin = StringUtils.isEmpty(inst) ? null : "inst::" + inst.toLowerCase();
            StringBuilder builder = new StringBuilder();
            if (ipOrigin != null) {
                builder.append(ipOrigin);
                builder.append(",");
            }
            if (appOrigin != null) {
                builder.append(appOrigin);
                builder.append(",");
            }
            if (instOrigin != null) {
                builder.append(instOrigin);
                builder.append(",");
            }
            String origin = "";
            if (builder.length() > 0) {
                origin = builder.toString().substring(0, builder.length() - 1);
            }
            ContextUtil.enter(operationName, origin);
            entry = SphU.entry(operationName, EntryType.IN, 1, inst, ip, app);
        } catch (BlockException e) {
            result.defineReturnValue(null);
            HttpServletResponse response = (HttpServletResponse) ContextManager.getRuntimeContext().get(RESPONSE_KEY_IN_RUNTIME_CONTEXT);
            response.setContentType("text/html;charset=utf-8");
            response.setStatus(TrafficConstants.RESULT_MAP.get(operationName) == null ? 429 : TrafficConstants.RESULT_MAP.get(operationName).getHttpCode());
            PrintWriter out = response.getWriter();
            out.print(TrafficConstants.RESULT_MAP.get(operationName) == null ? "{}" : TrafficConstants.RESULT_MAP.get(operationName).getResponse());
            out.flush();
            out.close();
            return true;
        } catch (RuntimeException e4) {
            logger.error(e4.getMessage(), e4);
            Tracer.trace(e4);
            throw e4;
        } finally {
            if (entry != null) {
                entry.exit();
            }
            ContextUtil.exit();
        }
        return false;
    }


    /**
     * 设置请求命中的灰度路由策略
     *
     * @param request
     */
    private void setGrayRouteStrategy(HttpServletRequest request) {
        AbstractSpan abstractTracingSpan = ContextManager.activeSpan();
        if (!GrayRouteManager.INSTANCE.getGrayCommonConfig().getExtensionHttpHeaders().isEmpty()) {
            //检测是否有需要透传的header
            if (isMatchedGrayRule(request, abstractTracingSpan) && GrayRouteManager.INSTANCE.getCover()) {
                ContextManager.getRuntimeContext().remove(GrayConstant.GRAY_RULE);
                return;
            }
            // 透传规则无需传递
            ContextManager.getRuntimeContext().remove(GrayConstant.GRAY_RULE);
        }
        if (!GovernanceConfig.GRAY_ENABLED) {
            return;
        }
        // intercept and get the gray rule
        String grayRuleStr = request.getHeader(GrayConstant.GRAY_RULE);
        if (!StringUtils.isEmpty(grayRuleStr)) {
            try {
                HashMap<String, GrayTag> grayRule = DEFAULT_GSON.fromJson(grayRuleStr, new TypeToken<HashMap<String, GrayTag>>() {
                }.getType());
                ContextManager.getRuntimeContext().put(GrayConstant.GRAY_RULE, grayRule);
                return;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                Map<String, String> eventMap = new HashMap<>();
                eventMap.put("parse-gray-rule", "parse gray rule error, data=" + grayRuleStr);
                abstractTracingSpan.log(TimeUtil.currentTimeMillis(), eventMap);
            }
        } else {
            try {
                if (isMatchedGrayRule(request, abstractTracingSpan))
                    return;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                Map<String, String> eventMap = new HashMap<>();
                eventMap.put("predicate-gray-rule", "predicate gray rule error");
                abstractTracingSpan.log(TimeUtil.currentTimeMillis(), eventMap);
                abstractTracingSpan.log(e);
            }

            ContextManager.getRuntimeContext().put(GrayConstant.GRAY_RULE, GrayConstant.EMPTY_GRAY_RULE);
        }
    }

    private boolean isMatchedGrayRule(HttpServletRequest request, AbstractSpan abstractTracingSpan) {
        GrayRule grayRule = GrayMatchService.INSTANCE.matchedServiceList(request);
        if (grayRule != null) {
            ContextManager.getRuntimeContext().put(GrayConstant.GRAY_RULE, grayRule.getGrayTag());
            Map<String, String> eventMap = new HashMap<>();
            eventMap.put(GrayConstant.GRAY_RULE, "strategy=" + DISABLE_HTML_ESCAPING_GSON.toJson(grayRule));
            abstractTracingSpan.log(TimeUtil.currentTimeMillis(), eventMap);
            GovernanceReportUtil.recordCount(GovernanceEventEnum.GRAY, String.valueOf(grayRule.getId()));
            return true;
        }
        return false;
    }

    private String buildOperationName(Object invoker, Method method) {
        StringBuilder operationName = new StringBuilder(invoker.getClass().getName())
                .append(".").append(method.getName()).append("(");
        for (Class<?> type : method.getParameterTypes()) {
            operationName.append(type.getName()).append(",");
        }

        if (method.getParameterTypes().length > 0) {
            operationName = operationName.deleteCharAt(operationName.length() - 1);
        }

        return operationName.append(")").toString();
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                              Object ret) throws Throwable {
        Boolean forwardRequestFlag = (Boolean) ContextManager.getRuntimeContext().get(FORWARD_REQUEST_FLAG);
        /**
         * Spring MVC plugin do nothing if current request is forward request.
         * Ref: https://github.com/apache/skywalking/pull/1325
         */
        if (forwardRequestFlag != null && forwardRequestFlag) {
            return ret;
        }

        HttpServletRequest request = (HttpServletRequest) ContextManager.getRuntimeContext().get(REQUEST_KEY_IN_RUNTIME_CONTEXT);

        if (request != null) {
            StackDepth stackDepth = (StackDepth) ContextManager.getRuntimeContext().get(CONTROLLER_METHOD_STACK_DEPTH);
            if (stackDepth == null) {
                throw new IllegalMethodStackDepthException();
            } else {
                stackDepth.decrement();
            }

            AbstractSpan span = ContextManager.activeSpan();

            if (stackDepth.depth() == 0) {
                HttpServletResponse response = (HttpServletResponse) ContextManager.getRuntimeContext().get(RESPONSE_KEY_IN_RUNTIME_CONTEXT);
                if (response == null) {
                    throw new ServletResponseNotFoundException();
                }

                if (IS_SERVLET_GET_STATUS_METHOD_EXIST && response.getStatus() >= 400) {
                    span.errorOccurred();
                    Tags.STATUS_CODE.set(span, Integer.toString(response.getStatus()));
                }

                ContextManager.getRuntimeContext().remove(REQUEST_KEY_IN_RUNTIME_CONTEXT);
                ContextManager.getRuntimeContext().remove(RESPONSE_KEY_IN_RUNTIME_CONTEXT);
                ContextManager.getRuntimeContext().remove(CONTROLLER_METHOD_STACK_DEPTH);
//                ContextManager.getRuntimeContext().remove(GrayConstant.FSTRESS_TASK);
                ContextManager.getRuntimeContext().remove(GrayConstant.EXTENSION_HTTP_HEADERS);
                ContextManager.getRuntimeContext().remove(GrayConstant.HTTP_HEADERS_CHECKED);
            }

            ContextManager.stopSpan();
        } else {
            logger.warn("request is null!");
//            ContextManager.getRuntimeContext().remove(GrayConstant.FSTRESS_TASK);
            ContextManager.getRuntimeContext().remove(GrayConstant.EXTENSION_HTTP_HEADERS);
            ContextManager.getRuntimeContext().remove(GrayConstant.HTTP_HEADERS_CHECKED);
        }

        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
        ContextManager.activeSpan().errorOccurred().log(t);
    }
}
