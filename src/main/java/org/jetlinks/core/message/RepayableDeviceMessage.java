package org.jetlinks.core.message;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface RepayableDeviceMessage<R extends DeviceMessageReply> extends DeviceMessage {

    R newReply();

}
