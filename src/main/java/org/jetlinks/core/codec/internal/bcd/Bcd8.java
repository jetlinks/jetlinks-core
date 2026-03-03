package org.jetlinks.core.codec.internal.bcd;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.codec.Codec;
import org.jspecify.annotations.NonNull;

import static org.jetlinks.core.codec.internal.bcd.BcdUtils.*;

/**
 * 8 位 Packed BCD 编解码器 (1 字节).
 * <p>
 * 将 1 字节的 BCD 编码转换为整数. 例如: 0x12 -> 12.
 *
 * @author zhouhao
 * @since 1.2
 */
public class Bcd8 implements Codec<Integer> {

    @Override
    public Class<Integer> forType() {
        return Integer.class;
    }

    @Override
    public String getId() {
        return "bcd_8";
    }

    @Override
    public int byteLength() {
        return 1;
    }

    @Override
    public Integer decode(@NonNull ByteBuf payload) {
        return decodeBcd(payload.readByte());
    }

    @Override
    public ByteBuf encode(Integer body, ByteBuf buf) {
        buf.writeByte(encodeBcd8(body));
        return buf;
    }
}
