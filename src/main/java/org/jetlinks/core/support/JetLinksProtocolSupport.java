package org.jetlinks.core.support;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.device.AuthenticationRequest;
import org.jetlinks.core.device.AuthenticationResponse;
import org.jetlinks.core.device.DeviceOperation;
import org.jetlinks.core.device.MqttAuthenticationRequest;
import org.jetlinks.core.message.codec.DeviceMessageCodec;
import org.jetlinks.core.metadata.DeviceMetadataCodec;

import javax.annotation.Nonnull;
import java.util.concurrent.*;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Slf4j
public class JetLinksProtocolSupport implements ProtocolSupport {

    private DeviceMessageCodec deviceMessageCodec = new JetLinksDeviceMessageCodec();

    private DeviceMetadataCodec metadataCodec = new JetLinksDeviceMetadataCodec();

    @Getter
    @Setter
    private Executor executor = ForkJoinPool.commonPool();

    @Override
    @Nonnull
    public String getId() {
        return "jet-links";
    }

    @Override
    public String getName() {
        return "JetLinks Protocol 1.0";
    }

    @Override
    public String getDescription() {
        return "JetLinks 协议";
    }

    @Override
    @Nonnull
    public DeviceMessageCodec getMessageCodec() {
        return deviceMessageCodec;
    }

    @Override
    @Nonnull
    public DeviceMetadataCodec getMetadataCodec() {
        return metadataCodec;
    }

    @Override
    @Nonnull
    public CompletionStage<AuthenticationResponse> authenticate(@Nonnull AuthenticationRequest request,
                                                                @Nonnull DeviceOperation deviceOperation) {

        if (request instanceof MqttAuthenticationRequest) {
            MqttAuthenticationRequest mqtt = ((MqttAuthenticationRequest) request);
            // secureId|timestamp
            String username = mqtt.getUsername();
            // md5(secureId|timestamp|secureKey)
            String password = mqtt.getPassword();
            String requestSecureId;
            try {
                String[] arr = username.split("[|]");
                if (arr.length <= 1) {
                    return CompletableFuture.completedFuture(AuthenticationResponse.error(401, "用户名格式错误"));
                }
                requestSecureId = arr[0];
                long time = Long.parseLong(arr[1]);
                //和设备时间差大于5分钟则认为无效
                if (Math.abs(System.currentTimeMillis() - time) > TimeUnit.MINUTES.toMillis(5)) {
                    return CompletableFuture.completedFuture(AuthenticationResponse.error(401, "设备时间不同步"));
                }
                return deviceOperation.getAllAsync("secureId", "secureKey")
                        .thenApply(conf -> {
                            String secureId = (String) conf.get("secureId");
                            String secureKey = (String) conf.get("secureKey");
                            //签名
                            String digest = DigestUtils.md5Hex(username + "|" + secureKey);
                            if (requestSecureId.equals(secureId) && digest.equals(password)) {
                                return AuthenticationResponse.success();
                            } else {
                                return AuthenticationResponse.error(401, "密钥错误");
                            }
                        });
//                return CompletableFuture.supplyAsync(() -> {
//
//                }, executor);

            } catch (NumberFormatException e) {
                return CompletableFuture.completedFuture(AuthenticationResponse.error(401, "用户名格式错误"));
            }
        }
        return CompletableFuture.completedFuture(AuthenticationResponse.error(400, "不支持的授权类型:" + request));
    }
}
