package org.jetlinks.core.buffer;

import io.netty.buffer.ByteBuf;

public class BufferImpl implements Buffer{
    private final ByteBuf byteBuf;

    public BufferImpl(ByteBuf byteBuf) {
        this.byteBuf = byteBuf;
    }

    @Override
    public ByteBuf byteBuf() {
        return byteBuf;
    }

}
