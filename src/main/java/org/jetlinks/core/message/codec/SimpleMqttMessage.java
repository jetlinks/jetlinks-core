package org.jetlinks.core.message.codec;

import io.netty.buffer.ByteBuf;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleMqttMessage implements MqttMessage {

    private String topic;

    private String clientId;

    private int qosLevel;

    private ByteBuf payload;

    private int messageId;

    private boolean will;

    private boolean dup;

    private boolean retain;

    private MessagePayloadType payloadType;

    @Override
    public String toString() {
        return print();
    }
}
