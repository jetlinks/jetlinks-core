package org.jetlinks.core.codec.layout;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;

/**
 * 反转字节布局。
 * 将输入的字节序列按指定的字节长度完全反转。
 *
 * <p>例如：[0x01, 0x02, 0x03, 0x04] 反转为 [0x04, 0x03, 0x02, 0x01]
 *
 * @author zhouhao
 * @since 1.2.4
 */
@AllArgsConstructor
public class ReverseByteLayout implements ByteLayout {
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
        int length = byteLength();
        if (length < 0) {
            length = byteBuf.readableBytes();
        }
        if (length == 0) {
            return byteBuf;
        }
        if (byteBuf.readableBytes() < length) {
            throw new IllegalArgumentException("Input buffer has insufficient data: expected " + length + ", but got " + byteBuf.readableBytes());
        }

        int readerIndex = byteBuf.readerIndex();
        for (int i = 0; i < length / 2; i++) {
            int leftIdx = readerIndex + i;
            int rightIdx = readerIndex + length - 1 - i;
            byte left = byteBuf.getByte(leftIdx);
            byte right = byteBuf.getByte(rightIdx);
            byteBuf.setByte(leftIdx, right);
            byteBuf.setByte(rightIdx, left);
        }

        return byteBuf;
    }
}
