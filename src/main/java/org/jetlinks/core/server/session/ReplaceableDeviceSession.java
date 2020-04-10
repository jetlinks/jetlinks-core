package org.jetlinks.core.server.session;

/**
 * 可替换的设备会话
 *
 * @since 1.0.2
 */
public interface ReplaceableDeviceSession {

    void replaceWith(DeviceSession session);

}
