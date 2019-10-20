package org.jetlinks.core;

import org.jetlinks.core.device.*;
import org.jetlinks.core.message.codec.DeviceMessageCodec;
import org.jetlinks.core.message.codec.MessageDecodeContext;
import org.jetlinks.core.message.codec.MessageEncodeContext;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.metadata.DeviceMetadataCodec;
import org.jetlinks.core.server.GatewayServerContextListener;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * 消息协议支持接口，通过实现此接口来自定义消息协议
 *
 * @author zhouhao
 * @since 1.0.0
 */
public interface ProtocolSupport {
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

    Flux<Transport> getSupportedTransport();

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
    Mono<DeviceMessageCodec> getMessageCodec(Transport transport);

    /**
     * 网关服务上下文监听器
     *
     * @param transport
     * @return
     */
    Mono<GatewayServerContextListener<?>> getServerContextHandler(Transport transport);

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

}
