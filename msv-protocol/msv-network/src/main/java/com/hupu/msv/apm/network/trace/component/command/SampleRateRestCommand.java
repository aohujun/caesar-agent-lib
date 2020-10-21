package com.hupu.msv.apm.network.trace.component.command;

import com.hupu.msv.apm.network.common.Command;
import com.hupu.msv.apm.network.common.KeyStringValuePair;

import java.util.List;

/**
 * @author: zhaoxudong
 * @date: 2019-12-13 22:11
 * @description: 重置采样率
 */
public class SampleRateRestCommand extends BaseCommand implements Serializable, Deserializable<SampleRateRestCommand> {
    public static final Deserializable<SampleRateRestCommand> DESERIALIZER = new SampleRateRestCommand("");
    public static final String NAME = "SampleRateRest";

    public SampleRateRestCommand(String serialNumber) {
        super(NAME,serialNumber);
    }
    @Override
    public SampleRateRestCommand deserialize(Command command) {
        final List<KeyStringValuePair> argsList = command.getArgsList();
        String serialNumber = null;
        for (final KeyStringValuePair pair : argsList) {
            if ("SerialNumber".equals(pair.getKey())) {
                serialNumber = pair.getValue();
                break;
            }
        }
        return new SampleRateRestCommand(serialNumber);
    }

    @Override
    public Command.Builder serialize() {
        return commandBuilder();
    }
}
