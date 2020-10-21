package com.hupu.msv.apm.agent.instrumentation.executor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author: edison.li
 * @date: 2020/6/17 9:12 下午
 * @description:
 */
public class NioEventLoopUtil {

    public static String getLoopName2NioEventLoop(Object obj) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
//        if (obj instanceof NioEventLoop) {
//            NioEventLoop nioEventLoop = (NioEventLoop)obj;
//            if (nioEventLoop.threadProperties() == null ) {
//                return null;
//            }
//            return nioEventLoop.threadProperties().name();
//        }

        Object object = invokeMethod(obj,"threadProperties");
        if ( object == null) {
            return null;
        }

        Object poolName = invokeMethod(object,"name");
        if ( poolName == null) {
            return null;
        }
        return poolName.toString();
    }

     private static Object invokeMethod(Object obj, String method) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException{
         Method threadPropertiesMethod = obj.getClass().getMethod(method);
         threadPropertiesMethod.setAccessible(true);
         return threadPropertiesMethod.invoke(obj);
     }

}
