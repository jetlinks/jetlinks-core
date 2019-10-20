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
        return supports
                .stream()
                .anyMatch(supports -> supports.isSupport(protocol));
    }

    @Override
    public Mono<ProtocolSupport> getProtocol(String protocol) {
        return supports.stream()
                .filter(supports -> supports.isSupport(protocol))
                .findFirst()
                .map(supports -> supports.getProtocol(protocol))
                .orElseGet(() -> Mono.error(new UnsupportedOperationException("不支持的协议:" + protocol)));
    }

    @Override
    public Flux<ProtocolSupport> getProtocols() {
        return Flux.fromIterable(supports)
                .flatMap(ProtocolSupports::getProtocols);
    }
}
