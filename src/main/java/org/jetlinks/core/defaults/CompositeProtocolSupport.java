package org.jetlinks.core.defaults;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.device.*;
import org.jetlinks.core.message.codec.DeviceMessageCodec;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.message.interceptor.DeviceMessageSenderInterceptor;
import org.jetlinks.core.metadata.*;
import org.jetlinks.core.route.Route;
import org.jetlinks.core.server.ClientConnection;
import org.jetlinks.core.server.DeviceGatewayContext;
import org.jetlinks.core.things.ThingRpcSupportChain;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Getter
@Setter
public class CompositeProtocolSupport implements ProtocolSupport {

    private String id;

    private String name;

    private String description;

    private DeviceMetadataCodec metadataCodec = DeviceMetadataCodecs.defaultCodec();

    @Getter(AccessLevel.PRIVATE)
    private final Map<String, Supplier<Mono<ConfigMetadata>>> configMetadata = new ConcurrentHashMap<>();

    @Getter(AccessLevel.PRIVATE)
    private final Map<String, Supplier<Mono<DeviceMetadata>>> defaultDeviceMetadata = new ConcurrentHashMap<>();

    @Getter(AccessLevel.PRIVATE)
    private final Map<String, Supplier<Mono<DeviceMessageCodec>>> messageCodecSupports = new ConcurrentHashMap<>();

    @Getter(AccessLevel.PRIVATE)
    private Map<String, ExpandsConfigMetadataSupplier> expandsConfigSupplier = new ConcurrentHashMap<>();


    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private DeviceMessageSenderInterceptor deviceMessageSenderInterceptor;

    @Getter(AccessLevel.PRIVATE)
    private Map<String, Authenticator> authenticators = new ConcurrentHashMap<>();

    private DeviceStateChecker deviceStateChecker;

    private volatile boolean disposed;

    private Disposable.Composite composite = Disposables.composite();

    private Mono<ConfigMetadata> initConfigMetadata = Mono.empty();

    private List<DeviceMetadataCodec> metadataCodecs = new ArrayList<>();

    private List<Consumer<Map<String, Object>>> doOnInit = new CopyOnWriteArrayList<>();

    private Function<DeviceOperator, Mono<Void>> onDeviceRegister;
    private Function<DeviceOperator, Mono<Void>> onDeviceUnRegister;
    private Function<DeviceOperator, Mono<Void>> onDeviceMetadataChanged;

    private Function<DeviceProductOperator, Mono<Void>> onProductRegister;
    private Function<DeviceProductOperator, Mono<Void>> onProductUnRegister;
    private Function<DeviceProductOperator, Mono<Void>> onProductMetadataChanged;

    private BiFunction<DeviceOperator, Flux<DeviceOperator>, Mono<Void>> onChildBind;
    private BiFunction<DeviceOperator, Flux<DeviceOperator>, Mono<Void>> onChildUnbind;

    private Map<String, Function<DeviceInfo, Mono<DeviceInfo>>> onBeforeCreate = new ConcurrentHashMap<>();

    private Map<String, BiFunction<ClientConnection, DeviceGatewayContext, Mono<Void>>> connectionHandlers = new ConcurrentHashMap<>();

    private Map<String, Flux<Feature>> features = new ConcurrentHashMap<>();
    private List<Feature> globalFeatures = new CopyOnWriteArrayList<>();

    private Map<String, List<Route>> routes = new ConcurrentHashMap<>();
    private Map<String, Supplier<String>> docFiles = new ConcurrentHashMap<>();

    private ThingRpcSupportChain rpcChain;

    private int order = Integer.MAX_VALUE;

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        composite.dispose();
        configMetadata.clear();
        defaultDeviceMetadata.clear();
        messageCodecSupports.clear();
        expandsConfigSupplier.clear();
    }

    public void setInitConfigMetadata(ConfigMetadata metadata) {
        initConfigMetadata = Mono.just(metadata);
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

    public void removeMessageCodecSupport(Transport transport) {
        messageCodecSupports.remove(transport.getId());
    }

    public void addAuthenticator(Transport transport, Authenticator authenticator) {
        authenticators.put(transport.getId(), authenticator);
    }

    public void addDefaultMetadata(Transport transport, Mono<DeviceMetadata> metadata) {
        defaultDeviceMetadata.put(transport.getId(), () -> metadata);
    }

    public void addDefaultMetadata(Transport transport, DeviceMetadata metadata) {
        defaultDeviceMetadata.put(transport.getId(), () -> Mono.just(metadata));
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

    public void addConfigMetadata(Transport transport, Supplier<Mono<ConfigMetadata>> metadata) {
        configMetadata.put(transport.getId(), metadata);
    }

    public void addConfigMetadata(Transport transport, ConfigMetadata metadata) {
        configMetadata.put(transport.getId(), () -> Mono.just(metadata));
    }


    public void setExpandsConfigMetadata(Transport transport,
                                         ExpandsConfigMetadataSupplier supplier) {
        expandsConfigSupplier.put(transport.getId(), supplier);
    }

    public void addRoutes(Transport transport, Collection<? extends Route> routes) {
        this.routes.computeIfAbsent(transport.getId(), id -> new ArrayList<>())
                   .addAll(routes);
    }


    @Override
    public Flux<ConfigMetadata> getMetadataExpandsConfig(Transport transport,
                                                         DeviceMetadataType metadataType,
                                                         String metadataId,
                                                         String dataTypeId) {

        return Optional
                .ofNullable(expandsConfigSupplier.get(transport.getId()))
                .map(supplier -> supplier.getConfigMetadata(metadataType, metadataId, dataTypeId))
                .orElse(Flux.empty());
    }

    @Override
    public Mono<DeviceMetadata> getDefaultMetadata(Transport transport) {
        return Optional
                .ofNullable(defaultDeviceMetadata.get(transport.getId()))
                .map(Supplier::get)
                .orElse(Mono.empty());
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

    public Flux<DeviceMetadataCodec> getMetadataCodecs() {
        return Flux.merge(Flux.just(metadataCodec), Flux.fromIterable(metadataCodecs));
    }

    public void addDeviceMetadataCodec(DeviceMetadataCodec codec) {
        metadataCodecs.add(codec);
    }

    @Nonnull
    @Override
    public Mono<AuthenticationResponse> authenticate(@Nonnull AuthenticationRequest request,
                                                     @Nonnull DeviceOperator deviceOperation) {
        return Mono.justOrEmpty(authenticators.get(request.getTransport().getId()))
                   .flatMap(at -> at
                           .authenticate(request, deviceOperation)
                           .defaultIfEmpty(AuthenticationResponse.error(400, "无法获取认证结果")))
                   .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("不支持的认证请求:" + request)));
    }

    @Nonnull
    @Override
    public Mono<AuthenticationResponse> authenticate(@Nonnull AuthenticationRequest request,
                                                     @Nonnull DeviceRegistry registry) {
        return Mono.justOrEmpty(authenticators.get(request.getTransport().getId()))
                   .flatMap(at -> at
                           .authenticate(request, registry)
                           .defaultIfEmpty(AuthenticationResponse.error(400, "无法获取认证结果")))
                   .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("不支持的认证请求:" + request)));
    }

    @Override
    public Mono<ConfigMetadata> getConfigMetadata(Transport transport) {
        return configMetadata.getOrDefault(transport.getId(), Mono::empty).get();
    }

    public Mono<ConfigMetadata> getInitConfigMetadata() {
        return initConfigMetadata;
    }

    @Nonnull
    @Override
    public Mono<DeviceStateChecker> getStateChecker() {
        return Mono.justOrEmpty(deviceStateChecker);
    }

    public CompositeProtocolSupport doOnDeviceRegister(Function<DeviceOperator, Mono<Void>> executor) {
        this.onDeviceRegister = executor;
        return this;
    }

    public CompositeProtocolSupport doOnDeviceUnRegister(Function<DeviceOperator, Mono<Void>> executor) {
        this.onDeviceUnRegister = executor;
        return this;
    }

    public CompositeProtocolSupport doOnProductRegister(Function<DeviceProductOperator, Mono<Void>> executor) {
        this.onProductRegister = executor;
        return this;
    }

    public CompositeProtocolSupport doOnProductUnRegister(Function<DeviceProductOperator, Mono<Void>> executor) {
        this.onProductUnRegister = executor;
        return this;
    }

    public CompositeProtocolSupport doOnProductMetadataChanged(Function<DeviceProductOperator, Mono<Void>> executor) {
        this.onProductMetadataChanged = executor;
        return this;
    }

    public CompositeProtocolSupport doOnDeviceMetadataChanged(Function<DeviceOperator, Mono<Void>> executor) {
        this.onDeviceMetadataChanged = executor;
        return this;
    }

    /**
     * 监听客户端连接,只有部分协议支持此操作,如:
     * <pre>{@code
     * support.doOnClientConnect(TCP,(connection,context)->{
     *  //客户端创建连接时,发送消息给客户端
     *  return connection
     *   .sendMessage(createHelloMessage())
     *    .then();
     *  })
     * }</pre>
     *
     * @param transport 通信协议,如: {@link org.jetlinks.core.message.codec.DefaultTransport#TCP}
     * @param handler   处理器
     * @since 1.1.6
     */
    public void doOnClientConnect(Transport transport,
                                  BiFunction<ClientConnection, DeviceGatewayContext, Mono<Void>> handler) {
        connectionHandlers.put(transport.getId(), handler);
    }

    @Override
    public Mono<Void> onDeviceRegister(DeviceOperator operator) {
        return onDeviceRegister != null ? onDeviceRegister.apply(operator) : Mono.empty();
    }

    @Override
    public Mono<Void> onDeviceUnRegister(DeviceOperator operator) {
        return onDeviceUnRegister != null ? onDeviceUnRegister.apply(operator) : Mono.empty();
    }

    @Override
    public Mono<Void> onProductRegister(DeviceProductOperator operator) {
        return onProductRegister != null ? onProductRegister.apply(operator) : Mono.empty();
    }

    @Override
    public Mono<Void> onProductUnRegister(DeviceProductOperator operator) {
        return onProductUnRegister != null ? onProductUnRegister.apply(operator) : Mono.empty();
    }

    @Override
    public Mono<Void> onDeviceMetadataChanged(DeviceOperator operator) {
        return onDeviceMetadataChanged != null ? onDeviceMetadataChanged.apply(operator) : Mono.empty();
    }

    @Override
    public Mono<Void> onProductMetadataChanged(DeviceProductOperator operator) {
        return onProductMetadataChanged != null ? onProductMetadataChanged.apply(operator) : Mono.empty();
    }

    public void doOnChildBind(BiFunction<DeviceOperator, Flux<DeviceOperator>, Mono<Void>> onChildBind) {
        this.onChildBind = onChildBind;
    }

    public void doOnChildUnbind(BiFunction<DeviceOperator, Flux<DeviceOperator>, Mono<Void>> onChildUnbind) {
        this.onChildUnbind = onChildUnbind;
    }

    @Override
    public Mono<Void> onClientConnect(Transport transport,
                                      ClientConnection connection,
                                      DeviceGatewayContext context) {
        BiFunction<ClientConnection, DeviceGatewayContext, Mono<Void>> function = connectionHandlers.get(transport.getId());
        if (function == null) {
            return Mono.empty();
        }
        return function.apply(connection, context);
    }

    @Override
    public Mono<Void> onChildBind(DeviceOperator gateway, Flux<DeviceOperator> child) {
        return onChildBind == null ? Mono.empty() : onChildBind.apply(gateway, child);
    }

    @Override
    public Mono<Void> onChildUnbind(DeviceOperator gateway, Flux<DeviceOperator> child) {
        return onChildUnbind == null ? Mono.empty() : onChildUnbind.apply(gateway, child);
    }

    /**
     * 给指定的Transport添加Feature
     *
     * @param features Feature
     */
    public void addFeature(Transport transport, Feature... features) {
        addFeature(transport, Flux.just(features));
    }

    /**
     * 给指定的Transport添加Feature
     *
     * @param features Feature
     */
    public void addFeature(Transport transport, Iterable<Feature> features) {
        addFeature(transport, Flux.fromIterable(features));
    }

    /**
     * 给指定的Transport添加Feature
     *
     * @param features Feature
     */
    public void addFeature(Transport transport, Flux<Feature> features) {
        this.features.put(transport.getId(), features);
    }

    /**
     * 添加全局Feature
     *
     * @param features Feature
     * @see MetadataFeature
     * @see ManagementFeature
     */
    public void addFeature(Feature... features) {
        addFeature(Arrays.asList(features));
    }

    /**
     * 添加全局Feature
     *
     * @param features Feature
     */
    public void addFeature(Iterable<Feature> features) {
        features.forEach(globalFeatures::add);
    }

    /**
     * 注册设备添加监听器,用于在创建设备时,进行自定义配置生成等操作.
     *
     * @param transport 传输协议
     * @param listener  监听器
     * @since 1.1.8
     */
    public void onBeforeDeviceCreate(Transport transport, Function<DeviceInfo, Mono<DeviceInfo>> listener) {
        onBeforeCreate.put(transport.getId(), listener);
    }


    @SneakyThrows
    public void setDocument(Transport transport, String documentUrlOrFile,ClassLoader loader) {
        if (documentUrlOrFile.startsWith("http")) {
            setDocument(transport, () -> documentUrlOrFile);
        } else {
            setDocument(transport, () -> new ClassPathResource(documentUrlOrFile, loader));
        }
    }

    public void setDocument(Transport transport, Supplier<String> document) {

        docFiles.put(transport.getId(), document);
    }

    public void setDocument(Transport transport, Callable<Resource> document) {
        setDocument(transport, () -> {
            try {
                Resource resource = document.call();
                if (resource.exists()) {
                    try (InputStream input = resource.getInputStream()) {
                        return StreamUtils.copyToString(input, StandardCharsets.UTF_8);
                    }
                }
            } catch (Throwable ignore) {
            }
            return null;
        });
    }

    @Override
    public Mono<DeviceInfo> doBeforeDeviceCreate(Transport transport, DeviceInfo deviceInfo) {
        Function<DeviceInfo, Mono<DeviceInfo>> listener = onBeforeCreate.get(transport.getId());
        if (null != listener) {
            return listener.apply(deviceInfo);
        }
        return ProtocolSupport.super.doBeforeDeviceCreate(transport, deviceInfo);
    }

    @Override
    public Flux<Feature> getFeatures(Transport transport) {
        return Flux
                .concat(
                        Flux.fromIterable(globalFeatures),
                        features.getOrDefault(transport.getId(), Flux.empty())
                )
                .distinct(Feature::getId);
    }

    @Override
    public Flux<Route> getRoutes(Transport transport) {
        return Flux.fromIterable(routes.getOrDefault(transport.getId(), Collections.emptyList()));
    }

    @Override
    public String getDocument(Transport transport) {
        Supplier<String> docFile = docFiles.get(transport.getId());
        if (docFile == null) {
            return null;
        }
        return docFile.get();
    }
}
