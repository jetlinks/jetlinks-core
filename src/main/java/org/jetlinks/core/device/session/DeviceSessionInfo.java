package org.jetlinks.core.device.session;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.server.session.ChildrenDeviceSession;
import org.jetlinks.core.server.session.DeviceSession;

import java.io.Serializable;
import java.net.InetSocketAddress;

@Getter
@Setter
public class DeviceSessionInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 服务节点ID
     */
    private String serverId;

    /**
     * 设备地址
     *
     * @see InetSocketAddress
     */
    private String address;

    /**
     * 连接时间
     */
    private long connectTime;

    /**
     * 最后一次通信时间
     */
    private long lastCommTime;

    /**
     * 通信协议
     *
     * @see org.jetlinks.core.message.codec.Transport
     */
    private String transport;

    /**
     * 上级设备ID,子设备时有效
     */
    private String parentDeviceId;

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
        //子设备
        if (session.isWrapFrom(ChildrenDeviceSession.class)) {
            sessionInfo.setParentDeviceId(session.unwrap(ChildrenDeviceSession.class).getParentDevice().getDeviceId());
        }

        return sessionInfo;
    }
}
