package com.hupu.msv.apm.plugin.gray.common.strategy.route;

import com.netflix.loadbalancer.Server;

import java.util.List;

/**
 * @description: 路由实例列表选择器
 * @author: Aile
 * @create: 2019/11/06 15:44
 */
public interface RouteServerChooser {

     List<Server> choose(List<Server> servers);
}
