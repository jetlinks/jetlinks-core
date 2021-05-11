package org.jetlinks.core.server.session;

/**
 * 设备会话提供者
 *
 * @author zhouhao
 * @since 1.1.6
 */
public interface DeviceSessionProvider {

    /**
     * @return 提供者ID
     */
    String getId();

    /**
     * 反序列化会话
     *
     * @param sessionData 会话数据
     * @return 会话
     */
    PersistentSession deserialize(byte[] sessionData);

}
