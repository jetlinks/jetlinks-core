package org.jetlinks.core.message.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Option;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public class CoapExchangeMessage implements CoapMessage {

    @Getter
    protected CoapExchange exchange;

    @Nonnull
    @Override
    public CoAP.Code getCode() {
        return exchange.getRequestCode();
    }

    public CoapExchangeMessage(CoapExchange exchange) {
        this.exchange = exchange;
    }

    static byte[] empty = new byte[0];

    public void response(CoapResponseMessage message) {
        Response response = new Response(message.getCode());

        if (CollectionUtils.isEmpty(message.getOptions())) {
            message.getOptions().forEach(response.getOptions()::addOption);
        }
        byte[] payload = message.payloadAsBytes();
        if (payload.length > 0) {
            response.setPayload(payload);
        }

        exchange.advanced().sendResponse(response);

    }

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
        return print(true);
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
