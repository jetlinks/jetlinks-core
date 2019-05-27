package org.jetlinks.core.message.codec;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import org.jetlinks.coap.CoapPacket;

import javax.annotation.Nonnull;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public class CoAPMessage implements EncodedMessage {

    private String deviceId;

    @Getter
    protected CoapPacket packet;

    public CoAPMessage(String deviceId, CoapPacket packet) {
        this.deviceId = deviceId;
        this.packet = packet;
    }

    @Nonnull
    @Override
    public ByteBuf getByteBuf() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public String getDeviceId() {
        return deviceId;
    }

    @Override
    public String toString() {
        return packet.toString(true, false, true, false);
    }
}
