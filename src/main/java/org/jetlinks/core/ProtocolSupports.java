package org.jetlinks.core;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface ProtocolSupports {

    boolean isSupport(String protocol);

    Mono<ProtocolSupport> getProtocol(String protocol);

    Flux<ProtocolSupport> getProtocols();
}
