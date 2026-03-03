package org.jetlinks.core.codec.internal.bcd;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.codec.Codec;
import org.jspecify.annotations.NonNull;

import static org.jetlinks.core.codec.internal.bcd.BcdUtils.*;

/**
 * 32 位 Packed BCD 编解码器 (4 字节).
 * <p>
 * 将 4 字节的 BCD 编码转换为整数. 例如: 0x12345678 -> 12345678.
 *
 * @author zhouhao
 * @since 1.2
 */
public class Bcd32 implements Codec<Integer> {

    @Override
    public Class<Integer> forType() {
        return Integer.class;
    }

    @Override
    public String getId() {
        return "bcd_32";
    }

    @Override
    public int byteLength() {
        return 4;
    }


    @Override
    public Integer decode(@NonNull ByteBuf payload) {
        return decodeBcd(payload.readInt());
    }

    @Override
    public ByteBuf encode(Integer body, ByteBuf buf) {
        buf.writeInt(encodeBcd32(body));
        return buf;
    }
}
