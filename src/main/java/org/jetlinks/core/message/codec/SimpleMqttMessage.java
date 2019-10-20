package org.jetlinks.core.message.codec;

import io.netty.buffer.ByteBuf;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SimpleMqttMessage implements MqttMessage {

    private String topic;

    private String deviceId;

    private int qosLevel = 0;

    private ByteBuf payload;

    private int messageId = -1;

}
