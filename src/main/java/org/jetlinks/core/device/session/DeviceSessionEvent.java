package org.jetlinks.core.device.session;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.core.server.session.DeviceSession;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class DeviceSessionEvent {
    private long timestamp;

    private Type type;

    private DeviceSession session;

    public static DeviceSessionEvent of(Type type, DeviceSession session) {
        return of(System.currentTimeMillis(), type, session);
    }

    public enum Type {
        unregister,
        register,
        timeout
    }
}
