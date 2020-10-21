package com.hupu.msv.apm.plugin.gray.common.strategy.predicate.impl;

import com.hupu.msv.apm.agent.core.context.ContextManager;
import com.hupu.msv.apm.agent.core.governance.gray.common.GrayConstant;
import com.hupu.msv.apm.agent.core.governance.gray.common.GrayRouteManager;
import com.hupu.msv.apm.agent.core.governance.gray.strategy.predicate.GrayRule;
import com.hupu.msv.apm.agent.core.governance.gray.utils.EmptyUtils;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * @author: zhaoxudong
 * @date: 2020-07-27 14:32
 * @description:
 */
public class GrayMatchStaticService {
    private static UrlPathHelper urlPathHelper = new UrlPathHelper();
    private static PathMatcher pathMatcher = new AntPathMatcher();



    public static GrayRule matchedServiceList(Object args) {

        if (args == null || !(args instanceof HttpServletRequest)) {
            return null;
        }

        HttpServletRequest httpServletRequest = (HttpServletRequest) args;
        if (!GrayRouteManager.INSTANCE.getGrayCommonConfig().getExtensionHttpHeaders().isEmpty()
                && ContextManager.getRuntimeContext().get(GrayConstant.HTTP_HEADERS_CHECKED) == null) {
            ContextManager.getRuntimeContext().put(GrayConstant.HTTP_HEADERS_CHECKED, true);
            return matchedGrayRule(httpServletRequest);
        } else {
            //path是否命中
            String path = urlPathHelper.getPathWithinApplication(httpServletRequest);
            GrayMatchStaticService.MapEntry<String, GrayRule> pathPredicate = matchedServiceListByPath(path);
            if (pathPredicate == null) {
                return null;
            }
            String pathPredicateKey = pathPredicate.getKey();
            GrayRule targetGrayRoute = pathPredicate.getValue();

            //header/parameter是否命中
            HashMap<String, GrayRule> paramPredicateMap = GrayRouteManager.INSTANCE.getGrayRouteStrategy().getPath2param().get(pathPredicateKey);
            String paramsPredicateKey = null;
            if (EmptyUtils.isNotEmpty(paramPredicateMap)) {

                //header
                Enumeration<String> headerEnumeration = httpServletRequest.getHeaderNames();
                while (headerEnumeration.hasMoreElements()) {
                    String key = headerEnumeration.nextElement();
                    if (GrayRouteManager.INSTANCE.getGrayRouteStrategy().getIgnoreHeader().contains(key)) {
                        continue;
                    }
                    String predicateKey = GrayConstant.HEADER + ":" + key + "=" + httpServletRequest.getHeader(key);
                    if (paramPredicateMap.containsKey(predicateKey)) {
                        paramsPredicateKey = predicateKey;
                        break;
                    }
                }

                //queryString、postData
                if (paramsPredicateKey == null) {
                    Enumeration<String> parameterNames = httpServletRequest.getParameterNames();
                    while (parameterNames.hasMoreElements()) {
                        String key = parameterNames.nextElement();
                        String predicateKey = GrayConstant.PARAM + ":" + key + "=" + httpServletRequest.getParameter(key);
                        if (paramPredicateMap.containsKey(predicateKey)) {
                            paramsPredicateKey = predicateKey;
                            break;
                        }
                    }
                }

                if (paramsPredicateKey != null) {
                    targetGrayRoute = paramPredicateMap.get(paramsPredicateKey);
                }
            }

            if (targetGrayRoute != null) {
                targetGrayRoute.setCondition(GrayConstant.PATH + ":" + pathPredicateKey + ";" + paramsPredicateKey);
                return targetGrayRoute;
            }
        }


        return null;
    }

    /**
     * 透传header匹配规则
     *
     * @param httpServletRequest
     * @return
     */
    private static GrayRule matchedGrayRule(HttpServletRequest httpServletRequest) {
        Collection<HashMap<String, String>> extensionHttpHeaders = GrayRouteManager.INSTANCE.getGrayCommonConfig().getExtensionHttpHeaders();
        String key, value, headerValue;
        GrayRule grayRule = null;
        List<HashMap<String, String>> targetHeaders = new ArrayList<>();
        String path = urlPathHelper.getPathWithinApplication(httpServletRequest);
        for (HashMap<String, String> map : extensionHttpHeaders) {
            key = map.get("key");
            value = map.get("value");
            headerValue = httpServletRequest.getHeader(key);
            if (value != null) {
                if (value.equals(headerValue)) {
                    //只创建一条规则即可
                    grayRule = grayRule == null ? createMatchedGrayRule(key, headerValue, map, path) : grayRule;
                    addTargetHeader(key, headerValue, targetHeaders);
                }
            } else {
                if (headerValue != null) {
                    grayRule = grayRule == null ? createMatchedGrayRule(key, headerValue, map, path) : grayRule;
                    addTargetHeader(key, headerValue, targetHeaders);
                }
            }
        }
        if (!targetHeaders.isEmpty()) {
            ContextManager.getRuntimeContext().put(GrayConstant.EXTENSION_HTTP_HEADERS, targetHeaders);
        }
        return grayRule;
    }

    private static void addTargetHeader(String key, String value, List<HashMap<String, String>> targetHeaders) {
        HashMap<String, String> targetMap = new HashMap();
        targetMap.put("key", key);
        targetMap.put("value", value);
        targetHeaders.add(targetMap);
    }

    private static GrayRule createMatchedGrayRule(String key, String headerValue, HashMap<String, String> map, String path) {
        GrayRule grayRule = new GrayRule();
        grayRule.setColor(map.get("color"));
        grayRule.setId(Long.valueOf(map.get("id")));
        grayRule.setCondition(GrayConstant.PATH + ":" + path + ";" + GrayConstant.HEADER + ":" + key + "=" + headerValue);
        return grayRule;
    }

    private static GrayMatchStaticService.MapEntry<String, GrayRule> matchedServiceListByPath(String path) {
        if (GrayRouteManager.INSTANCE.getGrayRouteStrategy().getStrictPathPredicate().containsKey(path)) {
            return new GrayMatchStaticService.MapEntry(path, GrayRouteManager.INSTANCE.getGrayRouteStrategy().getStrictPathPredicate().get(path));
        }

        for (String pattern : GrayRouteManager.INSTANCE.getGrayRouteStrategy().getFuzzyPathPredicate().keySet()) {
            if (GrayMatchStaticService.pathMatcher.match(pattern, path)) {
                return new GrayMatchStaticService.MapEntry(pattern, GrayRouteManager.INSTANCE.getGrayRouteStrategy().getFuzzyPathPredicate().get(pattern));
            }
        }

        return null;
    }

    private static class MapEntry<K, V> implements Map.Entry<K, V> {

        private K key;
        private V value;

        public MapEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return this.key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            return this.value = value;
        }

    }
}
