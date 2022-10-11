package org.jetlinks.core.message.firmware;

import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonDeviceMessage;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.utils.SerializeUtils;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;

/**
 * 设备上报固件信息
 *
 * @author zhouhao
 * @since 1.0.3
 */
@Getter
@Setter
public class ReportFirmwareMessage extends CommonDeviceMessage<ReportFirmwareMessage> {

    //版本号
    private String version;

    //其他属性
    private Map<String, Object> properties;

    @Override
    public MessageType getMessageType() {
        return MessageType.REPORT_FIRMWARE;
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
