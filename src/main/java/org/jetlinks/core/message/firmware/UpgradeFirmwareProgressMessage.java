package org.jetlinks.core.message.firmware;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonDeviceMessage;
import org.jetlinks.core.message.MessageType;

/**
 * 上报固件更新进度
 *
 * @author zhouhao
 * @since 1.3
 */
@Getter
@Setter
public class UpgradeFirmwareProgressMessage extends CommonDeviceMessage {

    //进度0-100
    private int progress;

    //是否已完成
    private boolean complete;

    //升级中的固件版本
    private String version;

    //是否成功
    private boolean success;

    //错误原因
    private String errorReason;

    @Override
    public MessageType getMessageType() {
        return MessageType.UPGRADE_FIRMWARE_PROGRESS;
    }
}
