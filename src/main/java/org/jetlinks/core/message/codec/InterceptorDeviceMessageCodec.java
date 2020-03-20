package org.jetlinks.core.message.codec;

import org.jetlinks.core.message.Message;
import org.jetlinks.core.message.interceptor.DeviceMessageCodecInterceptor;
import org.jetlinks.core.message.interceptor.DeviceMessageDecodeInterceptor;
import org.jetlinks.core.message.interceptor.DeviceMessageEncodeInterceptor;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author bsetfeng
 * @author zhouhao
 * @since 1.0
 **/
public class InterceptorDeviceMessageCodec implements DeviceMessageCodec {

    private DeviceMessageCodec messageCodec;

    private List<DeviceMessageDecodeInterceptor> decodeDeviceMessageInterceptors = new CopyOnWriteArrayList<>();

    private List<DeviceMessageEncodeInterceptor> encodeDeviceMessageInterceptors = new CopyOnWriteArrayList<>();

    public InterceptorDeviceMessageCodec(DeviceMessageCodec codec) {
        this.messageCodec = codec;
    }

    @Override
    public Transport getSupportTransport() {
        return messageCodec.getSupportTransport();
    }

    public void register(DeviceMessageCodecInterceptor interceptor) {
        if (interceptor instanceof DeviceMessageDecodeInterceptor) {
            decodeDeviceMessageInterceptors.add(((DeviceMessageDecodeInterceptor) interceptor));
        }
        if (interceptor instanceof DeviceMessageEncodeInterceptor) {
            encodeDeviceMessageInterceptors.add(((DeviceMessageEncodeInterceptor) interceptor));
        }
    }

    @Nonnull
    @Override
    public Flux<? extends EncodedMessage> encode(@Nonnull MessageEncodeContext context) {
        return Flux.defer(() -> {
            for (DeviceMessageEncodeInterceptor interceptor : encodeDeviceMessageInterceptors) {
                interceptor.preEncode(context);
            }
            Flux<? extends EncodedMessage> message = Flux.from(messageCodec.encode(context));

            for (DeviceMessageEncodeInterceptor interceptor : encodeDeviceMessageInterceptors) {
                message = message.flatMap(msg -> interceptor.postEncode(context, msg));
            }

            return message;
        });

    }

    @Nonnull
    @Override
    public Flux<? extends Message> decode(@Nonnull MessageDecodeContext context) {
        return Flux.defer(() -> {
            for (DeviceMessageDecodeInterceptor interceptor : decodeDeviceMessageInterceptors) {
                interceptor.preDecode(context);
            }
            Flux<? extends Message> message = Flux.from(messageCodec.decode(context));

            for (DeviceMessageDecodeInterceptor interceptor : decodeDeviceMessageInterceptors) {
                message = message.flatMap(msg -> interceptor.postDecode(context, msg));
            }

            return message;
        });
    }
}
