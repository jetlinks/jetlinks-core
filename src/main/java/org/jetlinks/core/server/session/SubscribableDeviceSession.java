package org.jetlinks.core.server.session;

import reactor.core.publisher.Mono;

public interface SubscribableDeviceSession {
    void subscribe(String address);

    void unsubscribe(String address);

    Mono<Boolean> subscribed(String addressPattern);
}
