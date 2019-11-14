package org.jetlinks.core.message.codec;

import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

public interface MqttMessageCodecDescription {
    Flux<Topic> getUpStreamTopics();

    Flux<Topic> getDownStreamTopics();

    interface Topic {

        MessagePayloadType getPayloadType();

        String getTopic();

        List<String> getVariables();

        String getTopic(Map<String, Object> variables);

        String getTemplatePayload();

        String getDescription();

    }
}
