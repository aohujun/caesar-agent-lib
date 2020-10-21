package com.hupu.msv.apm.plugin.foundation;

import com.hupu.msv.apm.agent.core.conf.RuntimeContextConfiguration;
import com.hupu.msv.apm.agent.core.context.ContextManager;
import com.hupu.msv.apm.agent.core.context.RuntimeContextSnapshot;

import java.util.concurrent.Callable;

/**
 * @description:
 * @author: Aile
 * @create: 2019/11/06 15:44
 */
public class WrappedCallable<T> implements Callable<T> {

    private final RuntimeContextSnapshot runtimeContextSnapshot;
    private final Callable<T> target;

    WrappedCallable(Callable<T> target) {
        this.runtimeContextSnapshot = ContextManager.getRuntimeContext().capture();
        this.target = target;
    }

    @Override
    public T call() throws Exception {
        try {
            ContextManager.getRuntimeContext().accept(runtimeContextSnapshot);
            return target.call();
        } finally {
            for (String key : RuntimeContextConfiguration.NEED_PROPAGATE_CONTEXT_KEY) {
                ContextManager.getRuntimeContext().remove(key);
            }
        }
    }
}