package com.hupu.msv.apm.plugin.hystrix.v1;

import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import com.hupu.msv.apm.plugin.hystrix.v1.factory.MatchedRelationRuleFactory;
import feign.Feign;
import feign.FeignException;
import feign.InvocationHandlerFactory;
import feign.Target;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;


/**
 * @author chenbaochao
 * @version 0.0.1
 * @date 2019/11/17 1:54 上午
 */
public class HystrixInvocationHandlerInterceptor implements InstanceConstructorInterceptor{

        @Override
        public void onConstruct(EnhancedInstance objInst, Object[] allArguments) {
            Target target = Target.class.cast(allArguments[0]);
            Map<Method, InvocationHandlerFactory.MethodHandler> dispatch = (Map<Method, InvocationHandlerFactory.MethodHandler>) allArguments[1];
            Set<Method> methods = dispatch.keySet();
            for (Method method : methods) {
                method.setAccessible(true);
                String[] paths= getPath(method);
                String serviceName = target.name();
                String commandKey = Feign.configKey(target.type(), method);
                for (String path : paths) {
                    MatchedRelationRuleFactory.getInstance().addRule(serviceName,path,commandKey);
                }
            }
        }


        private String[] getPath(Method method) {
            if (method.isAnnotationPresent(RequestMapping.class)) {
                RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                return requestMapping.value();
            } else if (method.isAnnotationPresent(GetMapping.class)) {
                GetMapping getMapping = method.getAnnotation(GetMapping.class);
                return getMapping.value();
            } else if (method.isAnnotationPresent(PutMapping.class)) {
                PutMapping putMapping = method.getAnnotation(PutMapping.class);
                return putMapping.value();
            } else if (method.isAnnotationPresent(PostMapping.class)) {
                PostMapping postMapping = method.getAnnotation(PostMapping.class);
                return postMapping.value();
            } else if (method.isAnnotationPresent(DeleteMapping.class)) {
                DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
                return deleteMapping.value();
            } else if (method.isAnnotationPresent(PatchMapping.class)) {
                PatchMapping patchMapping = method.getAnnotation(PatchMapping.class);
                return patchMapping.value();
            }

            throw new RuntimeException("Feignclient 方法缺少spring mvc注解！");
        }

}
