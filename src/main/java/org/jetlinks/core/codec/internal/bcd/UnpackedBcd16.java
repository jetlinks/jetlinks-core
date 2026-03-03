package org.jetlinks.core.codec.internal.bcd;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.codec.Codec;
import org.jspecify.annotations.NonNull;

import static org.jetlinks.core.codec.internal.bcd.BcdUtils.decodeUnpackedBcd;
import static org.jetlinks.core.codec.internal.bcd.BcdUtils.encodeUnpackedBcd16;

/**
 * 16 位 Unpacked BCD 编解码器 (2 字节).
 * <p>
 * 每个字节存储一个十进制数字. 例如: 0x0102 -> 12.
 *
 * @author zhouhao
 * @since 1.2
 */
public class UnpackedBcd16 implements Codec<Integer> {

    @Override
    public Class<Integer> forType() {
        return Integer.class;
    }

    @Override
    public String getId() {
        return "unpacked_bcd_16";
    }

    @Override
    public int byteLength() {
        return 2;
    }

    @Override
    public Integer decode(@NonNull ByteBuf payload) {
        return decodeUnpackedBcd(payload.readShort());
    }

    @Override
    public ByteBuf encode(Integer body, ByteBuf buf) {
        buf.writeShort(encodeUnpackedBcd16(body));
        return buf;
    }
}
