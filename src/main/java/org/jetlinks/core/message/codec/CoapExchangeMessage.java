package org.jetlinks.core.message.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import org.eclipse.californium.core.coap.Option;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.server.resources.CoapExchange;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public class CoapExchangeMessage implements CoapMessage {

    @Getter
    protected CoapExchange exchange;

    public CoapExchangeMessage(CoapExchange exchange) {
        this.exchange = exchange;
    }

    static byte[] empty = new byte[0];

    @Nonnull
    @Override
    public ByteBuf getPayload() {
        if (exchange.getRequestPayload() == null) {
            return Unpooled.wrappedBuffer(empty);
        }
        return Unpooled.wrappedBuffer(exchange.getRequestPayload());
    }

    @Override
    public String toString() {
        Request request = exchange.advanced().getRequest();
        StringBuilder builder = new StringBuilder()
                .append(request.getCode().name()).append(" ").append(getPath())
                .append("\n");

        for (Option option : request.getOptions().asSortedList()) {
            builder.append(option).append("\n");
        }
        builder.append("\n");

        byte[] payload = exchange.getRequestPayload();
        if (payload == null || payload.length == 0) {
            builder.append("no payload");
        } else {
            ByteBuf byteBuf= getPayload();
            if(ByteBufUtil.isText(byteBuf, StandardCharsets.UTF_8)){
                builder.append(byteBuf.toString(StandardCharsets.UTF_8));
            }else {
                ByteBufUtil.appendPrettyHexDump(builder, getPayload());
            }
        }
        return builder.toString();
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
