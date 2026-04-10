package org.jetlinks.core.device.session;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.server.ClientConnection;
import org.jetlinks.core.server.ConnectionMetrics;
import org.jetlinks.core.server.ConnectionMetricsInfo;
import reactor.core.Scannable;

import java.net.InetSocketAddress;

@Getter
@Setter
public class DeviceConnectionInfo {

    /**
     * 连接ID
     */
    private String id;

    /**
     * 设备地址
     *
     * @see InetSocketAddress
     */
    private String address;

    /**
     * 等待处理的消息数量
     *
     * @since 1.3
     */
    private Long pendingMessages;

    /**
     * 监控指标
     */
    private ConnectionMetricsInfo metrics;

    public static DeviceConnectionInfo of(ClientConnection connection) {
        DeviceConnectionInfo info = new DeviceConnectionInfo();
        info.setAddress(connection.address() == null ? "unknown" : connection.address().toString());
        info.setPendingMessages(connection.scanOrDefault(Scannable.Attr.LARGE_BUFFERED, 0L));
        ConnectionMetrics connectionMetrics = connection.metrics();
        if (connectionMetrics != null) {
            info.setMetrics(connectionMetrics.toInfo());
        }
        info.id = connection.id();
        return info;
    }

}
