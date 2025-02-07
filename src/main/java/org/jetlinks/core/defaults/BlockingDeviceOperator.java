package org.jetlinks.core.defaults;

import lombok.AllArgsConstructor;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.Value;
import org.jetlinks.core.Values;
import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.core.config.ConfigKeyValue;
import org.jetlinks.core.device.*;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.things.ThingMetadata;
import org.jetlinks.core.things.ThingRpcSupport;
import org.jetlinks.core.things.ThingType;
import org.jetlinks.core.utils.Reactors;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@AllArgsConstructor
public class BlockingDeviceOperator implements DeviceOperator {

    private final DeviceOperator target;
    private final Duration timeout;
    private final ContextView context;

    @Override
    public String getId() {
        return target.getId();
    }

    @Override
    public ThingType getType() {
        return target.getType();
    }

    @Override
    public String getDeviceId() {
        return target.getDeviceId();
    }

    private <T> T await(Mono<T> task) {
        return Reactors.await(task.contextWrite(context), timeout);
    }

    @Nullable
    public String getConnectionServerIdNow() {
        return await(getConnectionServerId());
    }

    @Override
    public Mono<String> getConnectionServerId() {
        return target.getConnectionServerId();
    }

    @Nullable
    public String getSessionIdNow() {
        return await(getConnectionServerId());
    }

    @Override
    public Mono<String> getSessionId() {
        return target.getSessionId();
    }

    @Nullable
    public String getAddressNow() {
        return await(getConnectionServerId());
    }

    @Override
    public Mono<String> getAddress() {
        return target.getAddress();
    }

    @Override
    public Mono<Void> setAddress(String address) {
        return target.setAddress(address);
    }

    @Override
    public Mono<Boolean> putState(byte state) {
        return target.putState(state);
    }

    public Byte getStateNow() {
        return await(getState());
    }

    @Override
    public Mono<Byte> getState() {
        return target.getState();
    }

    public Byte checkStateNow() {
        return await(checkState());
    }

    @Override
    public Mono<Byte> checkState() {
        return target.checkState();
    }

    public Long getOnlineTimeNow() {
        return await(target.getOnlineTime());
    }

    @Override
    public Mono<Long> getOnlineTime() {
        return target.getOnlineTime();
    }

    public Long getOfflineTimeNow() {
        return await(target.getOnlineTime());
    }

    @Override
    public Mono<Long> getOfflineTime() {
        return target.getOfflineTime();
    }

    @Override
    public Mono<Boolean> online(String serverId, String sessionId) {
        return target.online(serverId, sessionId);
    }

    @Override
    public Mono<Boolean> online(String serverId, String sessionId, String address) {
        return target.online(serverId, sessionId, address);
    }

    @Override
    public Mono<Boolean> online(String serverId, String address, long onlineTime) {
        return target.online(serverId, address, onlineTime);
    }

    /**
     * 获取设备自身的配置,当配置不存在返回<code>null</code>.
     * <p>
     * 注意: 请谨慎在响应式上下文中使用此方法. 可能导致性能问题.
     *
     * @param key 配置key
     * @return 配置值
     */
    @Nullable
    public Value getSelfConfigNow(String key) {
        return await(getSelfConfig(key));
    }

    @Override
    public Mono<Value> getSelfConfig(String key) {
        return target.getSelfConfig(key);
    }

    /**
     * 获取设备自身的多个配置,可返回值{@link Values#getValue(String)}获取具体的值.
     * <p>
     * 注意: 请谨慎在响应式上下文中使用此方法. 可能导致性能问题.
     *
     * @param keys key列表.
     * @return 配置值
     */
    public Values getSelfConfigsNow(Collection<String> keys) {
        return await(getSelfConfigs(keys));
    }

    @Override
    public Mono<Values> getSelfConfigs(Collection<String> keys) {
        return target.getSelfConfigs(keys);
    }

    /**
     * 获取设备自身的多个配置,可返回值{@link Values#getValue(String)}获取具体的值.
     * <p>
     * 注意: 请谨慎在响应式上下文中使用此方法. 可能导致性能问题.
     *
     * @param keys key列表.
     * @return 配置值
     */
    public Values getSelfConfigsNow(String... keys) {
        return await(getSelfConfigs(keys));
    }

    @Override
    public Mono<Values> getSelfConfigs(String... keys) {
        return target.getSelfConfigs(keys);
    }

    /**
     * 获取设备自身的配置,当配置不存在返回<code>null</code>.
     * <p>
     * 注意: 请谨慎在响应式上下文中使用此方法. 可能导致性能问题.
     *
     * @param key 配置key
     * @return 配置值
     */
    public <V> V getSelfConfigNow(ConfigKey<V> key) {
        return await(getSelfConfig(key));
    }

    @Override
    public <V> Mono<V> getSelfConfig(ConfigKey<V> key) {
        return DeviceOperator.super.getSelfConfig(key);
    }

    /**
     * 获取设备自身的多个配置,可返回值{@link Values#getValue(String)}获取具体的值.
     * <p>
     * 注意: 请谨慎在响应式上下文中使用此方法. 可能导致性能问题.
     *
     * @param keys key列表.
     * @return 配置值
     */
    public Values getSelfConfigsNow(ConfigKey<?>... keys) {
        return await(getSelfConfigs(keys));
    }

    @Override
    public Mono<Values> getSelfConfigs(ConfigKey<?>... keys) {
        return DeviceOperator.super.getSelfConfigs(keys);
    }

    @Override
    public ThingRpcSupport rpc() {
        return target.rpc();
    }

    public boolean isOnlineNow() {
        return Boolean.TRUE.equals(
            await(target.isOnline())
        );
    }

    @Override
    public Mono<Boolean> isOnline() {
        return target.isOnline();
    }

    public boolean offlineNow() {
        return Boolean.TRUE
            .equals(
                await(target.offline())
            );
    }

    @Override
    public Mono<Boolean> offline() {
        return target.offline();
    }

    public boolean disconnectNow() {
        return Boolean.TRUE.equals(
            await(target.disconnect())
        );
    }

    @Override
    public Mono<Boolean> disconnect() {
        return target.disconnect();
    }


    @Override
    public Mono<AuthenticationResponse> authenticate(AuthenticationRequest request) {
        return target.authenticate(request);
    }

    public DeviceMetadata getMetadataNow() {
        return await(getMetadata());
    }

    @Override
    public Mono<DeviceMetadata> getMetadata() {
        return target.getMetadata();
    }

    public ProtocolSupport getProtocolNow() {
        return await(getProtocol());
    }

    @Override
    public Mono<ProtocolSupport> getProtocol() {
        return target.getProtocol();
    }

    @Override
    public DeviceMessageSender messageSender() {
        return target.messageSender();
    }


    public boolean updateMetadataNow(String metadata) {
        return Boolean.TRUE.equals(
            await(updateMetadata(metadata))
        );
    }

    @Override
    public Mono<Boolean> updateMetadata(String metadata) {
        return target.updateMetadata(metadata);
    }

    public boolean updateMetadataNow(ThingMetadata metadata) {
        return Boolean.TRUE.equals(
            await(updateMetadata(metadata))
        );
    }

    @Override
    public Mono<Boolean> updateMetadata(ThingMetadata metadata) {
        return target.updateMetadata(metadata);
    }

    public void resetMetadataNow() {
        await(resetMetadata());
    }

    @Override
    public Mono<Void> resetMetadata() {
        return target.resetMetadata();
    }

    public DeviceProductOperator getProductNow() {
        return await(getProduct());
    }

    @Override
    public Mono<DeviceProductOperator> getProduct() {
        return target.getProduct();
    }

    @Override
    public Mono<DeviceProductOperator> getTemplate() {
        return target.getTemplate();
    }

    /**
     * 获取设备的配置,当设备自身没有进行配置时,尝试获取产品的配置,产品没有时,返回<code>null</code>.
     * <p>
     * 注意: 请谨慎在响应式上下文中使用此方法. 可能导致性能问题.
     *
     * @param key 配置key
     * @return 配置值
     */
    @Nullable
    public Value getConfigNow(String key) {
        return await(getConfig(key));
    }

    @Override
    public Mono<Value> getConfig(String key) {
        return target.getConfig(key);
    }

    private Values safeValues(Values values) {
        return values == null ? Values.of(Collections.emptyMap()) : values;
    }

    /**
     * 获取设备自身的多个配置,可返回值{@link Values#getValue(String)}获取具体的值.
     * <p>
     * 注意: 请谨慎在响应式上下文中使用此方法. 可能导致性能问题.
     *
     * @param keys key列表.
     * @return 配置值
     */
    public Values getConfigsNow(Collection<String> keys) {
        return safeValues(
            await(getConfigs(keys))
        );
    }

    @Override
    public Mono<Values> getConfigs(Collection<String> keys) {
        return target.getConfigs(keys);
    }

    public boolean setConfigNow(String key, Object value) {
        return Boolean.TRUE.equals(
            await(setConfig(key, value))
        );
    }

    @Override
    public Mono<Boolean> setConfig(String key, Object value) {
        return target.setConfig(key, value);
    }


    public boolean setConfigNow(ConfigKeyValue<?> keyValue) {
        return Boolean.TRUE.equals(
            await(setConfig(keyValue))
        );
    }

    @Override
    public Mono<Boolean> setConfig(ConfigKeyValue<?> keyValue) {
        return target.setConfig(keyValue);
    }

    public <T> boolean setConfigNow(ConfigKey<T> key, T value) {
        return Boolean.TRUE.equals(
            await(setConfig(key, value))
        );
    }

    @Override
    public <T> Mono<Boolean> setConfig(ConfigKey<T> key, T value) {
        return target.setConfig(key, value);
    }

    public <T> boolean setConfigsNow(ConfigKeyValue<?>... keyValues) {
        return Boolean.TRUE.equals(
            await(setConfigs(keyValues))
        );
    }

    @Override
    public Mono<Boolean> setConfigs(ConfigKeyValue<?>... keyValues) {
        return target.setConfigs(keyValues);
    }

    /**
     * 获取设备的配置,当设备自身没有进行配置时,尝试获取产品的配置,产品没有时,返回<code>null</code>.
     * <p>
     * 注意: 请谨慎在响应式上下文中使用此方法. 可能导致性能问题.
     *
     * @param key 配置key
     * @return 配置值
     */
    @Nullable
    public <V> V getConfigNow(ConfigKey<V> key) {
        return await(getConfig(key));
    }

    @Override
    public <V> Mono<V> getConfig(ConfigKey<V> key) {
        return target.getConfig(key);
    }

    /**
     * 获取设备自身的多个配置,可返回值{@link Values#getValue(String)}获取具体的值.
     * <p>
     * 注意: 请谨慎在响应式上下文中使用此方法. 可能导致性能问题.
     *
     * @param key key列表.
     * @return 配置值
     */
    public Values getConfigsNow(ConfigKey<?>... key) {
        return safeValues(
            await(getConfigs(key))
        );
    }

    @Override
    public Mono<Values> getConfigs(ConfigKey<?>... key) {
        return target.getConfigs(key);
    }

    /**
     * 获取设备自身的多个配置,可返回值{@link Values#getValue(String)}获取具体的值.
     * <p>
     * 注意: 请谨慎在响应式上下文中使用此方法. 可能导致性能问题.
     *
     * @param keys key列表.
     * @return 配置值
     */
    public Values getConfigsNow(String... keys) {
        return safeValues(
            await(getConfigs(keys))
        );
    }

    @Override
    public Mono<Values> getConfigs(String... keys) {
        return target.getConfigs(keys);
    }

    public boolean setConfigsNow(Map<String, Object> conf) {
        return Boolean.TRUE.equals(
            await(setConfigs(conf))
        );
    }

    @Override
    public Mono<Boolean> setConfigs(Map<String, Object> conf) {
        return target.setConfigs(conf);
    }


    public boolean removeConfigNow(String key) {
        return Boolean.TRUE.equals(
            await(removeConfig(key))
        );
    }

    @Override
    public Mono<Boolean> removeConfig(String key) {
        return target.removeConfig(key);
    }

    public Value getAndRemoveConfigNow(String key) {
        return await(getAndRemoveConfig(key));
    }

    @Override
    public Mono<Value> getAndRemoveConfig(String key) {
        return target.getAndRemoveConfig(key);
    }

    public boolean removeConfigsNow(Collection<String> key) {
        return Boolean.TRUE.equals(
            await(removeConfigs(key))
        );
    }

    @Override
    public Mono<Boolean> removeConfigs(Collection<String> key) {
        return target.removeConfigs(key);
    }

    @Override
    public Mono<Void> refreshConfig(Collection<String> keys) {
        return target.refreshConfig(keys);
    }

    @Override
    public Mono<Void> refreshAllConfig() {
        return target.refreshAllConfig();
    }

    @Override
    public Mono<Boolean> removeConfigs(ConfigKey<?>... key) {
        return target.removeConfigs(key);
    }

    @Override
    public boolean isWrapperFor(Class<?> type) {
        return target.isWrapperFor(type);
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        return target.unwrap(type);
    }
}
