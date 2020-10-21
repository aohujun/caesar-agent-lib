package com.hupu.msv.apm.agent.core.governance.gray.utils;

import java.util.Collection;
import java.util.Map;

/**
 * @description: 判断空工具类
 * @author: Aile
 * @create: 2019/09/09 16:06
 */
public class EmptyUtils {

    public static boolean isEmpty(Collection collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isEmpty(Map map) {
        return map == null || map.isEmpty();
    }


    public static boolean isNotEmpty(Map map) {
        return map != null && !map.isEmpty();
    }

    public static boolean isNotEmpty(Collection collection) {
        return collection != null && !collection.isEmpty();
    }

    public static boolean isEmpty(String data) {
        return data == null || "".equals(data);
    }

    public static boolean isNotEmpty(String data) {
        return data != null && !"".equals(data);
    }

}
