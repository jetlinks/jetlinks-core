package org.jetlinks.core.device.session;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.core.server.session.DeviceSession;

/**
 * 设备会话事件
 *
 * @author zhouhao
 * @since 1.2
 */
@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class DeviceSessionEvent {
    //时间戳,毫秒
    private long timestamp;

    //事件类型
    private Type type;

    //会话
    private DeviceSession session;

    //集群其他节点仍然存在会话
    //为true时表示会话同时在多个服务节点中创建
    private boolean clusterExists;

    public static DeviceSessionEvent of(Type type, DeviceSession session, boolean remoteExists) {
        return of(System.currentTimeMillis(), type, session, remoteExists);
    }

    public enum Type {
        //注册
        register,
        //注销
        unregister
    }
}
