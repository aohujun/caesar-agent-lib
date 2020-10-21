package com.hupu.msv.apm.plugin.governance.common;

import com.hupu.msv.apm.agent.core.governance.GovernanceEventEnum;
import com.hupu.msv.apm.agent.core.governance.GovernanceEventStatusEnum;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.hupu.msv.apm.agent.core.governance.GovernanceConstants.BIZ_METER_NAME_PREFIX;

/**
 * @author: zhaoxudong
 * @date: 2019-11-01 15:42
 * @description: 服务治理上报工具
 */
public class GovernanceReportUtil {

    private static final Map<String, Counter> COUNTERS = new ConcurrentHashMap<>();
    private static final Map<String, AtomicInteger> GAUGE_MAP = new ConcurrentHashMap<>();

    /**
     * @param event 上报的事件计数{@link GovernanceEventEnum}
     * @param tag
     */
    public static void recordCount(GovernanceEventEnum event, String tag) {
        String eventName = event.name().toLowerCase();
        Counter counter = COUNTERS.get(eventName + tag);
        if (counter == null) {
            counter = registerCounter(eventName, tag);
        }
        counter.increment();
        //  不做缓存，每次都判断counter是否存在，不存在就创建，然后计数，存在就直接计数。效率较低，但是节省内存空间。
        // 上面的做法，使用map做了缓存,效率相对较高，但是会占用内存。
//        Metrics.counter(BIZ_METER_NAME_PREFIX,"event", eventName, "interface", method).increment();
    }

    /**
     * 上报事件状态
     *
     * @param event
     * @param tag
     * @param eventStatus
     */
    public static void recordGauge(GovernanceEventEnum event, String tag, GovernanceEventStatusEnum eventStatus) {
        String eventName = event.name().toLowerCase();
        String key = eventName + tag;
        AtomicInteger gauge = GAUGE_MAP.get(key);
        if (gauge == null) {
            List<Tag> list = new ArrayList<>();
            list.add(new ImmutableTag("event", event.name().toLowerCase()));
            list.add(new ImmutableTag("tag",tag));
            gauge = Metrics.gauge(BIZ_METER_NAME_PREFIX + "_event_status", list, new AtomicInteger(eventStatus.getCode()));
            GAUGE_MAP.put(key,gauge);
        }
        gauge.set(eventStatus.getCode());

    }

    private static int getStatus(GovernanceEventStatusEnum h) {
        return h.getCode();
    }

    private static Counter registerCounter(String eventName, String tag) {

        Counter counter = Counter.builder(BIZ_METER_NAME_PREFIX)
                .tags("event", eventName, "tag", tag)
                .description(BIZ_METER_NAME_PREFIX)
                .register(Metrics.globalRegistry);
        COUNTERS.put(eventName + tag, counter);
        return counter;
    }


}
