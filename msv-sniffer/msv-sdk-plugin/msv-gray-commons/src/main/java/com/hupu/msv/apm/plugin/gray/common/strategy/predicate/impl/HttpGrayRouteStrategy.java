package com.hupu.msv.apm.plugin.gray.common.strategy.predicate.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hupu.msv.apm.agent.core.context.ContextManager;
import com.hupu.msv.apm.agent.core.governance.gray.common.GrayConstant;
import com.hupu.msv.apm.agent.core.governance.gray.common.GrayRouteManager;
import com.hupu.msv.apm.agent.core.governance.gray.config.GrayRuleConfig;
import com.hupu.msv.apm.agent.core.governance.gray.config.GrayTag;
import com.hupu.msv.apm.agent.core.governance.gray.strategy.predicate.GrayRouteStrategy;
import com.hupu.msv.apm.agent.core.governance.gray.strategy.predicate.GrayRule;
import com.hupu.msv.apm.agent.core.governance.gray.utils.EmptyUtils;
import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import java.nio.file.Path;
import java.util.*;


/**
 * @description: HTTP请求染色，根据请求path/queryString/header/postData，计算出该请求的灰度路由规则
 * @author: Aile
 * @create: 2019/11/08 21:07
 */
public class HttpGrayRouteStrategy implements GrayRouteStrategy {


    private static final ILog logger = LogManager.getLogger(HttpGrayRouteStrategy.class);

    private Gson gson = new GsonBuilder().serializeNulls().disableHtmlEscaping().create();

    /**
     * 精准条件
     * 策略命中条件:path => 规则
     */
    public HashMap<String, GrayRule> strictPathPredicate = new HashMap<>();

    /**
     * 模糊条件
     */
    private LinkedHashMap<String, GrayRule> fuzzyPathPredicate = new LinkedHashMap<>();
    private HashMap<String, HashMap<String, GrayRule>> path2param = new HashMap<>();

    private UrlPathHelper urlPathHelper = new UrlPathHelper();
    private PathMatcher pathMatcher = new AntPathMatcher();

    private HashSet<String> ignoreHeader = new HashSet<>(Arrays.asList("accept", "accept-encoding", "accept-language", "cache-control", "connection", "host",
            "sec-fetch-mode", "sec-fetch-site", "sec-fetch-user", "upgrade-insecure-requests"));


    /**
     * 根据灰度路由配置，初始化灰度路由表。
     *
     * @param strategyMap
     */
    @Override
    public void initRouteTable(LinkedHashMap<Long, GrayRuleConfig> strategyMap) {

        // init
        HashMap<String, GrayRule> strictPredicateTemp = new HashMap<>();
        LinkedHashMap<String, GrayRule> fuzzyPredicateTemp = new LinkedHashMap<>();
        HashMap<String, HashMap<String, GrayRule>> path2parameterTemp = new HashMap<>();
        if (strategyMap == null || strategyMap.isEmpty()) {
            logger.warn("灰度路由配置初始化：route.strategy 为空");
            this.strictPathPredicate = strictPredicateTemp;
            this.fuzzyPathPredicate = fuzzyPredicateTemp;
            this.path2param = path2parameterTemp;
            return;
        }


        //refresh config
        for (Map.Entry<Long, GrayRuleConfig> entry : strategyMap.entrySet()) {
            GrayRuleConfig grayRuleConfig = entry.getValue();

            if (grayRuleConfig == null) {
                logger.warn("灰度路由配置初始化：策略为空");
                continue;
            }

            if (grayRuleConfig.isDelete() || !grayRuleConfig.isEnable()) {
                logger.info("灰度路由配置已删除：strategy.{} is empty", grayRuleConfig.getId());
                continue;
            }

            //处理predicate
            GrayRuleConfig.Predicate predicateMap = grayRuleConfig.getPredicate();
            if (predicateMap == null) continue;

            //处理predicate 条件和 service-list的映射
            GrayRule grayRoute = HttpGrayRouteStrategy.this.getGrayRoute(grayRuleConfig);
            if (grayRoute == null) {
                logger.warn("gray route is null,strategy={}", new GsonBuilder().disableHtmlEscaping().create().toJson(grayRuleConfig));
                continue;
            }

            HashMap<String, GrayRule> predicateKeyMap = new HashMap<>();
            buildPredicateKeySet(GrayConstant.PARAM, predicateMap.getQueryString(), predicateKeyMap, grayRoute);
            buildPredicateKeySet(GrayConstant.PARAM, predicateMap.getPostData(), predicateKeyMap, grayRoute);
            buildPredicateKeySet(GrayConstant.HEADER, predicateMap.getHeader(), predicateKeyMap, grayRoute);

            if (EmptyUtils.isEmpty(predicateKeyMap) && EmptyUtils.isEmpty(predicateMap.getPaths())) {
                continue;
            }

            List<String> paths = null;
            if (EmptyUtils.isEmpty(predicateMap.getPaths())) {
                paths = Arrays.asList("/**");
            } else {
                paths = predicateMap.getPaths();
            }


            //路径匹配条件
            paths.forEach((urlPredicateKey) -> {

                if (urlPredicateKey.contains("*") || urlPredicateKey.contains("?")) {
                    if (EmptyUtils.isEmpty(predicateKeyMap)) {
                        fuzzyPredicateTemp.put(urlPredicateKey, grayRoute);
                    } else if (!fuzzyPredicateTemp.containsKey(urlPredicateKey)) {
                        fuzzyPredicateTemp.put(urlPredicateKey, null);
                    }
                } else {
                    if (EmptyUtils.isEmpty(predicateKeyMap)) {
                        strictPredicateTemp.put(urlPredicateKey, grayRoute);
                    } else if (!strictPredicateTemp.containsKey(urlPredicateKey)) {
                        strictPredicateTemp.put(urlPredicateKey, null);
                    }
                }

                if (EmptyUtils.isNotEmpty((predicateKeyMap))) {
                    if (path2parameterTemp.get(urlPredicateKey) != null) {
                        path2parameterTemp.get(urlPredicateKey).putAll(predicateKeyMap);
                    } else {
                        path2parameterTemp.put(urlPredicateKey, predicateKeyMap);
                    }
                }

            });
        }
        this.strictPathPredicate = strictPredicateTemp;
        this.fuzzyPathPredicate = fuzzyPredicateTemp;
        this.path2param = path2parameterTemp;

        logger.info("refresh strictPredicate ={} fluzzyPredicate={} path2parameter={}",
                gson.toJson(strictPredicateTemp),
                gson.toJson(fuzzyPredicateTemp),
                gson.toJson(path2parameterTemp)
        );
    }


    private void buildPredicateKeySet(String type, HashMap<String, List<String>> params, HashMap<String, GrayRule> predicateKeyMap, GrayRule grayRule) {

        if (EmptyUtils.isEmpty(params)) {
            return;
        }

        params.entrySet().forEach((e) -> {
            String headerKey = e.getKey();
            if (StringUtils.isEmpty(headerKey) || EmptyUtils.isEmpty(e.getValue())) {
                return;
            }
            for (String value : e.getValue()) {
                String predicateKey = type + ":" + headerKey + "=" + value;
                predicateKeyMap.put(predicateKey, grayRule);
            }
        });

    }


    private GrayRule getGrayRoute(GrayRuleConfig grayRuleConfig) {

        if (EmptyUtils.isEmpty(grayRuleConfig.getRules())) {
            return null;
        }

        GrayRule grayRule = new GrayRule();
        grayRule.setColor(grayRuleConfig.getColor());
        grayRule.setId(grayRuleConfig.getId());
        for (GrayRuleConfig.Rule rule : grayRuleConfig.getRules()) {
            if (EmptyUtils.isEmpty(rule.getValues())) {
                continue;
            }

            //计算权重
            List<Integer> weightSoFarList = null;
            int weightSoFar = 0;
            if (EmptyUtils.isEmpty(rule.getWeightSoFars())) {
                weightSoFarList = new ArrayList<>();
                if (EmptyUtils.isNotEmpty(rule.getWeights())) {
                    for (int weight : rule.getWeights()) {
                        weightSoFar = weightSoFar + weight;
                    }
                    weightSoFarList.add(weightSoFar);
                }
                rule.setWeightSoFars(weightSoFarList);
            } else {
                weightSoFarList = rule.getWeightSoFars();
            }

            if (EmptyUtils.isNotEmpty(rule.getWeightSoFars())) {
                if (rule.getValues().size() != rule.getWeightSoFars().size()) {
                    logger.warn("rule weight conig is inavalid : {}", gson.toJson(rule));
                    continue;
                }
            }


            GrayTag grayTag = new GrayTag(rule.getKey(), rule.getValues(), weightSoFarList);
            grayRule.getGrayTag().put(rule.getServiceId().toLowerCase(), grayTag);
        }
        return grayRule;
    }


    /**
     * 刷新灰度路由表，用于动态新增、修改、删除路由配置，不需要重新初始化路由表。
     *
     * @param strategyMap
     */
    @Override
    public void refreshRouteTable(LinkedHashMap<String, GrayRuleConfig> strategyMap) {

    }


//    @Override
    public GrayRule matchedServiceList(Object args) {
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
            String path = this.urlPathHelper.getPathWithinApplication(httpServletRequest);
            MapEntry<String, GrayRule> pathPredicate = matchedServiceListByPath(path);
            if (pathPredicate == null) {
                return null;
            }
            String pathPredicateKey = pathPredicate.getKey();
            GrayRule targetGrayRoute = pathPredicate.getValue();

            //header/parameter是否命中
            HashMap<String, GrayRule> paramPredicateMap = this.path2param.get(pathPredicateKey);
            String paramsPredicateKey = null;
            if (EmptyUtils.isNotEmpty(paramPredicateMap)) {

                //header
                Enumeration<String> headerEnumeration = httpServletRequest.getHeaderNames();
                while (headerEnumeration.hasMoreElements()) {
                    String key = headerEnumeration.nextElement();
                    if (this.ignoreHeader.contains(key)) {
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
    private GrayRule matchedGrayRule(HttpServletRequest httpServletRequest) {
        Collection<HashMap<String, String>> extensionHttpHeaders = GrayRouteManager.INSTANCE.getGrayCommonConfig().getExtensionHttpHeaders();
        String key, value, headerValue;
        GrayRule grayRule = null;
        List<HashMap<String, String>> targetHeaders = new ArrayList<>();
        String path = this.urlPathHelper.getPathWithinApplication(httpServletRequest);
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
        if(!targetHeaders.isEmpty()){
            ContextManager.getRuntimeContext().put(GrayConstant.EXTENSION_HTTP_HEADERS,targetHeaders);
        }
        return grayRule;
    }

    private void addTargetHeader(String key, String value, List<HashMap<String, String>> targetHeaders) {
        HashMap<String, String> targetMap = new HashMap();
        targetMap.put("key", key);
        targetMap.put("value", value);
        targetHeaders.add(targetMap);
    }

    private GrayRule createMatchedGrayRule(String key, String headerValue, HashMap<String, String> map, String path) {
        GrayRule grayRule = new GrayRule();
        grayRule.setColor(map.get("color"));
        grayRule.setId(Long.valueOf(map.get("id")));
        grayRule.setCondition(GrayConstant.PATH + ":" + path + ";" + GrayConstant.HEADER + ":" + key + "=" + headerValue);
        return grayRule;
    }

    private MapEntry<String, GrayRule> matchedServiceListByPath(String path) {
        if (this.strictPathPredicate.containsKey(path)) {
            return new MapEntry(path, this.strictPathPredicate.get(path));
        }

        for (String pattern : this.fuzzyPathPredicate.keySet()) {
            if (this.pathMatcher.match(pattern, path)) {
                return new MapEntry(pattern, this.fuzzyPathPredicate.get(pattern));
            }
        }

        return null;
    }

    private class MapEntry<K, V> implements Map.Entry<K, V> {

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

    @Override
    public LinkedHashMap<String, GrayRule> getFuzzyPathPredicate() {
        return fuzzyPathPredicate;
    }

    @Override
    public HashMap<String, HashMap<String, GrayRule>> getPath2param() {
        return path2param;
    }

    @Override
    public HashMap<String, GrayRule> getStrictPathPredicate() {
        return strictPathPredicate;
    }

    @Override
    public HashSet<String> getIgnoreHeader() {
        return ignoreHeader;
    }
}


