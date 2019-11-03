package org.jetlinks.core.message;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonDeviceMessage;
import org.jetlinks.core.message.CommonDeviceMessageReply;

import java.util.Map;

/**
 * 子设备离线消息
 *
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
public class ChildDeviceOfflineMessage extends CommonDeviceMessage {

    private String childDeviceId;

}
