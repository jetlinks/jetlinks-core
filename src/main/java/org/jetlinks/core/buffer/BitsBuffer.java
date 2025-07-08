package org.jetlinks.core.buffer;

/**
 * 位缓冲区接口，用于在字节缓冲区上进行位级别的读写操作。
 * 提供了方便的方法来操作单个位、字节和整型数据，支持跨字节的位操作。
 * 可用于解析和构建各种二进制协议中的位字段。
 *
 * @author zhouhao
 * @since 1.3
 */
public interface BitsBuffer {
    byte ZERO = 0x00;
    byte ONE = 0x01;

    /**
     * 获取位缓冲区的总长度(按位计算)，1字节=8位
     *
     * @return 位长度
     */
    int length();

    /**
     * 获取指定偏移量的位值，返回0或1
     *
     * @param offset 位偏移量
     * @return 位值 (0或1)
     * @throws IndexOutOfBoundsException 当偏移量超出有效范围时
     */
    int getBit(int offset);

    /**
     * 从当前读索引位置读取一个位，并移动读索引
     *
     * @return 位值 (0或1)
     * @throws IndexOutOfBoundsException 当没有更多位可读时
     */
    int readBit();

    /**
     * 从当前读索引位置读取指定长度的位，并返回包含这些位的新缓冲区
     * 读取后，当前缓冲区的读索引会前进指定的长度
     *
     * @param length 要读取的位数
     * @return 包含读取位的新缓冲区
     * @throws IndexOutOfBoundsException 当可读位数不足时
     */
    BitsBuffer readBits(int length);

    /**
     * 创建当前缓冲区的切片视图，不会影响原缓冲区的读索引和写索引
     *
     * @param offset 切片的起始位偏移量
     * @param length 切片的位长度
     * @return 表示切片的新缓冲区
     * @throws IndexOutOfBoundsException 当参数超出有效范围时
     */
    BitsBuffer slice(int offset, int length);

    /**
     * 在当前写索引位置写入一个位，并移动写索引
     *
     * @param bit1 要写入的位值 (0或1)
     * @return this
     * @throws IndexOutOfBoundsException 当缓冲区已满时
     */
    BitsBuffer writeBit(byte bit1);

    /**
     * 在当前写索引位置写入一个字节(8位)，并移动写索引
     * 高位优先写入：字节的最高有效位(MSB)写入当前位置，最低有效位(LSB)写入当前位置+7
     *
     * @param bit8 要写入的字节
     * @return this
     * @throws IndexOutOfBoundsException 当缓冲区剩余空间不足8位时
     */
    BitsBuffer writeByte(byte bit8);

    /**
     * 在当前写索引位置写入一个32位整数，并移动写索引
     * 高位优先写入：整数的最高有效字节写入当前位置，然后依次写入后续字节
     *
     * @param bit32 要写入的32位整数
     * @return this
     * @throws IndexOutOfBoundsException 当缓冲区剩余空间不足32位时
     */
    BitsBuffer writeInt(int bit32);


//    BitsBuffer writeIntLE(int bit32);
}
