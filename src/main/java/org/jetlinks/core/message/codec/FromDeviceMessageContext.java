package org.jetlinks.core.message.codec;


import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.server.session.DeviceSession;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface FromDeviceMessageContext extends MessageDecodeContext {
    DeviceSession getSession();

    @Override
    default DeviceOperator getDeviceOperator() {
        return getSession().getOperator();
    }

    static FromDeviceMessageContext of(DeviceSession session,EncodedMessage message){
        return new FromDeviceMessageContext() {
            @Override
            public DeviceSession getSession() {
                return session;
            }

            @Override
            public EncodedMessage getMessage() {
                return message;
            }
        };
    }
}
