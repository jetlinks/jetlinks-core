package org.jetlinks.core.message.firmware;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonDeviceMessage;
import org.jetlinks.core.message.MessageType;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import static org.jetlinks.core.utils.SerializeUtils.readNullableUTF;
import static org.jetlinks.core.utils.SerializeUtils.writeNullableUTF;

/**
 * 上报固件更新进度
 *
 * @author zhouhao
 * @since 1.3
 */
@Getter
@Setter
public class UpgradeFirmwareProgressMessage extends CommonDeviceMessage<UpgradeFirmwareProgressMessage> {

    /**
     * 进度0-100
     */
    private int progress;

    /**
     * 是否已完成
     */
    private boolean complete;

    /**
     * 升级中的固件版本
     */
    private String version;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误原因
     */
    private String errorReason;

    /**
     * 固件ID
     *
     * @since 1.1.4
     */
    private String firmwareId;


    public UpgradeFirmwareProgressMessage success(int progress) {
        setSuccess(true);
        setProgress(progress);
        return this;
    }

    public UpgradeFirmwareProgressMessage complete() {
        setComplete(true);
        return success(100);
    }

    public UpgradeFirmwareProgressMessage error(Throwable e) {
        setSuccess(false);
        setErrorReason(e.getMessage());
        return this;
    }

    public UpgradeFirmwareProgressMessage error(String reason) {
        setSuccess(false);

        setErrorReason(reason);
        return this;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.UPGRADE_FIRMWARE_PROGRESS;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeInt(progress);
        out.writeBoolean(complete);
        out.writeBoolean(success);

        writeNullableUTF(version, out);
        writeNullableUTF(errorReason, out);
        writeNullableUTF(firmwareId, out);

    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        this.progress = in.readInt();
        this.complete = in.readBoolean();
        this.success = in.readBoolean();
        this.version = readNullableUTF(in);
        this.errorReason = readNullableUTF(in);
        this.firmwareId = readNullableUTF(in);
    }
}
