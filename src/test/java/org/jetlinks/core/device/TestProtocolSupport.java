package org.jetlinks.core.device;

import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.codec.*;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.metadata.DeviceMetadataCodec;
import org.jetlinks.core.server.GatewayServerContextListener;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

public class TestProtocolSupport implements ProtocolSupport , ProtocolSupports {
    @Nonnull
    @Override
    public String getId() {
        return "test";
    }

    @Override
    public String getName() {
        return "test";
    }

    @Override
    public String getDescription() {
        return "test";
    }

    @Override
    public Flux<Transport> getSupportedTransport() {
        return Flux.empty();
    }

    @Nonnull
    @Override
    public Mono<DeviceMessageCodec> getMessageCodec(Transport transport) {
        return Mono.empty();
    }

    @Override
    public Mono<GatewayServerContextListener<?>> getServerContextHandler(Transport transport) {
        return Mono.empty();
    }

    @Nonnull
    @Override
    public DeviceMetadataCodec getMetadataCodec() {
        return new DeviceMetadataCodec() {
            @Override
            public Mono<DeviceMetadata> decode(String source) {
                return Mono.empty();
            }

            @Override
            public Mono<String> encode(DeviceMetadata metadata) {
                return Mono.empty();
            }
        };
    }

    @Nonnull
    @Override
    public Mono<AuthenticationResponse> authenticate(@Nonnull AuthenticationRequest request, @Nonnull DeviceOperator deviceOperation) {
        return Mono.just(AuthenticationResponse.success());
    }

    @Override
    public boolean isSupport(String protocol) {
        return true;
    }

    @Override
    public Mono<ProtocolSupport> getProtocol(String protocol) {
        return Mono.just(this);
    }

    @Override
    public Flux<ProtocolSupport> getProtocols() {
        return Flux.just(this);
    }
}
