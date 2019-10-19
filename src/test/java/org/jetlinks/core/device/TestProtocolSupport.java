package org.jetlinks.core.device;

import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.codec.*;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.metadata.DeviceMetadataCodec;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

public class TestProtocolSupport implements ProtocolSupport {
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

    @Nonnull
    @Override
    public DeviceMessageCodec getMessageCodec() {
        return new DeviceMessageCodec() {
            @Override
            public Mono<EncodedMessage> encode(Transport transport, MessageEncodeContext context) {
                return Mono.empty();
            }

            @Override
            public Mono<DeviceMessage> decode(Transport transport, MessageDecodeContext context) {
                return Mono.empty();
            }
        };
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
}
