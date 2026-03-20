package org.jetlinks.core.codec.layout;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;

/**
 * 字内反转字节布局。
 * 将输入的字节序列按指定的字长度(word length)进行内部反转。
 *
 * <p>例如：按 2 字节(word)长度反转 [0x01, 0x02, 0x03, 0x04] => [0x02, 0x01, 0x04, 0x03]
 *
 * @author zhouhao
 * @since 1.2.4
 */
@AllArgsConstructor
public class WordReverseByteLayout implements ByteLayout {
    private final String id;
    private final int totalLength;
    private final int wordLength;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int byteLength() {
        return totalLength;
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

        if (length % wordLength != 0) {
            throw new IllegalArgumentException("Total length " + length + " must be a multiple of word length " + wordLength);
        }

        int readerIndex = byteBuf.readerIndex();
        for (int i = 0; i < length; i += wordLength) {
            int wordStart = readerIndex + i;
            for (int j = 0; j < wordLength / 2; j++) {
                int leftIdx = wordStart + j;
                int rightIdx = wordStart + wordLength - 1 - j;
                byte left = byteBuf.getByte(leftIdx);
                byte right = byteBuf.getByte(rightIdx);
                byteBuf.setByte(leftIdx, right);
                byteBuf.setByte(rightIdx, left);
            }
        }

        return byteBuf;
    }
}
