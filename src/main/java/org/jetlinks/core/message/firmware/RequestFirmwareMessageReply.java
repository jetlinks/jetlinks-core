package org.jetlinks.core.message.firmware;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonDeviceMessageReply;
import org.jetlinks.core.message.MessageType;

import java.util.Map;

/**
 * 拉取固件信息响应
 *
 * @since 1.0.3
 * @author zhouhao
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

    @Override
    public MessageType getMessageType() {
        return MessageType.REQUEST_FIRMWARE_REPLY;
    }
}
