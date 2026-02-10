package org.jetlinks.core.device;

import org.jetlinks.core.principal.Credential;
import org.jetlinks.core.principal.CredentialType;
import org.jetlinks.core.principal.Identity;
import org.jetlinks.core.principal.Principal;
import reactor.core.publisher.Mono;

/**
 * 设备身份管理器
 *
 * @since 1.3.2
 */
public interface DevicePrincipalManager {

    /**
     * 根据设备身份信息获取设备身份信息.
     * <p>
     * 传入{@link org.jetlinks.core.principal.AuthenticationPrincipal} ,将进行平台内部的认证逻辑.
     * 可根据{@link DevicePrincipal#isVerified()}判断是否认证成功.
     * <p>
     * 否则只会获取设备对应的凭证信息.
     *
     * @return 设备ID
     */
    Mono<DevicePrincipal> resolveDevicePrincipal(Principal principal);

    /**
     * 获取设备身份证明信息
     *
     * @param deviceId 设备ID
     * @param identity identity
     * @param type     type
     * @return Credential
     */
    Mono<Credential> getDeviceCredential(String deviceId, Identity identity, CredentialType type);

}
