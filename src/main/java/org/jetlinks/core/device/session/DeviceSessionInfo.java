package org.jetlinks.core.device.session;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.server.session.DeviceSession;

import java.io.Serializable;

@Getter
@Setter
public class DeviceSessionInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String deviceId;

    private String serverId;

    private String address;

    private long connectTime;

    private long lastCommTime;

    private String transport;

    public static DeviceSessionInfo of(String serverId, DeviceSession session) {
        DeviceSessionInfo sessionInfo = new DeviceSessionInfo();
        sessionInfo.setServerId(serverId);
        sessionInfo.setAddress(session.getClientAddress().map(String::valueOf).orElse(null));
        sessionInfo.setConnectTime(session.connectTime());
        sessionInfo.setDeviceId(session.getDeviceId());

        //上一次通信时间
        sessionInfo.setLastCommTime(session.lastPingTime());
        if (null != session.getTransport()) {
            sessionInfo.setTransport(session.getTransport().getId());
        }
        return sessionInfo;
    }
}
