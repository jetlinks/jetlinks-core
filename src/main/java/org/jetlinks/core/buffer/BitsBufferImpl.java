package org.jetlinks.core.buffer;

import io.netty.buffer.ByteBuf;

/**
 * BitsBuffer接口的实现类，基于Netty的ByteBuf实现位级别的读写操作。
 * 支持字节偏移和位偏移，可在任意位置读写位数据，对于非字节对齐的位操作尤其有用。
 * 此实现使用大端序(Big-Endian)处理位，即高位在前，低位在后。
 *
 * @author cursor
 * @since 1.3
 */
public class BitsBufferImpl implements BitsBuffer{

    /**
     * 底层字节缓冲区
     */
    private final ByteBuf byteBuf;

    /**
     * 位缓冲区的总位长度
     */
    private final int length;

    /**
     * 当前写入位置的索引
     */
    private int writeIndex = 0;

    /**
     * 当前读取位置的索引
     */
    private int readIndex = 0;

    /**
     * 在字节内的位偏移量(0-7)
     * 用于支持非字节对齐的位操作
     */
    private final int bitOffset;

    /**
     * ByteBuf中的字节偏移量
     * 指定位缓冲区从ByteBuf的哪个字节开始
     */
    private final int byteOffset;

    /**
     * 创建位缓冲区
     * 从ByteBuf的开始位置(字节偏移0)创建指定长度的位缓冲区
     *
     * @param byteBuf ByteBuf对象
     * @param length 位长度
     */
    public BitsBufferImpl(ByteBuf byteBuf, int length) {
        this(byteBuf, 0, length, 0);
    }

    /**
     * 在指定ByteBuf的指定字节偏移处创建位缓冲区
     * 位偏移默认为0，即从字节的第一位开始
     *
     * @param byteBuf ByteBuf对象
     * @param byteOffset 字节偏移
     * @param length 位长度
     */
    public BitsBufferImpl(ByteBuf byteBuf, int byteOffset, int length) {
        this(byteBuf, byteOffset, length, 0);
    }

    /**
     * 完整的构造函数，支持字节偏移和位偏移
     * 此构造函数允许从ByteBuf的任意位置开始创建位缓冲区
     *
     * @param byteBuf ByteBuf对象
     * @param byteOffset 字节偏移
     * @param length 位长度
     * @param bitOffset 位偏移(0-7)，表示在起始字节内的位位置
     * @throws IllegalArgumentException 当参数无效时
     */
    private BitsBufferImpl(ByteBuf byteBuf, int byteOffset, int length, int bitOffset) {
        if (byteOffset < 0) {
            throw new IllegalArgumentException("字节偏移不能为负数");
        }
        if (bitOffset < 0 || bitOffset >= 8) {
            throw new IllegalArgumentException("位偏移必须在0-7之间");
        }
        if (length < 0) {
            throw new IllegalArgumentException("长度不能为负数");
        }

        this.byteBuf = byteBuf;
        this.byteOffset = byteOffset;
        this.length = length;
        this.bitOffset = bitOffset;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int length() {
        return length;
    }

    /**
     * {@inheritDoc}
     * 计算真实的物理位置并获取对应位的值
     */
    @Override
    public int getBit(int offset) {
        if (offset < 0 || offset >= length) {
            throw new IndexOutOfBoundsException("Offset: " + offset + ", Length: " + length);
        }

        // 计算真实的位偏移，加上初始位偏移
        int actualOffset = offset + bitOffset;
        int byteIndex = byteOffset + (actualOffset / 8);
        int bitIndex = actualOffset % 8;

        byte b = byteBuf.getByte(byteIndex);
        return (b >> (7 - bitIndex)) & 0x01;
    }

    /**
     * {@inheritDoc}
     * 从当前读索引位置读取一个位，并前进读索引
     */
    @Override
    public int readBit() {
        if (readIndex >= length) {
            throw new IndexOutOfBoundsException("读取索引超出缓冲区范围");
        }

        int bit = getBit(readIndex);
        readIndex++;
        return bit;
    }

    /**
     * {@inheritDoc}
     * 通过slice操作实现批量位读取
     */
    @Override
    public BitsBuffer readBits(int length) {
        if (readIndex + length > this.length) {
            throw new IndexOutOfBoundsException("没有足够的位可读取");
        }

        BitsBuffer result = slice(readIndex, length);
        readIndex += length;
        return result;
    }

    /**
     * {@inheritDoc}
     * 创建一个新的BitsBuffer，共享底层ByteBuf但具有独立的位视图
     */
    @Override
    public BitsBuffer slice(int offset, int length) {
        if (offset < 0 || length < 0 || offset + length > this.length) {
            throw new IndexOutOfBoundsException("Offset: " + offset + ", Length: " + length + ", Buffer length: " + this.length);
        }

        // 计算真实的位偏移
        int actualOffset = offset + bitOffset;
        int startByteIndex = byteOffset + (actualOffset / 8);
        int newBitOffset = actualOffset % 8;

        // 计算需要的字节数
        int byteCount = (newBitOffset + length + 7) / 8;

        // 创建一个新的ByteBuf视图，考虑位偏移
        ByteBuf slicedBuf = byteBuf.slice(startByteIndex, byteCount);

        // 创建新的BitsBuffer，传递位偏移
        return new BitsBufferImpl(slicedBuf, 0, length, newBitOffset);
    }

    /**
     * {@inheritDoc}
     * 实现单个位的写入，在指定位置设置或清除一个位
     */
    @Override
    public BitsBuffer writeBit(byte bit1) {
        if (writeIndex >= length) {
            throw new IndexOutOfBoundsException("Buffer is full");
        }

        int actualOffset = writeIndex + bitOffset;
        int byteIndex = byteOffset + (actualOffset / 8);
        int bitIndex = actualOffset % 8;

        byte b = byteBuf.getByte(byteIndex);
        // 清除目标位
        b &= (byte) ~(1 << (7 - bitIndex));
        // 设置新值
        b |= (byte) ((bit1 & 0x01) << (7 - bitIndex));

        byteBuf.setByte(byteIndex, b);
        writeIndex++;

        return this;
    }

    /**
     * {@inheritDoc}
     * 实现字节(8位)的写入，对齐和非对齐情况分别处理
     */
    @Override
    public BitsBuffer writeByte(byte bit8) {
        if (writeIndex + 8 > length) {
            throw new IndexOutOfBoundsException("Not enough space in buffer");
        }

        int actualOffset = writeIndex + bitOffset;

        // 如果写入位置正好是字节边界上，可以直接写入
        if (actualOffset % 8 == 0) {
            byteBuf.setByte(byteOffset + (actualOffset / 8), bit8);
            writeIndex += 8;
            return this;
        }

        // 非字节边界情况下需要处理位分布
        int byteIndex = byteOffset + (actualOffset / 8);
        int bitOffset = actualOffset % 8;

        // 第一个字节：保留前bitOffset位，然后添加bit8的高位部分
        byte firstByte = byteBuf.getByte(byteIndex);
        // 清除要写入的位
        firstByte &= (byte) (0xFF << (8 - bitOffset));
        // 写入bit8的高位部分
        firstByte |= (byte) ((bit8 & 0xFF) >>> bitOffset);
        byteBuf.setByte(byteIndex, firstByte);

        // 第二个字节：保留后(8-bitOffset)位，添加bit8的低位部分
        if (bitOffset > 0) {
            byte secondByte = byteBuf.getByte(byteIndex + 1);
            // 清除要写入的位
            secondByte &= (byte) (0xFF >>> bitOffset);
            // 写入bit8的低位部分
            secondByte |= (byte) ((bit8 & 0xFF) << (8 - bitOffset));
            byteBuf.setByte(byteIndex + 1, secondByte);
        }

        writeIndex += 8;
        return this;
    }

    /**
     * {@inheritDoc}
     * 实现32位整数的写入，对齐和非对齐情况分别处理
     */
    @Override
    public BitsBuffer writeInt(int bit32) {
        if (writeIndex + 32 > length) {
            throw new IndexOutOfBoundsException("Not enough space in buffer");
        }

        int actualOffset = writeIndex + bitOffset;

        // 如果写入位置在字节边界上，可以直接按字节写入
        if (actualOffset % 8 == 0) {
            int byteIndex = byteOffset + (actualOffset / 8);
            byteBuf.setByte(byteIndex, (byte) ((bit32 >> 24) & 0xFF));
            byteBuf.setByte(byteIndex + 1, (byte) ((bit32 >> 16) & 0xFF));
            byteBuf.setByte(byteIndex + 2, (byte) ((bit32 >> 8) & 0xFF));
            byteBuf.setByte(byteIndex + 3, (byte) (bit32 & 0xFF));
            writeIndex += 32;
            return this;
        }

        // 非字节边界情况下，分四次写入字节
        writeByte((byte) ((bit32 >> 24) & 0xFF));
        writeByte((byte) ((bit32 >> 16) & 0xFF));
        writeByte((byte) ((bit32 >> 8) & 0xFF));
        writeByte((byte) (bit32 & 0xFF));

        return this;
    }


}
