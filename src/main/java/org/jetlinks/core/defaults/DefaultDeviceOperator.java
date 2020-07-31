package org.jetlinks.core.defaults;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.Value;
import org.jetlinks.core.Values;
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
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.jetlinks.core.device.DeviceConfigKey.*;
import static org.jetlinks.core.device.DeviceConfigKey.selfManageState;

@Slf4j
public class DefaultDeviceOperator implements DeviceOperator, StorageConfigurable {

    private final String id;

    private final DeviceOperationBroker handler;

    private final DeviceRegistry registry;

    private final DeviceMessageSender messageSender;

    private final ProtocolSupports supports;

    private final Mono<ConfigStorage> storageMono;

    public DefaultDeviceOperator(String id,
                                 ProtocolSupports supports,
                                 ConfigStorageManager storageManager,
                                 DeviceOperationBroker handler,
                                 DeviceRegistry registry) {
        this(id, supports, storageManager, handler, registry, DeviceMessageSenderInterceptor.DO_NOTING);

    }

    public DefaultDeviceOperator(String id,
                                 ProtocolSupports supports,
                                 ConfigStorageManager storageManager,
                                 DeviceOperationBroker handler,
                                 DeviceRegistry registry,
                                 DeviceMessageSenderInterceptor interceptor) {
        this.id = id;
        this.supports = supports;
        this.registry = registry;
        this.handler = handler;
        this.messageSender = new DefaultDeviceMessageSender(handler, this, registry, interceptor);
        this.storageMono = storageManager.getStorage("device:" + id);

    }

    @Override
    public Mono<ConfigStorage> getReactiveStorage() {
        return storageMono;
    }

    @Override
    public String getDeviceId() {
        return id;
    }

    @Override
    public Mono<String> getConnectionServerId() {
        return getSelfConfig(connectionServerId.getKey())
                .map(Value::asString);
    }

    @Override
    public Mono<String> getSessionId() {
        return getSelfConfig(sessionId.getKey())
                .map(Value::asString);
    }

    @Override
    public Mono<String> getAddress() {
        return getConfig("address")
                .map(Value::asString);
    }

    @Override
    public Mono<Void> setAddress(String address) {
        return setConfig("address", address)
                .then();
    }

    @Override
    public Mono<Boolean> putState(byte state) {
        return setConfig("state", state);
    }

    @Override
    public Mono<Byte> getState() {
        return getSelfConfigs(Arrays.asList("state", parentGatewayId.getKey(), selfManageState.getKey()))
                .flatMap(values -> {
                    Byte state = values.getValue("state")
                            .map(val -> val.as(Byte.class))
                            .orElse(DeviceState.unknown);

                    boolean isSelfManageState = values.getValue(selfManageState.getKey())
                            .map(val -> val.as(Boolean.class))
                            .orElse(false);
                    if (isSelfManageState) {
                        return Mono.just(state);
                    }
                    String parentGatewayId = values
                            .getValue(DeviceConfigKey.parentGatewayId)
                            .orElse(null);

                    //获取父级设备状态
                    if (!state.equals(DeviceState.online) && StringUtils.hasText(parentGatewayId)) {
                        return registry
                                .getDevice(parentGatewayId)
                                .flatMap(DeviceOperator::getState);
                    }
                    return Mono.just(state);
                })
                .defaultIfEmpty(DeviceState.unknown);
    }


    private Mono<Byte> doCheckState() {
        return getSelfConfigs(Arrays.asList(
                connectionServerId.getKey(),
                parentGatewayId.getKey(),
                selfManageState.getKey(),
                "state"))
                .flatMap(values -> {

                    //当前设备连接到的服务器
                    String server = values
                            .getValue(connectionServerId)
                            .orElse(null);

                    //设备缓存的状态
                    Byte state = values.getValue("state")
                            .map(val -> val.as(Byte.class))
                            .orElse(DeviceState.unknown);


                    //如果缓存中存储有当前设备所在服务信息则尝试发起状态检查
                    if (StringUtils.hasText(server)) {
                        return handler
                                .getDeviceState(server, Collections.singletonList(id))
                                .map(DeviceStateInfo::getState)
                                .singleOrEmpty()
                                .timeout(Duration.ofSeconds(1), Mono.just(state))
                                .defaultIfEmpty(state);
                    }

                    //子设备是否自己管理状态
                    if (values.getValue(selfManageState).orElse(false)) {
                        return Mono.just(state);
                    }

                    //网关设备ID
                    String parentGatewayId = values
                            .getValue(DeviceConfigKey.parentGatewayId)
                            .orElse(null);

                    //如果关联了上级网关设备则获取父设备状态
                    if (StringUtils.hasText(parentGatewayId)) {
                        return registry
                                .getDevice(parentGatewayId)
                                .flatMap(DeviceOperator::checkState);
                    }

                    //如果是在线状态,则改为离线,否则保持状态不变
                    if (state.equals(DeviceState.online)) {
                        return Mono.just(DeviceState.offline);
                    } else {
                        return Mono.just(state);
                    }
                });
    }

    @Override
    public Mono<Byte> checkState() {

        return getProtocol()
                .flatMap(ProtocolSupport::getStateChecker) //协议自定义了状态检查逻辑
                .defaultIfEmpty(device -> doCheckState()) //默认的检查
                .flatMap(deviceStateChecker ->
                        //检查最新状态 并和当前状态进行对比
                        Mono.zip(deviceStateChecker.checkState(this).defaultIfEmpty(DeviceState.offline), getState())
                )
                .flatMap(tp2 -> {
                    byte newer = tp2.getT1();
                    byte old = tp2.getT2();
                    //状态不一致?
                    if (newer != old) {
                        log.info("device[{}] state changed from {} to {}", getDeviceId(), old, newer);
                        Map<String, Object> configs = new HashMap<>();
                        configs.put("state", newer);
                        if (newer == DeviceState.online) {
                            configs.put("onlineTime", System.currentTimeMillis());
                        } else if (newer == DeviceState.offline) {
                            configs.put("offlineTime", System.currentTimeMillis());
                        }
                        return setConfigs(configs)
                                .thenReturn(newer);
                    }
                    return Mono.just(newer);
                });
    }

    @Override
    public Mono<Long> getOnlineTime() {
        return getSelfConfig("onlineTime")
                .map(val -> val.as(Long.class))
                .switchIfEmpty(Mono.defer(() ->
                        this.getSelfConfig(parentGatewayId)
                                .flatMap(registry::getDevice)
                                .flatMap(DeviceOperator::getOnlineTime)));
    }

    @Override
    public Mono<Long> getOfflineTime() {
        return getSelfConfig("offlineTime")
                .map(val -> val.as(Long.class))
                .switchIfEmpty(Mono.defer(() ->
                        this.getSelfConfig(parentGatewayId)
                                .flatMap(registry::getDevice)
                                .flatMap(DeviceOperator::getOfflineTime)));
    }

    @Override
    public Mono<Boolean> offline() {
        return this
                .setConfigs(
                        selfManageState.value(true),
                        connectionServerId.value(""),
                        sessionId.value(""),
                        ConfigKey.of("offlineTime").value(System.currentTimeMillis()),
                        ConfigKey.of("state").value(DeviceState.offline)
                )
                .doOnError(err -> log.error("offline device error", err));
    }

    @Override
    public Mono<Boolean> online(String serverId, String sessionId, String address) {
        return this
                .setConfigs(
                        selfManageState.value(true),
                        connectionServerId.value(serverId),
                        DeviceConfigKey.sessionId.value(sessionId),
                        ConfigKey.of("address").value(address),
                        ConfigKey.of("onlineTime").value(System.currentTimeMillis()),
                        ConfigKey.of("state").value(DeviceState.online)
                )
                .doOnError(err -> log.error("online device error", err));
    }

    @Override
    public Mono<Value> getSelfConfig(String key) {
        return getConfig(key, false);
    }

    @Override
    public Mono<Values> getSelfConfigs(Collection<String> keys) {
        return getConfigs(keys, false);
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
        return getParent().flatMap(DeviceProductOperator::getMetadata);
//        return Mono.justOrEmpty(metadataCache.get())
//                .switchIfEmpty(getProtocol()
//                        .flatMap(protocol -> getConfig(DeviceConfigKey.metadata)
//                                .flatMap(protocol.getMetadataCodec()::decode)))
//                .switchIfEmpty(getParent().flatMap(DeviceProductOperator::getMetadata))
//                .doOnNext(metadataCache::set);
    }


    @Override
    public Mono<DeviceProductOperator> getParent() {
        return getReactiveStorage()
                .flatMap(store -> store.getConfig(productId.getKey()))
                .map(Value::asString)
                .flatMap(registry::getProduct);
    }

    @Override
    public Mono<ProtocolSupport> getProtocol() {
        return getConfig(protocol)
                .flatMap(supports::getProtocol)
                .switchIfEmpty(getParent().flatMap(DeviceProductOperator::getProtocol));
    }

    @Override
    public Mono<DeviceProductOperator> getProduct() {
        return getParent();
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
