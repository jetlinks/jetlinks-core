package org.jetlinks.core;

import org.jetlinks.core.device.*;
import org.jetlinks.core.message.codec.DeviceMessageCodec;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.message.interceptor.DeviceMessageSenderInterceptor;
import org.jetlinks.core.metadata.*;
import org.jetlinks.core.server.ClientConnection;
import org.jetlinks.core.server.DeviceGatewayContext;
import org.springframework.core.Ordered;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * 消息协议支持接口，通过实现此接口来自定义消息协议
 *
 * @author zhouhao
 * @since 1.0.0
 */
public interface ProtocolSupport extends Disposable, Ordered, Comparable<ProtocolSupport> {
    /**
     * @return 协议ID
     */
    @Nonnull
    String getId();

    /**
     * @return 协议名称
     */
    String getName();

    /**
     * @return 说明
     */
    String getDescription();

    /**
     * @return 获取支持的协议类型
     */
    Flux<? extends Transport> getSupportedTransport();

    /**
     * 获取设备消息编码解码器
     * <ul>
     * <li>用于将平台统一的消息对象转码为设备的消息</li>
     * <li>用于将设备发送的消息转吗为平台统一的消息对象</li>
     * </ul>
     *
     * @return 消息编解码器
     */
    @Nonnull
    Mono<? extends DeviceMessageCodec> getMessageCodec(Transport transport);

    /**
     * 获取设备消息发送拦截器, 用于拦截发送消息的行为.
     *
     * @return 监听器
     */
    default Mono<DeviceMessageSenderInterceptor> getSenderInterceptor() {
        return Mono.just(DeviceMessageSenderInterceptor.DO_NOTING);
    }

    /**
     * 获取默认的设备物模型编解码器
     * <ul>
     * <li>用于将平台统一的设备定义规范转码为协议的规范</li>
     * <li>用于将协议的规范转为平台统一的设备定义规范</li>
     *
     * </ul>
     *
     * @return 物模型编解码器
     */
    @Nonnull
    DeviceMetadataCodec getMetadataCodec();

    /**
     * 获取所有支持的物模型编解码器
     *
     * @return 物模型
     * @since 1.1.4
     */
    default Flux<DeviceMetadataCodec> getMetadataCodecs() {
        return Flux.just(getMetadataCodec());
    }

    /**
     * 进行设备认证
     *
     * @param request         认证请求，不同的连接方式实现不同
     * @param deviceOperation 设备操作接口,可用于配置设备
     * @return 认证结果
     * @see MqttAuthenticationRequest
     */
    @Nonnull
    Mono<AuthenticationResponse> authenticate(
            @Nonnull AuthenticationRequest request,
            @Nonnull DeviceOperator deviceOperation);

    /**
     * 对不明确的设备进行认证
     *
     * @param request  认证请求
     * @param registry 注册中心
     * @return 认证结果
     */
    @Nonnull
    default Mono<AuthenticationResponse> authenticate(
            @Nonnull AuthenticationRequest request,
            @Nonnull DeviceRegistry registry) {
        return Mono.error(new UnsupportedOperationException());
    }

    /**
     * 获取自定义设备状态检查器,用于检查设备状态.
     *
     * @return 设备状态检查器
     */
    @Nonnull
    default Mono<DeviceStateChecker> getStateChecker() {
        return Mono.empty();
    }

    /**
     * 获取协议所需的配置信息定义
     *
     * @return 配置定义
     * @see DeviceOperator#getConfigs(String...)
     * @see DeviceOperator#setConfigs(Map)
     */
    default Mono<ConfigMetadata> getConfigMetadata(Transport transport) {
        return Mono.empty();
    }

    /**
     * 获取协议初始化所需要的配置定义
     *
     * @return 配置定义
     * @since 1.1.2
     */
    default Mono<ConfigMetadata> getInitConfigMetadata() {
        return Mono.empty();
    }

    /**
     * 初始化协议
     *
     * @param configuration 配置信息
     */
    default void init(Map<String, Object> configuration) {

    }

    /**
     * 销毁协议
     */
    @Override
    default void dispose() {
    }

    /**
     * 获取默认物模型
     *
     * @param transport 传输协议
     * @return 物模型信息
     * @since 1.1.4
     */
    default Mono<DeviceMetadata> getDefaultMetadata(Transport transport) {
        return Mono.empty();
    }

    /**
     * 获取物模型拓展配置定义
     *
     * @param transport    传输协议类型
     * @param metadataType 物模型类型
     * @param dataTypeId   数据类型ID {@link DataType#getId()}
     * @param metadataId   物模型标识
     * @return 配置定义
     * @since 1.1.4
     */
    default Flux<ConfigMetadata> getMetadataExpandsConfig(Transport transport,
                                                          DeviceMetadataType metadataType,
                                                          String metadataId,
                                                          String dataTypeId) {
        return Flux.empty();
    }

    /**
     * 当设备注册生效后调用
     *
     * @param operator 设备操作接口
     * @return void
     * @since 1.1.5
     */
    default Mono<Void> onDeviceRegister(DeviceOperator operator) {
        return Mono.empty();
    }

    /**
     * 当设备注销前调用
     *
     * @param operator 设备操作接口
     * @return void
     * @since 1.1.5
     */
    default Mono<Void> onDeviceUnRegister(DeviceOperator operator) {
        return Mono.empty();
    }

    /**
     * 当产品注册后调用
     *
     * @param operator 产品操作接口
     * @return void
     * @since 1.1.5
     */
    default Mono<Void> onProductRegister(DeviceProductOperator operator) {
        return Mono.empty();
    }

    /**
     * 当产品注销前调用
     *
     * @param operator 产品操作接口
     * @return void
     * @since 1.1.5
     */
    default Mono<Void> onProductUnRegister(DeviceProductOperator operator) {
        return Mono.empty();
    }

    /**
     * 当产品物模型变更时调用
     *
     * @param operator 产品操作接口
     * @return void
     * @since 1.1.6
     */
    default Mono<Void> onProductMetadataChanged(DeviceProductOperator operator) {
        return Mono.empty();
    }

    /**
     * 当设备物模型变更时调用
     *
     * @param operator 设备操作接口
     * @return void
     * @since 1.1.6
     */
    default Mono<Void> onDeviceMetadataChanged(DeviceOperator operator) {
        return Mono.empty();
    }

    /**
     * 客户端创建连接时调用,返回设备ID,表示此设备上线.
     *
     * @param transport  传输协议
     * @param connection 客户端连接
     * @return void
     * @since 1.1.6
     */
    default Mono<Void> onClientConnect(Transport transport,
                                       ClientConnection connection,
                                       DeviceGatewayContext context) {
        return Mono.empty();
    }

    /**
     * 触发手动绑定子设备到网关设备
     *
     * @param gateway 网关
     * @param child   子设备流
     * @return void
     * @since 1.1.6
     */
    default Mono<Void> onChildBind(DeviceOperator gateway, Flux<DeviceOperator> child) {
        return Mono.empty();
    }

    /**
     * 触发手动接触绑定子设备到网关设备
     *
     * @param gateway 网关
     * @param child   子设备流
     * @return void
     * @since 1.1.6
     */
    default Mono<Void> onChildUnbind(DeviceOperator gateway, Flux<DeviceOperator> child) {
        return Mono.empty();
    }

    /**
     * 获取协议支持的某些自定义特性
     *
     * @return 特性集
     * @since 1.1.6
     */
    default Flux<Feature> getFeatures(Transport transport) {
        return Flux.empty();
    }

    /**
     * 在执行设备创建之前,执行指定的操作。通常用于自定义默认配置生成等操作
     *
     * @param deviceInfo 设备信息
     * @return 新等设备信息
     */
    default Mono<DeviceInfo> doBeforeDeviceCreate(Transport transport,
                                                  DeviceInfo deviceInfo) {
        return Mono.just(deviceInfo);
    }

    @Override
    default int getOrder() {
        return Integer.MAX_VALUE;
    }

    @Override
    default int compareTo(@Nonnull ProtocolSupport o) {
        return Integer.compare(this.getOrder(), o.getOrder());
    }
}
