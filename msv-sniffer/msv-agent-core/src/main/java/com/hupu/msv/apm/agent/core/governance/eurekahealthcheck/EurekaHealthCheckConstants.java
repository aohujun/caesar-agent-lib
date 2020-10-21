package com.hupu.msv.apm.agent.core.governance.eurekahealthcheck;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author: zhaoxudong
 * @date: 2020-02-13 19:55
 * @description:
 */
public class EurekaHealthCheckConstants {
    /**
     * 影响服务可用性的健康检测名字。
     * 默认两项，程序运行时可以被替换
     * ":"后面是检测次数，只有达到检测次数才会上报状态
     */
    public static List<String> INFLUENCE_AVAILABILITY_NAMES = new ArrayList<>(Arrays.asList("tomcatThread:3", "diskSpace"));
}
