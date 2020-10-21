package com.hupu.msv.apm.plugin.ribbon.loadbalancer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hupu.msv.apm.agent.core.governance.GovernanceConstants;
import com.hupu.msv.apm.agent.core.governance.loadbalancer.common.LoadBalancerConstants;
import com.hupu.msv.apm.agent.core.governance.loadbalancer.strategy.LoadBalancerStrategy;
import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.hupu.msv.apm.network.governance.LoadBalancerConfig;
import com.hupu.msv.apm.plugin.ribbon.loadbalancer.rule.BestAvailableAndRoundRule;
import com.netflix.loadbalancer.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.hupu.msv.apm.agent.core.governance.loadbalancer.common.LoadBalancerConstants.*;

/**
 * @author: zhaoxudong
 * @date: 2020-02-04 13:26
 * @description:
 */
public class LoadBalancerStrategyImpl implements LoadBalancerStrategy {
    private static final ILog log = LogManager.getLogger(LoadBalancerStrategyImpl.class);
    private Map<Long, LoadBalancerConfig> configMap = new LinkedHashMap<>();
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    @Override
    public void loadConfig(List<LoadBalancerConfig> configList) {
        if (configList.isEmpty()) {
            return;
        }
        for (LoadBalancerConfig config : configList) {
            // 删除或者不启用
            if (config.getDelFlag() == GovernanceConstants.RULE_DEL_FLAG_DEL
                    || config.getEnabled() == GovernanceConstants.RULE_ENABLED_CLOSE) {
                //将设置过的策略恢复为默认策略
                LoadBalancerConfig balancerConfig = configMap.get(config.getId());
                if (balancerConfig == null) {
                    continue;
                }
                recoverRule(balancerConfig);
                configMap.remove(config.getId());
                continue;
            }
            LoadBalancerConfig balancerConfig = configMap.get(config.getId());
            if (balancerConfig == null) {
                //新增策略
                configMap.put(config.getId(), config);
            } else {
                // 修改策略。
                // 需要把之前serverName负载均衡策略恢复成默认策略。
                recoverRule(balancerConfig);
                //覆盖原来的值
                configMap.put(config.getId(), config);
            }
            //设置策略
            setRule(config);
        }
        log.info("负载均衡策略更新完毕。serverRuleMap:{}", GSON.toJson(LoadBalancerRuleHandler.SERVER_RULE_MAP));
    }

    /**
     * 恢复负载均衡策略
     *
     * @param balancerConfig
     */
    private void recoverRule(LoadBalancerConfig balancerConfig) {
        long configId = balancerConfig.getId();
        //看以前的规则中是否有设置过相同的服务，如果有就恢复到以前的设置，没有就恢复默认
        Arrays.stream(balancerConfig.getServerName().split(",")).forEach(server -> {
            AtomicBoolean hasHistory = new AtomicBoolean(false);
            configMap.values().forEach(config -> {
                if (Objects.equals(config.getId(), configId)) {
                    return;
                }
                if (Arrays.asList(config.getServerName().split(",")).contains(server)) {
                    hasHistory.set(true);
                    LoadBalancerConfig loadBalancerConfig = LoadBalancerConfig.newBuilder(balancerConfig)
                            .setServerName(server).setRule(config.getRule()).build();
                    setRule(loadBalancerConfig);
                }
            });
            if (!hasHistory.get()) {
                LoadBalancerConfig loadBalancerConfig = LoadBalancerConfig.newBuilder(balancerConfig)
                        .setServerName(server).setRule(LoadBalancerConstants.RULE_DEFAULT).build();
                setRule(loadBalancerConfig);
            }
        });

    }

    /**
     * 设置策略
     *
     * @param balancerConfig
     */
    private void setRule(LoadBalancerConfig balancerConfig) {
        IRule rule = getRule(balancerConfig.getRule());
        Arrays.stream(balancerConfig.getServerName().split(",")).forEach(serverName ->
                LoadBalancerRuleHandler.SERVER_RULE_MAP.put(serverName, rule)
        );
    }

    private IRule getRule(String rule) {
        /**
         *
         * RandomRule: 随机策略
         *
         * RoundRobbinRule:轮询策略，也是Ribbon默认的负载均衡策略
         *
         * RetryRule: 重试策略
         *
         * WeightedResponseTimeRule: 该策略是对RoundRobinRule的扩展，增加了根据实例的响应时间来计算权重，并从权重中选择对应的实例。
         * 响应时间越长，weight越小，被选中的可能性越低。
         *
         * ClientConfigEnabledRoundRobinRule:该策略一般不直接使用，有些高级的策略会继承该类，完成一些高级的策略，
         * ClientConfigEnableRoundRobinRule策略默认使用 RoundRibinRule的线性轮询机制
         *
         * BestAvailableRule:通过遍历负载均衡中维护的所有服务实例，会过滤掉故障实例，并找出并发数请求数最小的实例，
         * 所以该策略的特性就是选出最空闲的实例
         *
         * PredicateBasedRule:该策略主要特性是“先过滤，再轮询”，也就是先过滤掉一些实例，得到过滤后的实例清单，然后轮询该实例清单，
         * PredicateBasedRule中“过滤”功能没有实现，需要继承它的类完成，也就是说不同继承PredicateBasedRule的类有不同的“过滤特性”
         *
         * AvailabilityFilteringRule:继承PredicateBasedRule策略的“先过滤，在轮询”特性，
         * AvailabilityFilteringRule策略的过滤特性是 1：是否故障，即断路器是否生效已断开
         * 2：实例的并发请求数大于阈值，默认2的32次方减一，该阈值可以通过 <clientName>.<nameSpace>.ActiveConnectionsLimit来设置，
         * 只要满足其中一个那么就会过滤掉
         * 过滤掉那些因为一直连接失败的被标记为circuit tripped的后端server，并过滤掉那些高并发的的后端server（active connections 超过配置的阈值）
         *
         * ZoneAvoidanceRule:复合判断server所在区域的性能和server的可用性选择server
         */
        IRule iRule;
        switch (rule) {
            case RULE_RANDOM:
                iRule = new RandomRule();
                break;
            case RULE_RESPONSE_TIME_WEIGHT:
                iRule = new WeightedResponseTimeRule();
                break;
            case RULE_BEST_AVAILABLE:
                iRule = new BestAvailableRule();
                break;
            case RULE_RETRY:
                iRule = new RetryRule();
                break;
            case RULE_AVAILABILITY_FILTERING:
                iRule = new AvailabilityFilteringRule();
                break;
            case RULE_ZONE_AVOIDANCE:
                iRule = new ZoneAvoidanceRule();
                break;
            case RULE_BEST_AVAILABLE_ROUND:
                iRule = new BestAvailableAndRoundRule();
                break;
            // 默认为轮询策略
            case RULE_DEFAULT:
            case RULE_ROUND:
            default:
                iRule = new RoundRobinRule();
                break;
        }
        return iRule;

    }
}
