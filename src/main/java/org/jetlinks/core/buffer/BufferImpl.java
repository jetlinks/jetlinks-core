package org.jetlinks.core.buffer;

import io.netty.buffer.ByteBuf;

public class BufferImpl implements Buffer{
    private final ByteBuf byteBuf;

    private long bitsReadIndex;

    public BufferImpl(ByteBuf byteBuf) {
        this.byteBuf = byteBuf;
    }

    @Override
    public ByteBuf byteBuf() {
        return byteBuf;
    }

    @Override
    public BitsBuffer readBits(int length) {

        return null;
    }

    @Override
    public BitsBuffer getBits(int offset, int length) {
        return null;
    }

    @Override
    public Buffer writeBits(BitsBuffer bitsBuffer) {
        return null;
    }
}
