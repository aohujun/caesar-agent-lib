package com.hupu.msv.apm.plugin.health.indicator.v1.common;

import com.hupu.msv.apm.plugin.health.indicator.v1.ext.TomcatThreadHealthIndicator;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author: zhaoxudong
 * @date: 2020-03-03 15:01
 * @description:
 */
public class HealthIndicatorHandler {
    private static Map<String, HealthIndicator> indicatorMap = new HashMap<>();

    //初始化indicator，后续可以放入更多的indicator
    static {
        indicatorMap.put("TomcatThreadHealthIndicator",new TomcatThreadHealthIndicator());
    }
    public static Map<String, HealthIndicator> getIndicatorMap(){
        return indicatorMap;
    }

    public static String getKey(String name) {
        int index = name.toLowerCase(Locale.ENGLISH).indexOf("healthindicator");
        if (index > 0) {
            return name.substring(0, index);
        }
        return name;
    }
}
