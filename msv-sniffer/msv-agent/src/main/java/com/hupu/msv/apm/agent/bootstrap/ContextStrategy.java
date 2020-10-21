package com.hupu.msv.apm.agent.bootstrap;

import java.lang.reflect.Method;


public interface ContextStrategy {

  Runnable wrapInCurrentContext(Runnable runnable, Object obj, Method originMethod);

}