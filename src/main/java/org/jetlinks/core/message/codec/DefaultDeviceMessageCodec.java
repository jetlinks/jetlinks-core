package org.jetlinks.core.message.codec;

import lombok.AllArgsConstructor;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.message.interceptor.DeviceMessageDecodeInterceptor;
import org.jetlinks.core.message.interceptor.DeviceMessageCodecInterceptor;
import org.jetlinks.core.message.interceptor.DeviceMessageEncodeInterceptor;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author bsetfeng
 * @author zhouhao
 * @since 1.0
 **/
@AllArgsConstructor
public class DefaultDeviceMessageCodec implements DeviceMessageCodec {

    private DeviceMessageCodec messageCodec;

    @Override
    public Transport getSupportTransport() {
        return messageCodec.getSupportTransport();
    }

    private List<DeviceMessageDecodeInterceptor> decodeDeviceMessageInterceptors = new CopyOnWriteArrayList<>();

    private List<DeviceMessageEncodeInterceptor> encodeDeviceMessageInterceptors = new CopyOnWriteArrayList<>();

    public void register(DeviceMessageCodecInterceptor interceptor) {
        if (interceptor instanceof DeviceMessageDecodeInterceptor) {
            decodeDeviceMessageInterceptors.add(((DeviceMessageDecodeInterceptor) interceptor));
        }
        if (interceptor instanceof DeviceMessageEncodeInterceptor) {
            encodeDeviceMessageInterceptors.add(((DeviceMessageEncodeInterceptor) interceptor));
        }
    }

    @Override
    public Mono<EncodedMessage> encode(MessageEncodeContext context) {
        return Mono.defer(() -> {
            for (DeviceMessageEncodeInterceptor interceptor : encodeDeviceMessageInterceptors) {
                interceptor.preEncode(context);
            }
            Mono<EncodedMessage> message = messageCodec.encode(context);

            for (DeviceMessageEncodeInterceptor interceptor : encodeDeviceMessageInterceptors) {
                message = message.flatMap(msg -> interceptor.postEncode(context, msg));
            }

            return message;
        });

    }

    @Override
    public <T extends Message> Mono<T> decode(MessageDecodeContext context) {
        return Mono.defer(() -> {
            for (DeviceMessageDecodeInterceptor interceptor : decodeDeviceMessageInterceptors) {
                interceptor.preDecode(context);
            }
            Mono<T> message = messageCodec.decode(context);

            for (DeviceMessageDecodeInterceptor interceptor : decodeDeviceMessageInterceptors) {
                message = message.flatMap(msg -> interceptor.postDecode(context, msg));
            }

            return message;
        });
    }
}
