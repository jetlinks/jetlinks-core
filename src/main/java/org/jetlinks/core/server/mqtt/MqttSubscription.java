package org.jetlinks.core.server.mqtt;

import org.jetlinks.core.server.session.DeviceSession;

import java.util.List;

public interface MqttSubscription {
    DeviceSession getSession();

    int getMessageId();

    List<Topic> getTopics();

    void accept(Integer... qos);

    interface Topic{
        String getName();

        int getQos();
    }
}
