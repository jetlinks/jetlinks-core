package org.jetlinks.core.server;

import org.jetlinks.core.server.session.DeviceSession;

public interface DeviceSessionListener {

    void onSessionCreated(DeviceSession session);

}
