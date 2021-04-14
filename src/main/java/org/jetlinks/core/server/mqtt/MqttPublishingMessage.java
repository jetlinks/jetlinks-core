package org.jetlinks.core.server.mqtt;

import org.jetlinks.core.message.codec.MqttMessage;

/**
 * MQTT 推送消息
 *
 * @author zhouhao
 * @since 1.1.6
 */
public interface MqttPublishingMessage extends MqttMessage {

    /**
     * 在QoS1,和QoS2时,此方法可能会被调用
     */
    void acknowledge();

    /**
     * 根据另外一个MqttMessage创建MqttPublishingMessage
     *
     * @param message         原始消息
     * @param doOnAcknowledge 应答回调
     * @return void
     */
    static MqttPublishingMessage of(MqttMessage message, Runnable doOnAcknowledge) {
        return ProxyMqttPublishingMessage.of(message, doOnAcknowledge);
    }
}
