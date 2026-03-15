package org.jetlinks.core.codec.internal.bcd;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.codec.Codec;
import org.jspecify.annotations.NonNull;

import static org.jetlinks.core.codec.internal.bcd.BcdUtils.decodeUnpackedBcd;
import static org.jetlinks.core.codec.internal.bcd.BcdUtils.encodeUnpackedBcd32;

/**
 * 32 位 Unpacked BCD 编解码器 (4 字节).
 * <p>
 * 每个字节存储一个十进制数字. 例如: 0x01020304 -> 1234.
 *
 * @author zhouhao
 * @since 1.2
 */
public class UnpackedBcd32 implements Codec<Integer> {

    @Override
    public Class<Integer> forType() {
        return Integer.class;
    }

    @Override
    public String getId() {
        return "unpacked_bcd_32";
    }

    @Override
    public int byteLength() {
        return 4;
    }

    @Override
    public Integer decode(@NonNull ByteBuf payload) {
        return decodeUnpackedBcd(payload.readInt());
    }

    @Override
    public ByteBuf encode(Integer body, ByteBuf buf) {
        buf.writeInt(encodeUnpackedBcd32(body));
        return buf;
    }
}
