package org.jetlinks.core.message.codec;

import org.jetlinks.core.server.ClientConnection;
import reactor.core.publisher.Mono;

public interface MessageParserFactory {

    Mono<MessageParser> create(ClientConnection connection);
}
