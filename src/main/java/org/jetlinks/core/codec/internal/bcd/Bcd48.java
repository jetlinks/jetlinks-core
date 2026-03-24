package org.jetlinks.core.codec.internal.bcd;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.codec.Codec;
import org.jspecify.annotations.NonNull;

import static org.jetlinks.core.codec.internal.bcd.BcdUtils.*;

/**
 * 48 位 Packed BCD 编解码器 (6 字节).
 * <p>
 * 将 6 字节的 BCD 编码转换为长整数. 例如: 0x123456789012L -> 123456789012L.
 * 常用于电力仪表地址等.
 *
 * @author zhouhao
 * @since 1.2
 */
public class Bcd48 implements Codec<Long> {

    @Override
    public Class<Long> forType() {
        return Long.class;
    }

    @Override
    public String getId() {
        return "bcd_48";
    }

    @Override
    public int byteLength() {
        return 6;
    }

    @Override
    public Long decode(@NonNull ByteBuf payload) {
        // ByteBuf 没有 readLong48, 手动读取
        long bcd = 0;
        for (int i = 0; i < 6; i++) {
            bcd = (bcd << 8) | (payload.readByte() & 0xFF);
        }
        return decodeBcd64(bcd);
    }

    @Override
    public ByteBuf encode(Long body, ByteBuf buf) {
        long bcd = encodeBcd48(body);
        for (int i = 5; i >= 0; i--) {
            buf.writeByte((int) ((bcd >> (i * 8)) & 0xFF));
        }
        return buf;
    }
}
