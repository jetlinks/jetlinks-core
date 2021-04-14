package org.jetlinks.core.server.mqtt;

import org.jetlinks.core.server.ClientConnection;

import java.util.Optional;

/**
 * MQTT 客户端连接
 *
 * @author zhouhao
 * @since 1.1.6
 */
public interface MqttClientConnection extends ClientConnection {

    /**
     * @return 客户端ID
     */
    String clientId();

    /**
     * @return 认证信息
     */
    Optional<MqttAuth> auth();

    /**
     * 拒绝连接
     * <pre>错误吗</pre>
     *
     * @param code 错误码
     */
    MqttClientConnection reject(MqttConnectReturnCode code);

    /**
     * 接受连接
     */
    MqttClientConnection accept();

    /**
     * 设置是否自动应答
     *
     * @param autoAcknowledge 是否自动应答
     * @return this
     */
    MqttClientConnection autoAcknowledge(boolean autoAcknowledge);
}
