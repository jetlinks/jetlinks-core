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
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.message.*;
import org.jetlinks.core.message.interceptor.DeviceMessageSenderInterceptor;
import org.jetlinks.core.message.state.DeviceStateCheckMessage;
import org.jetlinks.core.message.state.DeviceStateCheckMessageReply;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.utils.IdUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static org.jetlinks.core.device.DeviceConfigKey.*;

@Slf4j
public class DefaultDeviceOperator implements DeviceOperator, StorageConfigurable {
    public static final DeviceStateChecker DEFAULT_STATE_CHECKER = device -> checkState0(((DefaultDeviceOperator) device));

    private static final ConfigKey<Long> lastMetadataTimeKey = ConfigKey.of("lst_metadata_time");

    private static final AtomicReferenceFieldUpdater<DefaultDeviceOperator, DeviceMetadata> METADATA_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(DefaultDeviceOperator.class, DeviceMetadata.class, "metadataCache");
    private static final AtomicLongFieldUpdater<DefaultDeviceOperator> METADATA_TIME_UPDATER =
            AtomicLongFieldUpdater.newUpdater(DefaultDeviceOperator.class, "lastMetadataTime");

    private final String id;

    private final DeviceOperationBroker handler;

    private final DeviceRegistry registry;

    private final DeviceMessageSender messageSender;

    private final Mono<ConfigStorage> storageMono;

    private final Mono<ProtocolSupport> protocolSupportMono;

    private final Mono<DeviceMetadata> metadataMono;

    private final DeviceStateChecker stateChecker;

    private volatile long lastMetadataTime = -1;

    private volatile DeviceMetadata metadataCache;

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
        this(id, supports, storageManager, handler, registry, interceptor, DEFAULT_STATE_CHECKER);
    }

    public DefaultDeviceOperator(String id,
                                 ProtocolSupports supports,
                                 ConfigStorageManager storageManager,
                                 DeviceOperationBroker handler,
                                 DeviceRegistry registry,
                                 DeviceMessageSenderInterceptor interceptor,
                                 DeviceStateChecker deviceStateChecker) {
        this.id = id;
        this.registry = registry;
        this.handler = handler;
        this.messageSender = new DefaultDeviceMessageSender(handler, this, registry, interceptor);
        this.storageMono = storageManager.getStorage("device:" + id);

//        this.metadataMono = getParent().flatMap(DeviceProductOperator::getMetadata);
        this.protocolSupportMono = getProduct().flatMap(DeviceProductOperator::getProtocol);
        this.stateChecker = deviceStateChecker;
        this.metadataMono = this
                //获取最后更新物模型的时间
                .getSelfConfig(lastMetadataTimeKey)
                .flatMap(i -> {
                    //如果有时间,则表示设备有独立的物模型.
                    //如果时间一致,则直接返回物模型缓存.
                    if (i.equals(lastMetadataTime) && metadataCache != null) {
                        return Mono.just(metadataCache);
                    }
                    METADATA_TIME_UPDATER.set(this, i);
                    //加载真实的物模型
                    return Mono
                            .zip(getSelfConfig(metadata),
                                 protocolSupportMono)
                            .flatMap(tp2 -> tp2
                                    .getT2()
                                    .getMetadataCodec()
                                    .decode(tp2.getT1())
                                    .doOnNext(metadata -> METADATA_UPDATER.set(this, metadata)));

                })
                //如果上游为空,则使用产品的物模型
                .switchIfEmpty(this.getParent()
                                   .flatMap(DeviceProductOperator::getMetadata));

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
        return this
                .getSelfConfigs(Arrays.asList("state", parentGatewayId.getKey(), selfManageState.getKey()))
                .flatMap(values -> {
                    Byte state = values
                            .getValue("state")
                            .map(val -> val.as(Byte.class))
                            .orElse(DeviceState.unknown);

                    boolean isSelfManageState = values
                            .getValue(selfManageState.getKey())
                            .map(val -> val.as(Boolean.class))
                            .orElse(false);
                    String parentGatewayId = values
                            .getValue(DeviceConfigKey.parentGatewayId)
                            .orElse(null);

                    if (getDeviceId().equals(parentGatewayId)) {
                        log.warn("设备[{}]存在循环依赖", parentGatewayId);
                        return Mono.just(state);
                    }
                    if (isSelfManageState) {
                        return Mono.just(state);
                    }
                    //获取网关设备状态
                    if (StringUtils.hasText(parentGatewayId)) {
                        return registry
                                .getDevice(parentGatewayId)
                                .flatMap(DeviceOperator::getState);
                    }
                    return Mono.just(state);
                })
                .defaultIfEmpty(DeviceState.unknown);
    }

    private Mono<Byte> doCheckState() {
        return Mono
                .defer(() -> this
                        .getSelfConfigs(Arrays.asList(
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

                            //网关设备ID
                            String parentGatewayId = values
                                    .getValue(DeviceConfigKey.parentGatewayId)
                                    .orElse(null);

                            if (getDeviceId().equals(parentGatewayId)) {
                                log.warn("设备[{}]存在循环依赖", parentGatewayId);
                                return Mono.just(state);
                            }
                            boolean isSelfManageState = values.getValue(selfManageState).orElse(false);
                            //如果关联了上级网关设备则获取父设备状态
                            if (StringUtils.hasText(parentGatewayId)) {
                                return registry
                                        .getDevice(parentGatewayId)
                                        .flatMap(device -> {
                                            //不是状态自管理则直接返回网关的状态
                                            if (!isSelfManageState) {
                                                return device.checkState();
                                            }
                                            //发送设备状态检查指令给网关设备
                                            return device
                                                    .messageSender()
                                                    .<ChildDeviceMessageReply>
                                                            send(ChildDeviceMessage
                                                                         .create(parentGatewayId,
                                                                                 DeviceStateCheckMessage.create(getDeviceId())
                                                                         )
                                                                         .addHeader(Headers.timeout, 5000L)
                                                    )
                                                    .singleOrEmpty()
                                                    .map(msg -> {
                                                        if (msg.getChildDeviceMessage() instanceof DeviceStateCheckMessageReply) {
                                                            return ((DeviceStateCheckMessageReply) msg.getChildDeviceMessage())
                                                                    .getState();
                                                        }
                                                        log.warn("子设备状态检查返回消息错误{}", msg);
                                                        //网关设备在线,只是返回了错误的消息,所以也认为网关设备在线
                                                        return DeviceState.online;
                                                    })
                                                    .onErrorResume(err -> {
                                                        if (err instanceof DeviceOperationException) {
                                                            ErrorCode code = ((DeviceOperationException) err).getCode();
                                                            if (code == ErrorCode.CLIENT_OFFLINE) {
                                                                //父设备已经离线了
                                                                return Mono.just(DeviceState.offline);
                                                            } else if (code == ErrorCode.UNSUPPORTED_MESSAGE) {
                                                                //不支持的消息，则认为父设备是在线的，只是协议包不支持处理。
                                                                return Mono.just(DeviceState.online);
                                                            }
                                                        }
                                                        //发送返回错误,但是配置了状态自管理,直接返回原始状态
                                                        return Mono.just(state);
                                                    });
                                        })
                                        //没有父设备?则认为离线
                                        .defaultIfEmpty(state.equals(DeviceState.online) ? DeviceState.offline : state);
                            }

                            //如果是在线状态,则改为离线,否则保持状态不变
                            if (state.equals(DeviceState.online)) {
                                return Mono.just(DeviceState.offline);
                            } else {
                                return Mono.just(state);
                            }
                        }));
    }

    @Override
    public Mono<Byte> checkState() {
        return Mono
                .zip(
                        stateChecker
                                .checkState(this)
                                .switchIfEmpty(Mono.defer(() -> DEFAULT_STATE_CHECKER.checkState(this)))
                                .defaultIfEmpty(DeviceState.online),
                        this.getState()
                )
                .flatMap(tp2 -> {
                    byte newer = tp2.getT1();
                    byte old = tp2.getT2();
                    //状态不一致?
                    if (newer != old) {
                        log.info("device[{}] state changed from {} to {}", this.getDeviceId(), old, newer);
                        Map<String, Object> configs = new HashMap<>();
                        configs.put("state", newer);
                        if (newer == DeviceState.online) {
                            configs.put("onlineTime", System.currentTimeMillis());
                        } else if (newer == DeviceState.offline) {
                            configs.put("offlineTime", System.currentTimeMillis());
                        }
                        return this
                                .setConfigs(configs)
                                .thenReturn(newer);
                    }
                    return Mono.just(newer);
                });
    }

    @Override
    public Mono<Long> getOnlineTime() {
        return this
                .getSelfConfig("onlineTime")
                .map(val -> val.as(Long.class))
                .switchIfEmpty(Mono.defer(() -> this
                        .getSelfConfig(parentGatewayId)
                        .flatMap(registry::getDevice)
                        .flatMap(DeviceOperator::getOnlineTime)));
    }

    @Override
    public Mono<Long> getOfflineTime() {
        return this
                .getSelfConfig("offlineTime")
                .map(val -> val.as(Long.class))
                .switchIfEmpty(Mono.defer(() -> this
                        .getSelfConfig(parentGatewayId)
                        .flatMap(registry::getDevice)
                        .flatMap(DeviceOperator::getOfflineTime)));
    }

    @Override
    public Mono<Boolean> offline() {
        return this
                .setConfigs(
                        //selfManageState.value(true),
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
                        //  selfManageState.value(true),
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

    @Override
    public Mono<DeviceMetadata> getMetadata() {
        return metadataMono;
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
        return protocolSupportMono;
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
        Map<String, Object> configs = new HashMap<>();
        configs.put(DeviceConfigKey.metadata.getKey(), metadata);
        return setConfigs(configs);
    }

    @Override
    public Mono<Void> resetMetadata() {
        METADATA_UPDATER.set(this, null);
        METADATA_TIME_UPDATER.set(this, -1);
        return removeConfigs(metadata, lastMetadataTimeKey)
                .then(this.getProtocol()
                          .flatMap(support -> support.onDeviceMetadataChanged(this))
                );
    }

    @Override
    public Mono<Boolean> setConfigs(Map<String, Object> conf) {
        Map<String, Object> configs = new HashMap<>(conf);
        if (conf.containsKey(metadata.getKey())) {
            configs.put(lastMetadataTimeKey.getKey(), lastMetadataTime = System.currentTimeMillis());

            return StorageConfigurable.super
                    .setConfigs(configs)
                    .doOnNext(suc -> {
                        this.metadataCache = null;
                    })
                    .then(this.getProtocol()
                              .flatMap(support -> support.onDeviceMetadataChanged(this))
                    )
                    .thenReturn(true);
        }
        return StorageConfigurable.super.setConfigs(configs);
    }

    private static Mono<Byte> checkState0(DefaultDeviceOperator operator) {
        return operator
                .getProtocol()
                .flatMap(ProtocolSupport::getStateChecker) //协议自定义了状态检查逻辑
                .flatMap(deviceStateChecker -> deviceStateChecker.checkState(operator))
                .switchIfEmpty(operator.doCheckState()) //默认的检查
                ;
    }
}
