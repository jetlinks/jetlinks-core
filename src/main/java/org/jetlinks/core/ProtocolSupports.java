package org.jetlinks.core;

import reactor.core.publisher.Mono;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface ProtocolSupports {
    Mono<ProtocolSupport> getProtocol(String protocol);
}
