package org.jetlinks.core.support;

import org.jetlinks.core.message.codec.DefaultDeviceMessageCodec;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public class JetLinksDeviceMessageCodec extends DefaultDeviceMessageCodec {
    public JetLinksDeviceMessageCodec() {
        register(new JetLinkMQTTDeviceMessageCodec());
    }
}
