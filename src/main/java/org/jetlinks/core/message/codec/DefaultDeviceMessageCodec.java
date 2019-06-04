package org.jetlinks.core.message.codec;

import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.interceptor.DecodeDeviceMessageCodecInterceptor;
import org.jetlinks.core.message.interceptor.DeviceMessageCodecInterceptor;
import org.jetlinks.core.message.interceptor.EncodeDeviceMessageCodecInterceptor;

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
public class DefaultDeviceMessageCodec implements DeviceMessageCodec {

    private Map<Transport, TransportDeviceMessageCodec> messageCodec = new HashMap<>();

    private List<DecodeDeviceMessageCodecInterceptor> decodeDeviceMessageInterceptors = new CopyOnWriteArrayList<>();

    private List<EncodeDeviceMessageCodecInterceptor> encodeDeviceMessageInterceptors = new CopyOnWriteArrayList<>();


    public void register(TransportDeviceMessageCodec codec) {
        messageCodec.put(codec.getSupportTransport(), codec);
    }

    public void register(DeviceMessageCodecInterceptor interceptor) {
        if (interceptor instanceof DecodeDeviceMessageCodecInterceptor) {
            decodeDeviceMessageInterceptors.add(((DecodeDeviceMessageCodecInterceptor) interceptor));
        }
        if (interceptor instanceof EncodeDeviceMessageCodecInterceptor) {
            encodeDeviceMessageInterceptors.add(((EncodeDeviceMessageCodecInterceptor) interceptor));
        }
    }

    @Override
    public EncodedMessage encode(Transport transport, MessageEncodeContext context) {
        for (EncodeDeviceMessageCodecInterceptor interceptor : encodeDeviceMessageInterceptors) {
            interceptor.preEncode(context);
        }
        EncodedMessage message = Objects.requireNonNull(messageCodec.get(transport), "unsupported transport:" + transport).encode(context);

        for (EncodeDeviceMessageCodecInterceptor interceptor : encodeDeviceMessageInterceptors) {
            message = interceptor.postEncode(context, message);
        }
        return message;
    }

    @Override
    public DeviceMessage decode(Transport transport, MessageDecodeContext context) {
        for (DecodeDeviceMessageCodecInterceptor interceptor : decodeDeviceMessageInterceptors) {
            interceptor.preDecode(context);
        }
        DeviceMessage message = Objects.requireNonNull(messageCodec.get(transport), "unsupported transport:" + transport).decode(context);

        for (DecodeDeviceMessageCodecInterceptor interceptor : decodeDeviceMessageInterceptors) {
            message = interceptor.postDecode(context, message);
        }
        return message;
    }
}
