package com.hupu.msv.apm.agent.instrumentation;

import com.hupu.msv.apm.agent.bootstrap.ContextTrampoline;
import com.hupu.msv.apm.agent.core.plugin.instrumentation.Instrumenter;
import net.bytebuddy.agent.builder.AgentBuilder;

public final class ContextTrampolineInitializer implements Instrumenter {

  @Override
  public AgentBuilder instrument(AgentBuilder agentBuilder) {
    ContextTrampoline.setContextStrategy(new ContextStrategyImpl());
    return agentBuilder;
  }

}