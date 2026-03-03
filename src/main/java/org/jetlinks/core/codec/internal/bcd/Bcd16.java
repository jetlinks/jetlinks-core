package org.jetlinks.core.codec.internal.bcd;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.codec.Codec;
import org.jspecify.annotations.NonNull;

import static org.jetlinks.core.codec.internal.bcd.BcdUtils.*;

/**
 * 16 位 Packed BCD 编解码器 (2 字节).
 * <p>
 * 将 2 字节的 BCD 编码转换为整数. 例如: 0x1234 -> 1234.
 *
 * @author zhouhao
 * @since 1.2
 */
public class Bcd16 implements Codec<Integer> {

    @Override
    public Class<Integer> forType() {
        return Integer.class;
    }

    @Override
    public String getId() {
        return "bcd_16";
    }

    @Override
    public int byteLength() {
        return 2;
    }

    @Override
    public Integer decode(@NonNull ByteBuf payload) {
        return decodeBcd(payload.readShort());
    }

    @Override
    public ByteBuf encode(Integer body, ByteBuf buf) {
        buf.writeShort(encodeBcd(body));
        return buf;
    }
}
