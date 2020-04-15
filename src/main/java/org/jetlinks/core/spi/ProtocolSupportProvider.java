package org.jetlinks.core.spi;

import org.jetlinks.core.ProtocolSupport;
import reactor.core.publisher.Mono;

public interface ProtocolSupportProvider {

    Mono<? extends ProtocolSupport> create(ServiceContext context);

    default void close(){

    }
}
