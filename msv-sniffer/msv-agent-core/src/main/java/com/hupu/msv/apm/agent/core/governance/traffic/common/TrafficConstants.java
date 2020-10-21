package com.hupu.msv.apm.agent.core.governance.traffic.common;

import com.hupu.msv.apm.network.governance.Result;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: zhaoxudong
 * @date: 2019-10-30 17:17
 * @description: 限流常量
 */
public class TrafficConstants {
    /**
     * 流控类型：0：普通类型
     */
    public static final int TRAFFIC_TYPE_DEFAULT = 0;

    /**
     * 流控类型：1：白名单
     */
    public static final int TRAFFIC_TYPE_WHITE = 1;

    /**
     * 流控类型：2：黑名单
     */
    public static final int TRAFFIC_TYPE_BLACK = 2;

    /**
     * 流控类型：3：热点参数限流
     */
    public static final int TRAFFIC_TYPE_PARAM = 3;
    /**
     * 流量来源类型：所有类型
     */
    public static final int SRC_TYPE_ALL = 0;
    /**
     * 流量来源类型：instanceID
     */
    public static final int SRC_TYPE_INSTANCEID = 1;

    /**
     * 流量来源类型：IP
     */
    public static final int SRC_TYPE_IP = 2;

    /**
     * 流量来源类型：APP
     */
    public static final int SRC_TYPE_APP = 3;

    /**
     * 流量目标类型：APP
     */
    public static final int DST_TYPE_APP = 0;

    /**
     * 流量目标类型：instanceID
     */
    public static final int DST_TYPE_INSTANCEID = 1;

    /**
     * 限流之后返回结果
     */
    public static volatile Map<String, Result> RESULT_MAP = new ConcurrentHashMap<>();


}
