package org.jetlinks.core.defaults;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.Value;
import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.core.config.ConfigStorage;
import org.jetlinks.core.config.ConfigStorageManager;
import org.jetlinks.core.config.StorageConfigurable;
import org.jetlinks.core.device.*;
import org.jetlinks.core.message.DeviceMessageReply;
import org.jetlinks.core.message.DisconnectDeviceMessage;
import org.jetlinks.core.message.interceptor.DeviceMessageSenderInterceptor;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.utils.IdUtils;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class DefaultDeviceOperator implements DeviceOperator, StorageConfigurable {

    private final String id;

    private final String configStorageKey;

    private ConfigStorageManager manager;

    private DeviceMessageHandler handler;

    private DeviceRegistry registry;

    private DeviceMessageSender messageSender;

    protected ProtocolSupports supports;


    public DefaultDeviceOperator(String id,
                                 ProtocolSupports supports,
                                 ConfigStorageManager storageManager,
                                 DeviceMessageHandler handler,
                                 DeviceMessageSenderInterceptor interceptor,
                                 DeviceRegistry registry) {
        this.id = id;
        this.supports = supports;
        this.manager = storageManager;
        this.registry = registry;
        this.handler = handler;
        this.configStorageKey = "device:" + id;
        this.messageSender = new DefaultDeviceMessageSender(handler, this, interceptor);
    }

    @Override
    public Mono<ConfigStorage> getReactiveStorage() {
        return manager.getStorage(configStorageKey);
    }

    @Override
    public String getDeviceId() {
        return id;
    }

    @Override
    public Mono<String> getConnectionServerId() {
        return getConfig(DeviceConfigKey.connectionServerId);
    }

    @Override
    public Mono<String> getSessionId() {
        return getConfig(DeviceConfigKey.sessionId);
    }

    @Override
    public Mono<Boolean> putState(byte state) {
        return setConfig("state", state);
    }

    @Override
    public Mono<Byte> getState() {
        return getConfig("state")
                .map(v -> v.as(Byte.class))
                .defaultIfEmpty(DeviceState.unknown);
    }

    @Override
    public Mono<Byte> checkState() {
        return getConnectionServerId()
                .flatMap(server -> handler.getDeviceState(server, Collections.singletonList(id)))
                .flatMap(map -> Mono.justOrEmpty(map.get(id)))
                .defaultIfEmpty(DeviceState.offline)
                .flatMap(state ->
                        getState()
                                .defaultIfEmpty(DeviceState.unknown)
                                .flatMap(current -> {
                                    if (!current.equals(state)) {
                                        log.warn("device[{}] state changed to {}", getDeviceId(), state);
                                        return putState(state)
                                                .thenReturn(state);
                                    }
                                    return Mono.just(state);
                                }));
    }

    @Override
    public Mono<Long> getOnlineTime() {
        return getConfig("onlineTime")
                .map(val -> val.as(Long.class));
    }

    @Override
    public Mono<Long> getOfflineTime() {
        return getConfig("offlineTime")
                .map(val -> val.as(Long.class));
    }

    @Override
    public Mono<Boolean> online(String serverId, String sessionId) {
        return setConfigs(
                DeviceConfigKey.connectionServerId.value(serverId),
                DeviceConfigKey.sessionId.value(serverId),
                ConfigKey.of("onlineTime").value(System.currentTimeMillis()),
                ConfigKey.of("state").value(DeviceState.online)
        );
    }

    @Override
    public Mono<Boolean> offline() {
        return removeConfigs(DeviceConfigKey.connectionServerId, DeviceConfigKey.sessionId)
                .flatMap(nil -> setConfigs(
                        ConfigKey.of("offlineTime").value(System.currentTimeMillis()),
                        ConfigKey.of("state").value(DeviceState.offline)
                ));
    }

    @Override
    public Mono<Boolean> disconnect() {
        DisconnectDeviceMessage disconnect = new DisconnectDeviceMessage();
        disconnect.setDeviceId(getDeviceId());
        disconnect.setMessageId(IdUtils.newUUID());

        return messageSender()
                .send(Mono.just(disconnect))
                .next()
                .map(DeviceMessageReply::isSuccess);
    }

    @Override
    public Mono<AuthenticationResponse> authenticate(AuthenticationRequest request) {
        return getProtocol()
                .flatMap(protocolSupport -> protocolSupport.authenticate(request, this));
    }

    private AtomicReference<DeviceMetadata> metadataCache = new AtomicReference<>();

    @Override
    public Mono<DeviceMetadata> getMetadata() {
        return Mono.justOrEmpty(metadataCache.get())
                .switchIfEmpty(getProtocol()
                        .flatMap(protocol -> getConfig(DeviceConfigKey.metadata)
                                .flatMap(protocol.getMetadataCodec()::decode)
                                .doOnNext(metadataCache::set)))
                .switchIfEmpty(getParent().flatMap(DeviceProductOperator::getMetadata));
    }


    @Override
    public Mono<DeviceProductOperator> getParent() {
        return getReactiveStorage()
                .flatMap(store -> store.getConfig(DeviceConfigKey.productId.getKey()))
                .map(Value::asString)
                .flatMap(registry::getProduct);
    }

    @Override
    public Mono<ProtocolSupport> getProtocol() {
        return getConfig(DeviceConfigKey.protocol)
                .flatMap(supports::getProtocol);
    }

    @Override
    public DeviceMessageSender messageSender() {
        return messageSender;
    }

    @Override
    public Mono<Boolean> updateMetadata(String metadata) {
        return setConfig(DeviceConfigKey.metadata.value(metadata));
    }
}
