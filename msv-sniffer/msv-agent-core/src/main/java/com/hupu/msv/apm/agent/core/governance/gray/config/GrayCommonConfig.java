package com.hupu.msv.apm.agent.core.governance.gray.config;

import com.hupu.msv.apm.agent.core.governance.gray.strategy.predicate.GrayRule;

import java.util.*;

/**
 * @description: 灰度路由properties配置
 * @author: Aile
 * @create: 2019/11/04 19:09
 */
public class GrayCommonConfig {

    /**
     * 策略id =>  策略项
     */
    private LinkedHashMap<Long, GrayRuleConfig> grayRuleConfigMap = new LinkedHashMap<>();

    /**
     * key=serviceId
     * values=instanceid list
     */
    private HashMap<String, Set<String>> exclusiveInstanceMap = new HashMap<>();


    /**
     * 需要透传的Headers
     *
     * [{
     *     "key":"testHeader",
     *     "value":"123",
     *     "id":"1"
     *     "color":"red"
     * },{
     *     "key":"header2",
     *     "id":"2"
     *     "color":"blue"
     * }]
     */
    private Collection<HashMap<String, String>> extensionHttpHeaders  = new ArrayList<>();
    public void setGrayRuleConfigMap(LinkedHashMap<Long, GrayRuleConfig> grayRuleConfigMap) {
        this.grayRuleConfigMap = grayRuleConfigMap;
    }

    public LinkedHashMap<Long, GrayRuleConfig> getGrayRuleConfigMap() {
        return grayRuleConfigMap;
    }

    public HashMap<String, Set<String>> getExclusiveInstanceMap() {
        return exclusiveInstanceMap;
    }

    public void setExclusiveInstanceMap(HashMap<String, Set<String>> exclusiveInstanceMap) {
        this.exclusiveInstanceMap = exclusiveInstanceMap;
    }

    public Collection<HashMap<String, String>> getExtensionHttpHeaders() {
        return extensionHttpHeaders;
    }

    public void setExtensionHttpHeaders(Collection<HashMap<String, String>> extensionHttpHeaders) {
        this.extensionHttpHeaders = extensionHttpHeaders;
    }

//    /**
//     * 精准条件
//     * 策略命中条件:path => 规则
//     */
//    public static HashMap<String, GrayRule> strictPathPredicate = new HashMap<>();
//
//    /**
//     * 模糊条件
//     */
//    public static  LinkedHashMap<String, GrayRule> fuzzyPathPredicate = new LinkedHashMap<>();
//    public static  HashMap<String, HashMap<String, GrayRule>> path2param = new HashMap<>();

}
