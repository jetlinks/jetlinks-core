package org.jetlinks.core.defaults;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.i18n.LocaleUtils;
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
import org.jetlinks.core.exception.ProductNotActivatedException;
import org.jetlinks.core.message.*;
import org.jetlinks.core.message.interceptor.DeviceMessageSenderInterceptor;
import org.jetlinks.core.message.state.DeviceStateCheckMessage;
import org.jetlinks.core.message.state.DeviceStateCheckMessageReply;
import org.jetlinks.core.metadata.CompositeDeviceMetadata;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.metadata.SimpleDeviceMetadata;
import org.jetlinks.core.things.ThingMetadata;
import org.jetlinks.core.things.ThingRpcSupport;
import org.jetlinks.core.things.ThingRpcSupportChain;
import org.jetlinks.core.utils.IdUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static org.jetlinks.core.device.DeviceConfigKey.*;

@Slf4j
public class DefaultDeviceOperator implements DeviceOperator, StorageConfigurable {
    public static final DeviceStateChecker DEFAULT_STATE_CHECKER = device -> checkState0(((DefaultDeviceOperator) device));

    private static final ConfigKey<Long> lastMetadataTimeKey = ConfigKey.of("lst_metadata_time", "最后物模型更新时间", Long.class);
    private static final ConfigKey<Byte> stateKey = ConfigKey.of("state", "状态", Byte.class);
    private static final ConfigKey<Long> onlineTimeKey = ConfigKey.of("onlineTime", "上线时间", Long.class);
    private static final ConfigKey<Long> offlineTimeKey = ConfigKey.of("offlineTime", "离线时间", Long.class);

    static final List<String> productIdAndVersionKey = Arrays.asList(productId.getKey(), productVersion.getKey());

    private static final AtomicReferenceFieldUpdater<DefaultDeviceOperator, DeviceMetadata> METADATA_UPDATER =
        AtomicReferenceFieldUpdater.newUpdater(DefaultDeviceOperator.class, DeviceMetadata.class, "metadataCache");
    private static final AtomicLongFieldUpdater<DefaultDeviceOperator> METADATA_TIME_UPDATER =
        AtomicLongFieldUpdater.newUpdater(DefaultDeviceOperator.class, "lastMetadataTime");

    private static final DeviceMetadata NON_METADATA = new SimpleDeviceMetadata();

    @Getter
    private final String id;

    private final DeviceOperationBroker handler;

    private final DeviceRegistry registry;

    private final DeviceMessageSender messageSender;

    private final Mono<ConfigStorage> storageMono;

    private final Mono<ProtocolSupport> protocolSupportMono;

    private final Mono<DeviceMetadata> metadataMono;

    private final DeviceStateChecker stateChecker;

    private final Mono<DeviceProductOperator> parent;

    private volatile long lastMetadataTime = -1;

    private volatile DeviceMetadata metadataCache;

    @Setter
    private ThingRpcSupportChain rpcChain;


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
        this.parent = getReactiveStorage()
            .flatMap(store -> store.getConfigs(productIdAndVersionKey))
            .flatMap(productIdAndVersion -> {
                //支持指定产品版本
                String _productId = productIdAndVersion.getString(productId.getKey(), (String) null);
                String _version = productIdAndVersion.getString(productVersion.getKey(), (String) null);
                return registry.getProduct(_productId, _version);
            });
        //支持设备自定义协议
        this.protocolSupportMono = this
            .getSelfConfig(protocol)
            .flatMap(supports::getProtocol)
            .switchIfEmpty(this.parent.flatMap(DeviceProductOperator::getProtocol));

        this.stateChecker = deviceStateChecker;

        this.metadataMono = Mono
            .zip(productMetadata(),
                 selfMetadata().defaultIfEmpty(NON_METADATA),
                 (product, self) -> {
                     if (self == NON_METADATA) {
                         return product;
                     }
                     //组合产品和设备的物模型
                     return new CompositeDeviceMetadata(product, self);
                 });
    }

    private Mono<DeviceMetadata> selfMetadata() {
        return this
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

            });
    }


    private Mono<DeviceMetadata> productMetadata() {
        return this
            .getParent()
            .switchIfEmpty(Mono.defer(this::onProductNonexistent))
            .flatMap(DeviceProductOperator::getMetadata);
    }

    private Mono<DeviceProductOperator> onProductNonexistent() {
        return getReactiveStorage()
            .flatMap(store -> store.getConfig(productId.getKey()))
            .map(Value::asString)
            .flatMap(productId -> Mono.error(new ProductNotActivatedException(productId)));
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
        return getSelfConfig("address")
            .map(Value::asString);
    }

    @Override
    public Mono<Void> setAddress(String address) {
        return setConfig("address", address).then();
    }

    @Override
    public Mono<Boolean> putState(byte state) {
        return setConfig("state", state);
    }

    private final static List<String> stateCacheKeys = Arrays
        .asList(stateKey.getKey(),
                parentGatewayId.getKey(),
                selfManageState.getKey(),
                connectionServerId.getKey());

    @Override
    public Mono<Byte> getState() {
        return this
            .getSelfConfig(stateKey)
            .defaultIfEmpty(DeviceState.unknown);
//        return this
//                .getSelfConfigs(stateCacheKeys)
//                .flatMap(values -> {
//                    //缓存中的状态
//                    Byte state = values
//                            .getValue("state")
//                            .map(val -> val.as(Byte.class))
//                            .orElse(DeviceState.unknown);
//                    //是否为状态自管理,通常是子设备设置此配置
//                    boolean isSelfManageState = values
//                            .getValue(selfManageState)
//                            .orElse(false);
//
//                    String server = values
//                            .getValue(connectionServerId)
//                            .orElse(null);
//
//                    //已经连接到服务器,则直接返回状态
//                    if (StringUtils.hasText(server)) {
//                        return Mono.just(state);
//                    }
//                    //网关ID
//                    String parentGatewayId = values
//                            .getValue(DeviceConfigKey.parentGatewayId)
//                            .orElse(null);
//                    //存在循环依赖时直接返回
//                    if (getDeviceId().equals(parentGatewayId)) {
//                        log.warn(LocaleUtils.resolveMessage("validation.parent_id_and_id_can_not_be_same", parentGatewayId));
//                        return Mono.just(state);
//                    }
//                    //如果是自状态管理则返回缓存中的状态?
//                    if (isSelfManageState) {
//                        return Mono.just(state);
//                    }
//                    //获取网关设备状态
//                    if (StringUtils.hasText(parentGatewayId)) {
//                        return registry
//                                .getDevice(parentGatewayId)
//                                .flatMap(DeviceOperator::getState);
//                    }
//                    return Mono.just(state);
//                })
//                .defaultIfEmpty(DeviceState.unknown);
    }

    private Mono<Byte> doCheckState() {
        return Mono
            .defer(() -> this
                .getSelfConfigs(stateCacheKeys)
                .flatMap(values -> {

                    //当前设备连接到的服务器
                    String server = values
                        .getValue(connectionServerId)
                        .orElse(null);

                    //设备缓存的状态
                    Byte state = values
                        .getValue(stateKey)
                        .orElse(DeviceState.unknown);

                    Mono<Byte> checker = handler
                        .getDeviceState(server, Collections.singletonList(id))
                        .map(DeviceStateInfo::getState)
                        .singleOrEmpty()
                        .defaultIfEmpty(state);

                    //当前缓存中没有server信息?
                    if (!StringUtils.hasText(server)) {
                        //网关设备ID
                        String parentGatewayId = values
                            .getValue(DeviceConfigKey.parentGatewayId)
                            .orElse(null);

                        if (getDeviceId().equals(parentGatewayId)) {
                            log.warn(LocaleUtils.resolveMessage("validation.parent_id_and_id_can_not_be_same", parentGatewayId));
                            return checker;
                        }
                        boolean isSelfManageState = values.getValue(selfManageState).orElse(false);
                        //如果关联了上级网关设备则尝试给网关设备发送指令进行检查
                        if (StringUtils.hasText(parentGatewayId) && isSelfManageState) {
                            return this
                                .checkStateFromParent(parentGatewayId, checker)
                                .switchIfEmpty(checker);
                        }
                    }

                    return checker;
                }));
    }

    private Mono<Byte> checkStateFromParent(String parentId, Mono<Byte> defaultState) {

        return registry
            .getDevice(parentId)
            .flatMap(device -> {
                //发送设备状态检查指令给网关设备
                return device
                    .messageSender()
                    .<ChildDeviceMessageReply>
                        send(ChildDeviceMessage
                                 .create(parentId,
                                         DeviceStateCheckMessage.create(getDeviceId())
                                 )
                                 .addHeader(Headers.timeout, 5000L))
                    .singleOrEmpty()
                    .map(msg -> {
                        if (msg.getChildDeviceMessage() instanceof DeviceStateCheckMessageReply) {
                            return ((DeviceStateCheckMessageReply) msg.getChildDeviceMessage())
                                .getState();
                        }
                        log.warn("State check return error {}", msg);
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
                        return defaultState;
                    });
            });
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
                //最新的状态与缓存中的状态不一致.
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
            })
            .doOnError(err -> log.warn("check device [{}] state error", getDeviceId(), err));
    }

    @Override
    public Mono<Long> getOnlineTime() {
        return this
            .getSelfConfig(onlineTimeKey)
            .switchIfEmpty(Mono.defer(() -> this
                .getSelfConfig(parentGatewayId)
                .flatMap(registry::getDevice)
                .flatMap(DeviceOperator::getOnlineTime)));
    }

    @Override
    public Mono<Long> getOfflineTime() {
        return this
            .getSelfConfig(offlineTimeKey)
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
                stateKey.value(DeviceState.offline)
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
                stateKey.value(DeviceState.online)
            )
            .doOnError(err -> log.error("online device error", err));
    }

    @Override
    public Mono<Boolean> online(String serverId, String address, long onlineTime) {

        Map<String, Object> configs = Maps.newHashMapWithExpectedSize(4);
        configs.put(connectionServerId.getKey(), serverId);
        configs.put(stateKey.getKey(), DeviceState.online);

        if (null != address) {
            configs.put("address", address);
        }
        if (onlineTime > 0) {
            configs.put("onlineTime", onlineTime);
        }

        return this
            .setConfigs(configs)
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
        return parent;
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
    public Mono<Boolean> updateMetadata(ThingMetadata metadata) {
        if (metadata instanceof DeviceMetadata) {
            return getProtocol()
                .flatMap(protocol -> protocol.getMetadataCodec().encode((DeviceMetadata) metadata))
                .flatMap(this::updateMetadata);
        }
        // FIXME: 2021/11/3
        return Mono.just(false);
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

    @Override
    public ThingRpcSupport rpc() {
        return (msg) -> this
            .getProtocol()
            .flatMapMany(support -> {
                //默认使用sender发送
                ThingRpcSupport temp = (m) -> messageSender.send(convertToDeviceMessage(msg));
                //自定义
                ThingRpcSupportChain chain = support.getRpcChain();
                if (chain != null) {
                    ThingRpcSupport ftemp = temp;
                    temp = m -> chain.call(m, ftemp);
                }

                //全局
                if (rpcChain != null) {
                    ThingRpcSupport ftemp = temp;
                    temp = m -> rpcChain.call(msg, ftemp);
                }
                //执行
                return temp.call(msg);
            });

    }

    private DeviceMessage convertToDeviceMessage(ThingMessage message) {
        if (message instanceof DeviceMessage) {
            return ((DeviceMessage) message);
        }
        //将非DeviceMessage转为DeviceMessage

        JSONObject msg = message.toJson();
        msg.remove("thingId");
        msg.remove("thingType");
        msg.put("deviceId", message.getThingId());
        return MessageType
            .convertMessage(msg)
            .filter(DeviceMessage.class::isInstance)
            .map(DeviceMessage.class::cast)
            .orElseThrow(() -> new UnsupportedOperationException("unsupported message type " + message.getMessageType()));
    }
}
