package org.jetlinks.core.message.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
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

    public static SimpleMqttMessageBuilder builder() {
        return new SimpleMqttMessageBuilder();
    }

    @Override
    public String toString() {
        return print();
    }

    /**
     * <pre>
     *     QoS0 /topic
     *
     *     {"hello":"world"}
     * </pre>
     *
     * @param str
     * @return
     */
    public static SimpleMqttMessage of(String str) {
        SimpleMqttMessage mqttMessage = new SimpleMqttMessage();
        TextMessageParser.of(
                start -> {
                    //QoS0 /topic
                    String[] qosAndTopic = start.split("[ ]");
                    if (qosAndTopic.length == 1) {
                        mqttMessage.setTopic(qosAndTopic[0]);
                    } else {
                        mqttMessage.setTopic(qosAndTopic[1]);
                        String qos = qosAndTopic[0].toLowerCase();
                        if (qos.length() == 1) {
                            mqttMessage.setQosLevel(Integer.parseInt(qos));
                        } else {
                            mqttMessage.setQosLevel(Integer.parseInt(qos.substring(qos.length() - 1)));
                        }
                    }
                },
                (header, value) -> {

                },
                body -> {
                    mqttMessage.setPayload(Unpooled.wrappedBuffer(body.getBody()));
                    mqttMessage.setPayloadType(body.getType());
                },
                () -> mqttMessage.setPayload(Unpooled.wrappedBuffer(new byte[0]))
        ).parse(str);

        return mqttMessage;
    }

    public static class SimpleMqttMessageBuilder {
        private String topic;
        private String clientId;
        private int qosLevel;
        private ByteBuf payload;
        private int messageId;
        private boolean will;
        private boolean dup;
        private boolean retain;
        private MessagePayloadType payloadType;

        SimpleMqttMessageBuilder() {
        }

        public SimpleMqttMessageBuilder body(String payload) {
            return payload(payload.getBytes());
        }

        public SimpleMqttMessageBuilder body(byte[] payload) {
            return payload(Unpooled.wrappedBuffer(payload));
        }

        public SimpleMqttMessageBuilder payload(String payload) {
            return payload(payload.getBytes());
        }

        public SimpleMqttMessageBuilder payload(byte[] payload) {
            return payload(Unpooled.wrappedBuffer(payload));
        }

        public SimpleMqttMessageBuilder topic(String topic) {
            this.topic = topic;
            return this;
        }

        public SimpleMqttMessageBuilder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public SimpleMqttMessageBuilder qosLevel(int qosLevel) {
            this.qosLevel = qosLevel;
            return this;
        }

        public SimpleMqttMessageBuilder payload(ByteBuf payload) {
            this.payload = payload;
            return this;
        }

        public SimpleMqttMessageBuilder messageId(int messageId) {
            this.messageId = messageId;
            return this;
        }

        public SimpleMqttMessageBuilder will(boolean will) {
            this.will = will;
            return this;
        }

        public SimpleMqttMessageBuilder dup(boolean dup) {
            this.dup = dup;
            return this;
        }

        public SimpleMqttMessageBuilder retain(boolean retain) {
            this.retain = retain;
            return this;
        }

        public SimpleMqttMessageBuilder payloadType(MessagePayloadType payloadType) {
            this.payloadType = payloadType;
            return this;
        }

        public SimpleMqttMessage build() {
            return new SimpleMqttMessage(topic, clientId, qosLevel, payload, messageId, will, dup, retain, payloadType);
        }

        public String toString() {
            return "SimpleMqttMessage.SimpleMqttMessageBuilder(topic=" + this.topic + ", clientId=" + this.clientId + ", qosLevel=" + this.qosLevel + ", payload=" + this.payload + ", messageId=" + this.messageId + ", will=" + this.will + ", dup=" + this.dup + ", retain=" + this.retain + ", payloadType=" + this.payloadType + ")";
        }
    }
}
