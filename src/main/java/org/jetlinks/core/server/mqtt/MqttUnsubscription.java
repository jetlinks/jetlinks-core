package org.jetlinks.core.server.mqtt;

import org.jetlinks.core.server.session.DeviceSession;

import java.util.List;

public interface MqttUnsubscription {
    DeviceSession getSession();

    int getMessageId();

    List<String> getTopics();

    void accept();

}
