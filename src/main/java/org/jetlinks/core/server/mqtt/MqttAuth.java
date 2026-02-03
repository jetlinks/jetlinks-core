package org.jetlinks.core.server.mqtt;

import org.jetlinks.core.device.identity.PasswordCredential;

public interface MqttAuth extends PasswordCredential {
    String getUsername();

    String getPassword();
}
