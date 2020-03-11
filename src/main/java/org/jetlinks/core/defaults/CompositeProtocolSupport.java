package org.jetlinks.core.defaults;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.device.AuthenticationRequest;
import org.jetlinks.core.device.AuthenticationResponse;
import org.jetlinks.core.device.CompositeDeviceMessageSenderInterceptor;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.codec.DeviceMessageCodec;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.message.interceptor.DeviceMessageSenderInterceptor;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DeviceMetadataCodec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Getter
@Setter
public class CompositeProtocolSupport implements ProtocolSupport {

    private String id;

    private String name;

    private String description;

    private DeviceMetadataCodec metadataCodec;

    @Getter(AccessLevel.PRIVATE)
    private final Map<String, Supplier<Mono<ConfigMetadata>>> configMetadata = new ConcurrentHashMap<>();

    @Getter(AccessLevel.PRIVATE)
    private final Map<String, Supplier<Mono<DeviceMessageCodec>>> messageCodecSupports = new ConcurrentHashMap<>();

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private DeviceMessageSenderInterceptor deviceMessageSenderInterceptor;

    @Getter(AccessLevel.PRIVATE)
    private Map<String, Authenticator> authenticators = new ConcurrentHashMap<>();

    public void addMessageCodecSupport(Transport transport, Supplier<Mono<DeviceMessageCodec>> supplier) {
        messageCodecSupports.put(transport.getId(), supplier);
    }

    public void addMessageCodecSupport(Transport transport, DeviceMessageCodec codec) {
        messageCodecSupports.put(transport.getId(), () -> Mono.just(codec));
    }

    public void addMessageCodecSupport(DeviceMessageCodec codec) {
        addMessageCodecSupport(codec.getSupportTransport(), codec);
    }

    public void addAuthenticator(Transport transport, Authenticator authenticator) {
        authenticators.put(transport.getId(), authenticator);
    }

    public synchronized void addMessageSenderInterceptor(DeviceMessageSenderInterceptor interceptor) {
        if (this.deviceMessageSenderInterceptor == null) {
            this.deviceMessageSenderInterceptor = interceptor;
        } else {
            CompositeDeviceMessageSenderInterceptor composite;
            if (!(this.deviceMessageSenderInterceptor instanceof CompositeDeviceMessageSenderInterceptor)) {
                composite = new CompositeDeviceMessageSenderInterceptor();
                composite.addInterceptor(this.deviceMessageSenderInterceptor);
            } else {
                composite = ((CompositeDeviceMessageSenderInterceptor) this.deviceMessageSenderInterceptor);
            }
            composite.addInterceptor(interceptor);
            this.deviceMessageSenderInterceptor = composite;
        }
    }

    public void addConfigMetadata(Transport transport, Supplier<Mono<ConfigMetadata>> authenticator) {
        configMetadata.put(transport.getId(), authenticator);
    }

    public void addConfigMetadata(Transport transport, ConfigMetadata authenticator) {
        configMetadata.put(transport.getId(), () -> Mono.just(authenticator));
    }

    @Override
    public Flux<Transport> getSupportedTransport() {
        return Flux.fromIterable(messageCodecSupports.values())
                .flatMap(Supplier::get)
                .map(DeviceMessageCodec::getSupportTransport)
                .distinct(Transport::getId);
    }

    @Nonnull
    @Override
    public Mono<? extends DeviceMessageCodec> getMessageCodec(Transport transport) {
        return messageCodecSupports.getOrDefault(transport.getId(), Mono::empty).get();
    }

    @Nonnull
    @Override
    public DeviceMetadataCodec getMetadataCodec() {
        return metadataCodec;
    }

    @Nonnull
    @Override
    public Mono<AuthenticationResponse> authenticate(@Nonnull AuthenticationRequest request,
                                                     @Nonnull DeviceOperator deviceOperation) {
        return Mono.justOrEmpty(authenticators.get(request.getTransport().getId()))
                .flatMap(at -> at.authenticate(request, deviceOperation))
                .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("unsupported authentication request : " + request)));
    }

    @Override
    public Mono<ConfigMetadata> getConfigMetadata(Transport transport) {
        return configMetadata.getOrDefault(transport.getId(), Mono::empty).get();
    }
}
