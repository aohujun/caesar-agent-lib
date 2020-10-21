package com.hupu.msv.apm.agent.instrumentation.executor;

import com.hupu.msv.apm.agent.bootstrap.ContextTrampoline;
import com.hupu.msv.apm.agent.core.plugin.instrumentation.Instrumenter;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class ExecutorInstrumentation implements Instrumenter {

    /**
     *  如果要减少性能损失，可以减少匹配的范围
     * @param agentBuilder
     * @return
     */
    @Override
    public AgentBuilder instrument(AgentBuilder agentBuilder) {
        return agentBuilder.type(
                ElementMatchers.isSubTypeOf(Executor.class)
                        .and(not(isAbstract()))
        ).transform(new Transformer());
    }

    private static class Transformer implements AgentBuilder.Transformer {

        @Override
        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                                TypeDescription typeDescription,
                                                ClassLoader classLoader,
                                                JavaModule module) {

            return builder.method(
                    ElementMatchers.named("execute")
                            .and(takesArguments(1)
                                    .and(takesArgument(0,Runnable.class))))
                    .intercept(Advice.to(Execute.class));
        }
    }

    private static class Execute {

        @Advice.OnMethodEnter
        private static void intercept(@Advice.Argument(value = 0, readOnly = false) Runnable runnable,
                                      @Advice.This Object obj,
                                      @Advice.Origin Method method) throws Exception {
            runnable = ContextTrampoline.wrapInCurrentContext(runnable,obj,method);
        }
    }
}
