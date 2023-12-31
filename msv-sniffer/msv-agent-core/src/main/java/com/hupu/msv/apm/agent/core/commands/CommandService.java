/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.hupu.msv.apm.agent.core.commands;

import com.hupu.msv.apm.agent.core.boot.BootService;
import com.hupu.msv.apm.agent.core.boot.DefaultImplementor;
import com.hupu.msv.apm.agent.core.boot.ServiceManager;
import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.hupu.msv.apm.network.common.Command;
import com.hupu.msv.apm.network.common.Commands;
import com.hupu.msv.apm.network.trace.component.command.BaseCommand;
import com.hupu.msv.apm.network.trace.component.command.CommandDeserializer;
import com.hupu.msv.apm.network.trace.component.command.SampleRateRestCommand;
import com.hupu.msv.apm.network.trace.component.command.UnsupportedCommandException;
import com.hupu.msv.apm.util.RunnableWithExceptionProtection;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

@DefaultImplementor
public class CommandService implements BootService, Runnable {

    private static final ILog LOGGER = LogManager.getLogger(CommandService.class);

    private volatile boolean isRunning = true;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private LinkedBlockingQueue<BaseCommand> commands = new LinkedBlockingQueue<BaseCommand>(64);
    private CommandSerialNumberCache serialNumberCache = new CommandSerialNumberCache();

    @Override
    public void prepare() throws Throwable {
    }

    @Override
    public void boot() throws Throwable {
        executorService.submit(new RunnableWithExceptionProtection(this, new RunnableWithExceptionProtection.CallbackWhenException() {
            @Override
            public void handle(final Throwable t) {
                LOGGER.error(t, "CommandService failed to execute commands");
            }
        }));
    }

    @Override
    public void run() {
        final CommandExecutorService commandExecutorService = ServiceManager.INSTANCE.findService(CommandExecutorService.class);

        while (isRunning) {
            try {
                BaseCommand command = commands.take();

                if (isCommandExecuted(command)) {
                    continue;
                }

                commandExecutorService.execute(command);
                serialNumberCache.add(command.getSerialNumber());
            } catch (InterruptedException e) {
                LOGGER.error(e, "Failed to take commands.");
            } catch (CommandExecutionException e) {
                LOGGER.error(e, "Failed to execute command[{}].", e.command().getCommand());
            } catch (Throwable e) {
                LOGGER.error(e, "There is unexpected exception");
            }
        }
    }

    private boolean isCommandExecuted(BaseCommand command) {
        if(command.getCommand().equalsIgnoreCase(SampleRateRestCommand.NAME)){
            return false;
        }
        return serialNumberCache.contain(command.getSerialNumber());
    }

    @Override
    public void onComplete() throws Throwable {

    }

    @Override
    public void shutdown() throws Throwable {
        isRunning = false;
        commands.drainTo(new ArrayList<BaseCommand>());
        executorService.shutdown();
    }

    public void receiveCommand(Commands commands) {
        for (Command command : commands.getCommandsList()) {
            try {
                BaseCommand baseCommand = CommandDeserializer.deserialize(command);

                if (isCommandExecuted(baseCommand)) {
                    LOGGER.warn("Command[{}] is executed, ignored", baseCommand.getCommand());
                    continue;
                }

                boolean success = this.commands.offer(baseCommand);

                if (!success && LOGGER.isWarnEnable()) {
                    LOGGER.warn("Command[{}, {}] cannot add to command list. because the command list is full.",
                        baseCommand.getCommand(), baseCommand.getSerialNumber());
                }
            } catch (UnsupportedCommandException e) {
                if (LOGGER.isWarnEnable()) {
                    LOGGER.warn("Received unsupported command[{}].", e.getCommand().getCommand());
                }
            }
        }
    }
}
