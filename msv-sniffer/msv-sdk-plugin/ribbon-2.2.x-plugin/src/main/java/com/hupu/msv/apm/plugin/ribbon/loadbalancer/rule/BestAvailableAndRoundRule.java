package com.hupu.msv.apm.plugin.ribbon.loadbalancer.rule;


import com.netflix.loadbalancer.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *  最小并发算法增强版：通过轮询+最小并发算法，解决转发低延迟接口时负载不均衡的问题，最终达到高、低延迟接口均能负载均衡
 */
public class BestAvailableAndRoundRule extends ClientConfigEnabledRoundRobinRule {

    private LoadBalancerStats loadBalancerStats;

    private AtomicInteger nextServerCyclicCounter = new AtomicInteger();


    @Override
    public Server choose(Object key) {

        if (loadBalancerStats == null) {
            return super.choose(key);
        }

        List<Server> serverList = getLoadBalancer().getAllServers();
        int minimalConcurrentConnections = Integer.MAX_VALUE;
        long currentTime = System.currentTimeMillis();
        int nextServerIndex = incrementAndGetModulo(serverList.size());

        Server chosen = null;
        for (int i = 0, len = serverList.size(); i < len; i++) {
            Server server = serverList.get((nextServerIndex + i) % len);
            ServerStats serverStats = loadBalancerStats.getSingleServerStat(server);
            if (!serverStats.isCircuitBreakerTripped(currentTime)) {
                int concurrentConnections = serverStats.getActiveRequestsCount(currentTime);
                if (concurrentConnections < minimalConcurrentConnections) {
                    minimalConcurrentConnections = concurrentConnections;
                    chosen = server;
                }
            }
        }
        if (chosen == null) {
            return super.choose(key);
        } else {
            return chosen;
        }
    }

    private int incrementAndGetModulo(int modulo) {
        for (; ; ) {
            int current = nextServerCyclicCounter.get();
            int next = (current + 1) % modulo;
            if (nextServerCyclicCounter.compareAndSet(current, next))
                return next;
        }
    }


    @Override
    public void setLoadBalancer(ILoadBalancer lb) {
        super.setLoadBalancer(lb);
        if (lb instanceof AbstractLoadBalancer) {
            loadBalancerStats = ((AbstractLoadBalancer) lb).getLoadBalancerStats();
        }
    }


}

