package org.jetlinks.core.server.session;

import io.netty.util.ReferenceCountUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.utils.Reactors;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class LostDeviceSession implements DeviceSession {
    @Getter
    private final String id;

    @Getter
    private final DeviceOperator operator;

    @Getter
    private final Transport transport;

    private final long connectTime;

    public LostDeviceSession(String id, DeviceOperator device, Transport transport) {
        this(id, device, transport, System.currentTimeMillis());
    }

    @Override
    public String getDeviceId() {
        return operator == null ? id : operator.getDeviceId();
    }

    @Override
    public long lastPingTime() {
        return connectTime;
    }

    @Override
    public long connectTime() {
        return connectTime;
    }

    @Override
    public Mono<Boolean> send(EncodedMessage encodedMessage) {
        ReferenceCountUtil.safeRelease(encodedMessage.getPayload());
        return Mono.error(new DeviceOperationException.NoStackTrace(ErrorCode.CONNECTION_LOST));
    }

    @Override
    public void close() {

    }

    @Override
    public void ping() {

    }

    @Override
    public boolean isAlive() {
        return false;
    }

    @Override
    public void onClose(Runnable call) {

    }

    @Override
    public boolean isChanged(DeviceSession another) {
        return !another.isWrapFrom(LostDeviceSession.class);
    }

    @Override
    public Mono<Boolean> isAliveAsync() {
        return Reactors.ALWAYS_FALSE;
    }
}
