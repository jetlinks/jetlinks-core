package org.jetlinks.core.server.session;

/**
 * 支持持久化的Session
 *
 * @author zhouhao
 * @since 1.1.6
 */
public interface PersistentSession extends DeviceSession {

    /**
     * @return 会话提供者
     */
    String getProvider();


}
