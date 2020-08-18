package org.jetlinks.core.defaults;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.device.*;
import org.jetlinks.core.message.codec.DeviceMessageCodec;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.message.interceptor.DeviceMessageSenderInterceptor;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DeviceMetadataCodec;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
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

    private DeviceStateChecker deviceStateChecker;

    private volatile boolean disposed;

    private Disposable.Composite composite = Disposables.composite();

    private List<Consumer<Map<String, Object>>> doOnInit = new CopyOnWriteArrayList<>();

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        composite.dispose();
    }

    @Override
    public void init(Map<String, Object> configuration) {
        for (Consumer<Map<String, Object>> mapConsumer : doOnInit) {
            mapConsumer.accept(configuration);
        }
    }

    public CompositeProtocolSupport doOnDispose(Disposable disposable) {
        composite.add(disposable);
        return this;
    }

    public CompositeProtocolSupport doOnInit(Consumer<Map<String, Object>> runnable) {
        doOnInit.add(runnable);
        return this;
    }

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

    @Override
    public Mono<DeviceMessageSenderInterceptor> getSenderInterceptor() {
        return Mono.justOrEmpty(deviceMessageSenderInterceptor)
                .defaultIfEmpty(DeviceMessageSenderInterceptor.DO_NOTING);
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
                .flatMap(at -> at
                        .authenticate(request, deviceOperation)
                        .defaultIfEmpty(AuthenticationResponse.error(400, "unsupported")))
                .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("unsupported authentication request : " + request)));
    }

    @Nonnull
    @Override
    public Mono<AuthenticationResponse> authenticate(@Nonnull AuthenticationRequest request,
                                                     @Nonnull DeviceRegistry registry) {
        return Mono.justOrEmpty(authenticators.get(request.getTransport().getId()))
                .flatMap(at -> at
                        .authenticate(request, registry)
                        .defaultIfEmpty(AuthenticationResponse.error(400, "unsupported")))
                .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("unsupported authentication request : " + request)));
    }

    @Override
    public Mono<ConfigMetadata> getConfigMetadata(Transport transport) {
        return configMetadata.getOrDefault(transport.getId(), Mono::empty).get();
    }

    @Nonnull
    @Override
    public Mono<DeviceStateChecker> getStateChecker() {
        return Mono.justOrEmpty(deviceStateChecker);
    }
}
