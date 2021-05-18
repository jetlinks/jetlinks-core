package org.jetlinks.core.server.session;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.Transport;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class LostDeviceSession implements DeviceSession{
    @Getter
    private final String id;

    @Getter
    private final DeviceOperator operator;

    @Getter
    private final Transport transport;

    @Override
    public String getDeviceId() {
        return operator.getDeviceId();
    }
    @Override
    public long lastPingTime() {
        return -1;
    }

    @Override
    public long connectTime() {
        return -1;
    }

    @Override
    public Mono<Boolean> send(EncodedMessage encodedMessage) {
        return Mono.error(new DeviceOperationException(ErrorCode.CONNECTION_LOST));
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
}
