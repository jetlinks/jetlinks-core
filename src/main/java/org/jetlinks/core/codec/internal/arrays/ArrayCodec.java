package org.jetlinks.core.codec.internal.arrays;

import io.netty.buffer.ByteBuf;
import lombok.SneakyThrows;
import org.jetlinks.core.codec.Codec;

import javax.annotation.Nonnull;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 数组编解码器抽象基类.
 * 将单个元素的编解码器转换为数组编解码器，支持固定长度和动态长度的元素编解码.
 *
 * @param <T> 数组元素类型
 * @author zhouhao
 * @since 1.2
 */
public class ArrayCodec<T> implements Codec<T[]> {

    protected final Codec<T> codec;
    protected final Class<T[]> arrayType;
    protected final Class<T> componentType;

    /**
     * 构造函数
     *
     * @param codec 元素编解码器
     */
    @SuppressWarnings("unchecked")
    public ArrayCodec(Codec<T> codec) {
        if (codec == null) {
            throw new IllegalArgumentException("codec cannot be null");
        }
        this.codec = codec;
        this.componentType = codec.forType();
        this.arrayType = (Class<T[]>) Array.newInstance(componentType, 0).getClass();
    }

    /**
     * 创建指定长度的数组容器
     *
     * @param length 数组长度
     * @return 数组实例
     */
    @SuppressWarnings("unchecked")
    protected T[] newContainer(int length) {
        return (T[]) Array.newInstance(componentType, length);
    }

    /**
     * 将元素编解码器解码的结果转换为目标类型
     *
     * @param value 解码结果
     * @return 转换后的值
     */
    @SuppressWarnings("unchecked")
    protected T convertElement(Object value) {
        return (T) value;
    }

    @Override
    @SuppressWarnings("all")
    public Class<T[]> forType() {
        return arrayType;
    }

    @Override
    public String getId() {
        return codec.getId() + "_array";
    }

    @Override
    public int byteLength() {
        return -1;
    }

    /**
     * 判断是否支持指定长度的字节解码
     *
     * @param len 长度
     * @return 是否支持
     */
    @Override
    public boolean isByteLengthSupported(int len) {
        int elementLength = codec.byteLength();
        if (elementLength == -1) {
            // 动态长度，总是支持
            return true;
        }
        // 固定长度：总长度必须是元素长度的倍数
        return len > 0 && len % elementLength == 0;
    }

    /**
     * 解码：将字节数组解码为元素数组
     * 根据元素编解码器的字节长度类型采用不同的解码策略：
     * - 固定长度：根据总字节数计算元素数量
     * - 动态长度：读取到 ByteBuf 为空
     *
     * @param payload ByteBuf
     * @return 元素数组
     */
    @Override
    public T[] decode(@Nonnull ByteBuf payload) {

        int elementLength = codec.byteLength();

        if (elementLength == -1) {
            // 动态长度：读取到 ByteBuf 为空
            ArrayList<T> elements = new ArrayList<>();
            while (payload.isReadable()) {
                Object decoded = codec.decode(payload);
                elements.add(convertElement(decoded));
            }
            return elements.toArray(newContainer(elements.size()));
        } else {
            // 固定长度：根据总字节数计算元素数量
            int totalBytes = payload.readableBytes();
            if (totalBytes == 0) {
                return newContainer(0);
            }
            if (totalBytes % elementLength != 0) {
                throw new IllegalArgumentException(
                    String.format("Invalid payload length: %d bytes, expected multiple of %d",
                                  totalBytes, elementLength));
            }

            int elementCount = totalBytes / elementLength;
            T[] elements = newContainer(elementCount);
            for (int i = 0; i < elementCount; i++) {
                Object decoded = codec.decode(payload);
                elements[i] = convertElement(decoded);
            }
            return elements;
        }
    }

    /**
     * 编码：将元素数组编码为字节数组
     * 遍历数组中的每个元素，使用元素编解码器进行编码
     *
     * @param body 元素数组
     * @param buf  ByteBuf
     * @return ByteBuf
     */
    @Override
    public ByteBuf encode(T[] body, ByteBuf buf) {
        if (body == null || body.length == 0) {
            return buf;
        }

        if (buf == null) {
            throw new IllegalArgumentException("buf cannot be null");
        }

        // 遍历数组中的每个元素进行编码
        for (int i = 0; i < body.length; i++) {
            T element = body[i];
            try {
                codec.encode(element, buf);
            } catch (Exception e) {
                throw new IllegalArgumentException(
                    String.format("Cannot encode element at index %d, codec: %s", i, codec.getId()), e);
            }
        }

        return buf;
    }
}
