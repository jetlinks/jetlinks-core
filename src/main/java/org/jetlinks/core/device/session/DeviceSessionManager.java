package org.jetlinks.core.device.session;

import org.jetlinks.core.server.session.DeviceSession;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.util.function.Function;

public interface DeviceSessionManager {

    Mono<Void> compute(String deviceId,Function<Mono<DeviceSession>,Mono<DeviceSession>> session);

    Mono<DeviceSession> getSession(String deviceId, boolean onlyLocal);

    Mono<DeviceSession> register(DeviceSession session);

    Mono<Void> unregister(String deviceId, boolean onlyLocal);

    Mono<Boolean> isAlive(String deviceId, boolean onlyLocal);

    Mono<Long> totalSessions(boolean onlyLocal);

    Disposable listenEvent(Function<DeviceSessionEvent, Mono<Void>> handler);


}
