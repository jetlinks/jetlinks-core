package org.jetlinks.core.codec;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;

public interface Codec<T> extends Decoder<T>, Encoder<T> {


}
