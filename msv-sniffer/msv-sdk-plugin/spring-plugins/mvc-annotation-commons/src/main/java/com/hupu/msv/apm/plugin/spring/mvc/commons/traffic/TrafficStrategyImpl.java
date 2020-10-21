package com.hupu.msv.apm.plugin.spring.mvc.commons.traffic;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityRule;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowItem;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.hupu.msv.apm.agent.core.governance.traffic.common.TrafficConstants;
import com.hupu.msv.apm.agent.core.governance.traffic.strategy.TrafficStrategy;
import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.hupu.msv.apm.network.governance.TrafficConfig;

import java.util.*;

import static com.alibaba.csp.sentinel.slots.block.RuleConstant.AUTHORITY_BLACK;
import static com.alibaba.csp.sentinel.slots.block.RuleConstant.AUTHORITY_WHITE;
import static com.hupu.msv.apm.agent.core.governance.GovernanceConstants.RULE_DEL_FLAG_DEL;
import static com.hupu.msv.apm.agent.core.governance.GovernanceConstants.RULE_ENABLED_CLOSE;
import static com.hupu.msv.apm.agent.core.governance.traffic.common.TrafficConstants.*;

/**
 * @author: zhaoxudong
 * @date: 2019-12-12 10:42
 * @description:
 */
public class TrafficStrategyImpl implements TrafficStrategy {
    private static final ILog logger = LogManager.getLogger(TrafficStrategyImpl.class);
    @Override
    public void loadRule(Collection<TrafficConfig> trafficRules) {
        if (trafficRules == null || trafficRules.size() == 0) {
            // 清空规则
            AuthorityRuleManager.loadRules(Collections.emptyList());
            FlowRuleManager.loadRules(Collections.emptyList());
            ParamFlowRuleManager.loadRules(Collections.emptyList());
            TrafficConstants.RESULT_MAP.clear();
            return;
        }
        //转换规则
        logger.info("转换规则");
        List<AuthorityRule> authorityRules = new ArrayList<>();
        List<FlowRule> flowRules = new ArrayList<>();
        List<ParamFlowRule> paramFlowRules = new ArrayList<>();
        transformRules(authorityRules, flowRules, paramFlowRules, trafficRules);
        AuthorityRuleManager.loadRules(authorityRules);
        FlowRuleManager.loadRules(flowRules);
        ParamFlowRuleManager.loadRules(paramFlowRules);
        logger.info("规则转换并加载完成");
    }

    /**
     * 将控制台的规则转换为Sentinel可识别的规则
     *
     * @param authorityRules
     * @param flowRules
     * @param paramFlowRules
     * @param trafficRules
     */
    private void transformRules(List<AuthorityRule> authorityRules, List<FlowRule> flowRules, List<ParamFlowRule> paramFlowRules, Collection<TrafficConfig> trafficRules) {
        Map<String, Integer> authorityStrategyMap = new HashMap<>();
        Map<String, List<AuthorityRule>> authorityRuleMap = new HashMap<>();
        for (TrafficConfig trafficRule : trafficRules) {
            if (RULE_ENABLED_CLOSE == trafficRule.getEnabled()
                    || RULE_DEL_FLAG_DEL == trafficRule.getDelFlag()) {
                continue;
            }
            String[] paths = trafficRule.getPath().split(",");
            for (String path : paths) {
                TrafficConfig trafficConfig = TrafficConfig.newBuilder(trafficRule).setPath(path).build();
                setRule(flowRules, paramFlowRules, authorityStrategyMap, authorityRuleMap, trafficConfig);
            }
        }
        if (authorityRuleMap.isEmpty()) {
            return;
        }
        authorityRuleMap.keySet().forEach(key -> {
            AuthorityRule authorityRule = getAuthorityRule(authorityRuleMap.get(key));
            authorityRules.add(authorityRule);
        });
    }

    private void setRule(List<FlowRule> flowRules, List<ParamFlowRule> paramFlowRules, Map<String, Integer> authorityStrategyMap, Map<String, List<AuthorityRule>> authorityRuleMap, TrafficConfig trafficRule) {
        TrafficConstants.RESULT_MAP.put(trafficRule.getPath(), trafficRule.getResult());
        if (TRAFFIC_TYPE_DEFAULT == (trafficRule.getTrafficType())) {

            transformFlowRule(flowRules, paramFlowRules, trafficRule);
        } else if (TRAFFIC_TYPE_BLACK == (trafficRule.getTrafficType())
                || TRAFFIC_TYPE_WHITE == (trafficRule.getTrafficType())) {
            //黑白名单流控
            AuthorityRule authorityRule = new AuthorityRule();
            setAuthLimitApp(trafficRule, authorityRule);
            authorityRule.setResource(trafficRule.getPath());
            authorityRule.setStrategy(TRAFFIC_TYPE_WHITE == (trafficRule.getTrafficType()) ? AUTHORITY_WHITE : AUTHORITY_BLACK);
            Integer strategy = authorityStrategyMap.get(trafficRule.getPath());
            if (strategy == null) {
                authorityStrategyMap.put(trafficRule.getPath(), trafficRule.getTrafficType());
            } else {
                if (!strategy.equals(trafficRule.getTrafficType())) {
                    return;
                }
            }
            List<AuthorityRule> ruleList = authorityRuleMap.get(trafficRule.getPath());
            if (ruleList == null || ruleList.size() == 0) {
                ruleList = new ArrayList<>();
                authorityRuleMap.put(trafficRule.getPath(), ruleList);
            }
            ruleList.add(authorityRule);

            if (TRAFFIC_TYPE_WHITE == (trafficRule.getTrafficType())
                    && trafficRule.getAmount() > -1) {
                //设置普通流控规则
                transformFlowRule(flowRules, paramFlowRules, trafficRule);
            }
        } else if (TRAFFIC_TYPE_PARAM == (trafficRule.getTrafficType())) {
            //热点参数流控
            transformParamFlow(paramFlowRules, trafficRule);
        }
    }

    private void transformFlowRule(List<FlowRule> flowRules, List<ParamFlowRule> paramFlowRules, TrafficConfig trafficRule) {
        if (StringUtil.isNotBlank(trafficRule.getSrc()) && SRC_TYPE_ALL != (trafficRule.getSrcType())) {
            //设置了来源，采用热点参数方式
            transformParamFlow(paramFlowRules, trafficRule);
            return;
        }
        //普通流控
        FlowRule flowRule = getBaseFlowRule(trafficRule);
        flowRule.setLimitApp(RuleConstant.LIMIT_APP_DEFAULT);
        flowRules.add(flowRule);
    }

    private FlowRule getBaseFlowRule(TrafficConfig trafficRule) {
        FlowRule flowRule = new FlowRule();
        flowRule.setCount(Double.valueOf(trafficRule.getAmount()));
        flowRule.setResource(trafficRule.getPath());
        return flowRule;
    }

    private AuthorityRule getAuthorityRule(List<AuthorityRule> authorityRules) {
        AuthorityRule authorityRule = authorityRules.remove(0);
        String limitApp = authorityRule.getLimitApp();
        for (AuthorityRule rule : authorityRules) {
            limitApp += "," + rule.getLimitApp();
        }
        authorityRule.setLimitApp(limitApp);
        return authorityRule;

    }

    private void transformParamFlow(List<ParamFlowRule> paramFlowRules, TrafficConfig trafficRule) {
        String[] srcs = trafficRule.getSrc().split(",");
        for (String src : srcs) {
            ParamFlowRule paramFlowRule = new ParamFlowRule(trafficRule.getPath());
            paramFlowRule.setCount(Integer.MAX_VALUE);
            setParamIndex(paramFlowRule, trafficRule);
            ParamFlowItem item = new ParamFlowItem().setObject(String.valueOf(src).toLowerCase())
                    .setClassType(String.class.getName())
                    .setCount(trafficRule.getAmount());
            paramFlowRule.setParamFlowItemList(Collections.singletonList(item));
            paramFlowRules.add(paramFlowRule);
        }
    }

    private void setParamIndex(ParamFlowRule paramFlowRule, TrafficConfig trafficRule) {
        if (trafficRule.getSrcType() == SRC_TYPE_INSTANCEID) {
            paramFlowRule.setParamIdx(SRC_TYPE_INSTANCEID - 1);
        } else if (trafficRule.getSrcType() == SRC_TYPE_IP) {
            paramFlowRule.setParamIdx(SRC_TYPE_IP - 1);
        } else if (trafficRule.getSrcType() == SRC_TYPE_APP) {
            paramFlowRule.setParamIdx(SRC_TYPE_APP - 1);
        } else {
            paramFlowRule.setParamIdx(0);
        }
    }

    private void setAuthLimitApp(TrafficConfig trafficRule, AuthorityRule authorityRule) {
        StringBuilder limit = new StringBuilder();
        String[] srcs = trafficRule.getSrc().split(",");
        if (SRC_TYPE_INSTANCEID == (trafficRule.getSrcType())) {
            jointLimit(limit, srcs, "inst::");
        } else if (SRC_TYPE_IP == (trafficRule.getSrcType())) {
            jointLimit(limit, srcs, "ip::");
        } else if (SRC_TYPE_APP == (trafficRule.getSrcType())) {
            jointLimit(limit, srcs, "app::");
        }
        authorityRule.setLimitApp(limit.toString().substring(0, limit.length() - 1));
    }

    private void jointLimit(StringBuilder limit, String[] srcs, String s) {
        for (String src : srcs) {
            limit.append(s);
            limit.append(src);
            limit.append(",");
        }
    }
}
