package org.jetlinks.core.defaults;

import org.jetlinks.core.device.AuthenticationRequest;
import org.jetlinks.core.device.AuthenticationResponse;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

/**
 * 认证器,用于设备连接的时候进行认证
 *
 * @author zhouhao
 * @since 1.0
 */
public interface Authenticator {

    /**
     * 对指定对设备进行认证
     *
     * @param request 认证请求
     * @param device  设备
     * @return 认证结果
     */
    Mono<AuthenticationResponse> authenticate(@Nonnull AuthenticationRequest request,
                                              @Nonnull DeviceOperator device);

    /**
     * 在网络连接建立的时候,可能无法获取设备的标识(如:http,websocket等),则会调用此方法来进行认证.
     * 注意: 认证通过后,需要设置设备ID.{@link AuthenticationResponse#success(String)}
     *
     * @param request  认证请求
     * @param registry 设备注册中心
     * @return 认证结果
     */
    default Mono<AuthenticationResponse> authenticate(@Nonnull AuthenticationRequest request,
                                                      @Nonnull DeviceRegistry registry) {
        return Mono.just(AuthenticationResponse.success());
    }
}
