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

import static org.jetlinks.core.utils.SerializeUtils.*;

/**
 * 拉取固件信息响应
 *
 * @author zhouhao
 * @since 1.0.3
 */
@Getter
@Setter
public class RequestFirmwareMessageReply extends CommonDeviceMessageReply<RequestFirmwareMessageReply> {

    /**
     * 固件下载地址
     */
    private String url;

    /**
     * 固件版本
     */
    private String version;

    /**
     * 其他参数
     */
    private Map<String, Object> parameters;

    /**
     * 签名
     */
    private String sign;

    /**
     * 签名方式,md5,sha256
     */
    private String signMethod;

    /**
     * 固件ID
     *
     * @since 1.1.4
     */
    private String firmwareId;

    /**
     * 固件大小
     *
     * @since 1.1.5
     */
    private long size;

    @Override
    public MessageType getMessageType() {
        return MessageType.REQUEST_FIRMWARE_REPLY;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        writeNullableUTF(url, out);
        writeNullableUTF(version, out);
        writeNullableUTF(sign, out);
        writeNullableUTF(signMethod, out);
        writeNullableUTF(firmwareId, out);
        out.writeLong(size);
        writeKeyValue(parameters, out);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        this.url = readNullableUTF(in);
        this.version = readNullableUTF(in);
        this.sign = readNullableUTF(in);
        this.signMethod = readNullableUTF(in);
        this.firmwareId = readNullableUTF(in);
        this.size = in.readLong();
        this.parameters = SerializeUtils.readMap(in, Maps::newHashMapWithExpectedSize);
    }
}
