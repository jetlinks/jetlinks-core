package org.jetlinks.core.message.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import org.eclipse.californium.core.server.resources.CoapExchange;

import javax.annotation.Nonnull;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public class CoapMessage implements EncodedMessage {

    private String deviceId;

    @Getter
    protected CoapExchange exchange;

    public CoapMessage(String deviceId, CoapExchange exchange) {
        this.deviceId = deviceId;
        this.exchange = exchange;
    }

    @Nonnull
    @Override
    public ByteBuf getPayload() {
        return Unpooled.copiedBuffer(exchange.getRequestPayload());
    }

    @Nonnull
    @Override
    public String getDeviceId() {
        return deviceId;
    }

    @Override
    public String toString() {
        return exchange.toString();
    }
}
