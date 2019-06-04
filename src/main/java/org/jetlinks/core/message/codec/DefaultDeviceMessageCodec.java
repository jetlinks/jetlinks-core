package org.jetlinks.core.message.codec;

import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.interceptor.DecodeDeviceMessageInterceptor;
import org.jetlinks.core.message.interceptor.DeviceMessageInterceptor;
import org.jetlinks.core.message.interceptor.EncodeDeviceMessageInterceptor;

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

    private List<DecodeDeviceMessageInterceptor> decodeDeviceMessageInterceptors = new CopyOnWriteArrayList<>();

    private List<EncodeDeviceMessageInterceptor> encodeDeviceMessageInterceptors = new CopyOnWriteArrayList<>();


    public void register(TransportDeviceMessageCodec codec) {
        messageCodec.put(codec.getSupportTransport(), codec);
    }

    public void register(DeviceMessageInterceptor interceptor) {
        if (interceptor instanceof DecodeDeviceMessageInterceptor) {
            decodeDeviceMessageInterceptors.add(((DecodeDeviceMessageInterceptor) interceptor));
        }
        if (interceptor instanceof EncodeDeviceMessageInterceptor) {
            encodeDeviceMessageInterceptors.add(((EncodeDeviceMessageInterceptor) interceptor));
        }
    }

    @Override
    public EncodedMessage encode(Transport transport, MessageEncodeContext context) {
        for (EncodeDeviceMessageInterceptor interceptor : encodeDeviceMessageInterceptors) {
            interceptor.preEncode(context);
        }
        EncodedMessage message = Objects.requireNonNull(messageCodec.get(transport), "unsupported transport:" + transport).encode(context);

        for (EncodeDeviceMessageInterceptor interceptor : encodeDeviceMessageInterceptors) {
            message = interceptor.postEncode(context, message);
        }
        return message;
    }

    @Override
    public DeviceMessage decode(Transport transport, MessageDecodeContext context) {
        for (DecodeDeviceMessageInterceptor interceptor : decodeDeviceMessageInterceptors) {
            interceptor.preDecode(context);
        }
        DeviceMessage message = Objects.requireNonNull(messageCodec.get(transport), "unsupported transport:" + transport).decode(context);

        for (DecodeDeviceMessageInterceptor interceptor : decodeDeviceMessageInterceptors) {
            message = interceptor.postDecode(context, message);
        }
        return message;
    }
}
