package org.jetlinks.core.codec.layout;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;

@AllArgsConstructor
class DirectByteLayout implements ByteLayout{
    private final String id;
    private final int byteLength;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int byteLength() {
        return byteLength;
    }

    @Override
    public ByteBuf reorder(ByteBuf byteBuf) {
        return byteBuf;
    }
}
