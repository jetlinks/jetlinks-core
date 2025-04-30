package org.jetlinks.core.buffer;

import io.netty.buffer.ByteBuf;


public interface Buffer {

    static Buffer create(ByteBuf buf){
        return new BufferImpl(buf);
    }

    ByteBuf byteBuf();

    BitsBuffer readBits(int length);

    BitsBuffer getBits(int offset, int length);

    Buffer writeBits(BitsBuffer bitsBuffer);


}
