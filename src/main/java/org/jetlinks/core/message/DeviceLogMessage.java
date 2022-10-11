package org.jetlinks.core.message;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.utils.SerializeUtils;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

@Getter
@Setter
public class DeviceLogMessage extends CommonDeviceMessage<DeviceLogMessage> {

    private String log;

    @Override
    public MessageType getMessageType() {
        return MessageType.LOG;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        SerializeUtils.writeObject(log, out);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        this.log = (String) SerializeUtils.readObject(in);
    }
}
