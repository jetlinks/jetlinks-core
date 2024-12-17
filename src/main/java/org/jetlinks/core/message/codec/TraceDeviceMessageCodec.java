package org.jetlinks.core.message.codec;

import lombok.AllArgsConstructor;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.trace.FluxTracer;
import org.jetlinks.core.trace.ProtocolTracer;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;

@AllArgsConstructor
public class TraceDeviceMessageCodec implements DeviceMessageCodec {
    private final String protocolId;
    private final DeviceMessageCodec target;

    @Override
    public Transport getSupportTransport() {
        return target.getSupportTransport();
    }

    @Nonnull
    @Override
    public Flux<? extends Message> decode(@Nonnull MessageDecodeContext context) {
        return Flux
                .from(target.decode(context))
                .as(FluxTracer.create(ProtocolTracer.SpanName.decode0(protocolId)));
    }

    @Nonnull
    @Override
    public Flux<? extends EncodedMessage> encode(@Nonnull MessageEncodeContext context) {
        return Flux
                .from(target.encode(context))
                .as(FluxTracer.create(ProtocolTracer.SpanName.encode0(protocolId)));
    }
}
