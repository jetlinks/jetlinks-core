package org.jetlinks.core.defaults;

import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.ProtocolSupports;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CompositeProtocolSupports implements ProtocolSupports {

    private final List<ProtocolSupports> supports = new CopyOnWriteArrayList<>();

    public void register(ProtocolSupports supports) {
        this.supports.add(supports);
    }

    @Override
    public boolean isSupport(String protocol) {
        for (ProtocolSupports support : supports) {
            if (support.isSupport(protocol)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Mono<ProtocolSupport> getProtocol(String protocol) {
        for (ProtocolSupports support : supports) {
            if (support.isSupport(protocol)) {
                return support.getProtocol(protocol);
            }
        }

        return Mono.error(new UnsupportedOperationException("不支持的协议:" + protocol));
    }

    @Override
    public Flux<ProtocolSupport> getProtocols() {
        return Flux.fromIterable(supports)
                   .flatMap(ProtocolSupports::getProtocols);
    }
}
