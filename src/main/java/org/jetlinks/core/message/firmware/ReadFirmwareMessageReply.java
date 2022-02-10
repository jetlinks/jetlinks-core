package org.jetlinks.core.message.firmware;

import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonDeviceMessageReply;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.utils.SerializeUtils;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;

/**
 * 读取固件信息回复.
 *
 * @author zhouhao
 * @see ReadFirmwareMessage
 * @since 1.0.3
 */
@Getter
@Setter
public class ReadFirmwareMessageReply extends CommonDeviceMessageReply<ReadFirmwareMessageReply> {

    //固件版本号
    private String version;

    //其他信息
    private Map<String, Object> properties;

    @Override
    public MessageType getMessageType() {
        return MessageType.READ_FIRMWARE_REPLY;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        this.version = SerializeUtils.readNullableUTF(in);
        if (this.properties == null) {
            this.properties = Maps.newHashMapWithExpectedSize(32);
        }
        SerializeUtils.readKeyValue(in, this.properties::put);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        SerializeUtils.writeNullableUTF(version, out);
        SerializeUtils.writeKeyValue(properties, out);
    }
}
