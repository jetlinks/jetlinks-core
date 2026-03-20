package org.jetlinks.core.codec.layout;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;

/**
 * 交换字布局。
 * 将输入的字节序列按指定的块长度(word length)进行交换。
 *
 * <p>例如：按 2 字节(word)长度交换 [0x01, 0x02, 0x03, 0x04] => [0x03, 0x04, 0x01, 0x02]
 *
 * @author zhouhao
 * @since 1.2.4
 */
@AllArgsConstructor
public class WordSwapByteLayout implements ByteLayout {
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
        int words = length / wordLength;
        for (int i = 0; i < words / 2; i++) {
            int leftWordStart = readerIndex + i * wordLength;
            int rightWordStart = readerIndex + (words - 1 - i) * wordLength;

            for (int j = 0; j < wordLength; j++) {
                byte left = byteBuf.getByte(leftWordStart + j);
                byte right = byteBuf.getByte(rightWordStart + j);
                byteBuf.setByte(leftWordStart + j, right);
                byteBuf.setByte(rightWordStart + j, left);
            }
        }

        return byteBuf;
    }
}
