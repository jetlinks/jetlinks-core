package org.jetlinks.core.message.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import org.eclipse.californium.core.coap.Option;
import org.eclipse.californium.core.server.resources.CoapExchange;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public class CoapExchangeMessage implements CoapMessage {

    private String deviceId;

    @Getter
    protected CoapExchange exchange;

    public CoapExchangeMessage(String deviceId, CoapExchange exchange) {
        this.deviceId = deviceId;
        this.exchange = exchange;
    }

    @Nonnull
    @Override
    public ByteBuf getPayload() {
        return Unpooled.wrappedBuffer(exchange.getRequestPayload());
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

    @Override
    @Nonnull
    public String getPath() {
        return exchange.getRequestOptions().getUriPathString();
    }

    @Override
    @Nonnull
    public List<Option> getOptions() {
        return exchange
                .getRequestOptions()
                .asSortedList();
    }
}
