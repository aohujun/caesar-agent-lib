package com.hupu.msv.apm.agent.instrumentation;


import com.hupu.msv.apm.agent.bootstrap.ContextStrategy;
import com.hupu.msv.apm.agent.core.context.ContextManager;
import com.hupu.msv.apm.agent.core.context.ContextSnapshot;
import com.hupu.msv.apm.agent.core.context.RuntimeContextSnapshot;
import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.hupu.msv.apm.agent.instrumentation.executor.RunnableWrapper;

import java.lang.reflect.Method;

public class ContextStrategyImpl implements ContextStrategy {

    private static final ILog logger = LogManager.getLogger(ContextStrategyImpl.class);
    @Override
    public Runnable wrapInCurrentContext(Runnable runnable, Object obj, Method originMethod) {
        if (ContextManager.isActive()) {
            ContextSnapshot capture = null;
            RuntimeContextSnapshot runtimeContextSnapshot = ContextManager.getRuntimeContext().capture();
            try{
                capture = ContextManager.capture();
            }catch (Exception e){
                logger.error(e,"wrapInCurrentContext-ContextManager.capture() error GlobalTraceId:{}",ContextManager.getGlobalTraceId());
                // IllegalStateException异常 如果当前activeSpan不存在，会抛出异常阻碍用户系统逻辑运行
                capture=null;
            }
            return new RunnableWrapper(
                    runnable,
                    capture,
                    runtimeContextSnapshot,
                    obj,
                    originMethod);
        }
        return runnable;
    }
}