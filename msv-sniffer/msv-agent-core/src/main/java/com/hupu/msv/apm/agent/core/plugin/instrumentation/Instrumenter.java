package com.hupu.msv.apm.agent.core.plugin.instrumentation;

import net.bytebuddy.agent.builder.AgentBuilder;

public interface Instrumenter {

    AgentBuilder instrument(AgentBuilder agentBuilder);
}
