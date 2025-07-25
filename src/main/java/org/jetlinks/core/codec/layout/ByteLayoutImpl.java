package org.jetlinks.core.codec.layout;

import io.netty.buffer.ByteBuf;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Arrays;

/**
 * 字节布局编解码器，用于重新排列字节顺序
 *
 * <p>通过 indexTransformer 数组指定字节重排规则：
 * - decode: indexTransformer[i] 表示输出第i个位置的字节来自输入的第indexTransformer[i]个位置
 * - encode: indexTransformer[i] 表示输入第i个字节将被放置到输出的第indexTransformer[i]个位置
 *
 * <p>预定义常量：
 * - ABCD: 正常顺序 [0,1,2,3]
 * - CDAB: 重排顺序 [2,3,0,1] - 将CD移到前面，AB移到后面
 *
 * @author jetlinks
 * @since 1.0
 */
class ByteLayoutImpl implements ByteLayout {
    //@formatter:on
    @Getter
    private final String id;
    private final int[] indexTransformer;
    private final int maxIndex;

    public ByteLayoutImpl(String id, int[] indexTransformer) {
        this.id = id;
        this.indexTransformer = indexTransformer;
        this.maxIndex = Arrays
            .stream(indexTransformer)
            .max()
            .orElseThrow(IllegalArgumentException::new);
    }

    @Override
    public int byteLength() {
        return indexTransformer.length;
    }


    /**
     * 解码：根据索引转换器重排字节顺序
     *
     * @param payload 输入字节缓冲区（不会被自动释放）
     * @return 重排后的字节缓冲区
     * @throws IndexOutOfBoundsException 如果索引超出范围
     */
    @Override
    public ByteBuf reorder(@Nonnull ByteBuf payload) {

        // 验证输入长度
        int inputLength = payload.readableBytes();
        if (inputLength == 0 && indexTransformer.length > 0) {
            throw new IllegalArgumentException("Input buffer is empty but transformer requires data");
        }

        // 验证索引转换器
        if (maxIndex >= inputLength) {
            throw new IndexOutOfBoundsException(inputLength);
        }
        payload.markReaderIndex();
        payload.markWriterIndex();
        int index = payload.readerIndex();
        byte[] bytes = new byte[indexTransformer.length];

        // 按索引转换器读取字节
        for (int i = 0; i < indexTransformer.length; i++) {
            bytes[i] = payload.getByte(indexTransformer[i] + index);
        }

        payload.setBytes(index, bytes);
        payload.resetReaderIndex();
        payload.resetWriterIndex();

        return payload;
    }

}
