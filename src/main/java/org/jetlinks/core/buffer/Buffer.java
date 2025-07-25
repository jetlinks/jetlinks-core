package org.jetlinks.core.buffer;

import io.netty.buffer.ByteBuf;


public interface Buffer {

    static Buffer create(ByteBuf buf){
        return new BufferImpl(buf);
    }

    ByteBuf byteBuf();

}
