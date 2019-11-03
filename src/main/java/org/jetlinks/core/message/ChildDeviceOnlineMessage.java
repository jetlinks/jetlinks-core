package org.jetlinks.core.message;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonDeviceMessage;
import org.jetlinks.core.message.CommonDeviceMessageReply;

import java.util.Map;

/**
 * 子设备入网消息
 *
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
public class ChildDeviceOnlineMessage extends CommonDeviceMessage {

    private String childDeviceId;

}
