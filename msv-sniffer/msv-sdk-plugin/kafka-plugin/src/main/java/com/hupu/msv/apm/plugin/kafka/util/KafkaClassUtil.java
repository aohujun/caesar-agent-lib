package com.hupu.msv.apm.plugin.kafka.util;

import java.lang.reflect.Field;

/**
 * @author: edison.li
 * @date: 2020/7/13 6:41 下午
 * @description:
 */
public class KafkaClassUtil {

    //记录当前用户kafka的服务消息版本 初始值为-1
    public static byte MAX_USABLE_PRODUCE_MAGIC = -1;

    public static Object getObjectField(Object obj, String name) throws NoSuchFieldException, IllegalAccessException {
        Field field = obj.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(obj);
    }
}
