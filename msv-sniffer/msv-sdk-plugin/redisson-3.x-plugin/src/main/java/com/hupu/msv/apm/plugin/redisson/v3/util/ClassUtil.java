package com.hupu.msv.apm.plugin.redisson.v3.util;
import java.lang.reflect.Field;

/**
 * @author: edison.li
 * @date: 2020/6/11 2:56 下午
 * @description:
 */
public class ClassUtil {
    
    /**
     * This method should only be used in low frequency. It should not use in trace context, but just in the metadata
     * preparation stage.
     */
    public static Object getObjectField(Object obj, String name) throws NoSuchFieldException, IllegalAccessException {
        Field field = obj.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(obj);
    }
}
