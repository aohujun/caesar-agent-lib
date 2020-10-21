package com.hupu.msv.apm.agent.core.commands.executor;

import com.hupu.msv.apm.agent.core.commands.CommandExecutionException;
import com.hupu.msv.apm.agent.core.commands.CommandExecutor;
import com.hupu.msv.apm.agent.core.conf.Config;
import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.hupu.msv.apm.network.trace.component.command.BaseCommand;

/**
 * @author: zhaoxudong
 * @date: 2019-12-13 22:06
 * @description:
 */
public class SampleRateRestCommandExecutor implements CommandExecutor {
    private static final ILog LOGGER = LogManager.getLogger(SampleRateRestCommandExecutor.class);

    @Override
    public void execute(final BaseCommand command) throws CommandExecutionException {
        String rate = command.getSerialNumber();
        int sampleRate = Integer.parseInt(rate);
        if (Config.Agent.SAMPLE_RATE == sampleRate) {
            return;
        }
        Config.Agent.SAMPLE_RATE = sampleRate;
        LOGGER.info("接收到采样率：{},并更新成功", rate);
    }
}
