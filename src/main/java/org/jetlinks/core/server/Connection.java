package org.jetlinks.core.server;

import org.jetlinks.core.message.codec.EncodedMessage;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;

public interface Connection {

    @Nullable
    InetSocketAddress getRemoteAddress();

    Mono<Void> send(EncodedMessage message);

    void close();
}
