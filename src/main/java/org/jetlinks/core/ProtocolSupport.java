package org.jetlinks.core;

import org.jetlinks.core.device.*;
import org.jetlinks.core.message.codec.DeviceMessageCodec;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.message.interceptor.DeviceMessageSenderInterceptor;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DeviceMetadataCodec;
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
public interface ProtocolSupport extends Disposable {
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
     * 获取设备元数据编解码器
     * <ul>
     * <li>用于将平台统一的设备定义规范转码为协议的规范</li>
     * <li>用于将协议的规范转为平台统一的设备定义规范</li>
     * *
     * </ul>
     *
     * @return 元数据编解码器
     */
    @Nonnull
    DeviceMetadataCodec getMetadataCodec();

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

}
