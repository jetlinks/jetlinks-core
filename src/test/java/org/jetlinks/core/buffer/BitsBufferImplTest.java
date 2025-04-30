package org.jetlinks.core.buffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import static org.junit.Assert.*;

public class BitsBufferImplTest {

    @Test
    public void testBasicBitOperation() {
        ByteBuf byteBuf = Unpooled.buffer(10);
        BitsBuffer bitsBuffer = new BitsBufferImpl(byteBuf, 80); // 10字节 = 80位

        // 写入单个位
        bitsBuffer.writeBit((byte) 1);
        bitsBuffer.writeBit((byte) 0);
        bitsBuffer.writeBit((byte) 1);
        bitsBuffer.writeBit((byte) 1);

        // 验证写入的位
        assertEquals(1, bitsBuffer.getBit(0));
        assertEquals(0, bitsBuffer.getBit(1));
        assertEquals(1, bitsBuffer.getBit(2));
        assertEquals(1, bitsBuffer.getBit(3));

        // 验证读取操作
        assertEquals(1, bitsBuffer.readBit());
        assertEquals(0, bitsBuffer.readBit());
        assertEquals(1, bitsBuffer.readBit());
        assertEquals(1, bitsBuffer.readBit());
    }

    @Test
    public void testWithByteOffset() {
        ByteBuf byteBuf = Unpooled.buffer(20).clear();

        // 在ByteBuf的前5个字节写入一些数据
        for (int i = 0; i < 5; i++) {
            byteBuf.setByte(i, (byte)0xFF);
        }

        // 从字节偏移5开始创建BitsBuffer
        BitsBuffer bitsBuffer = new BitsBufferImpl(byteBuf, 5, 40); // 从第5个字节开始，40位

        // 写入一些数据
        bitsBuffer.writeByte((byte) 0xAA); // 10101010
        bitsBuffer.writeByte((byte) 0x55); // 01010101

        // 验证原始ByteBuf中的数据
        assertEquals((byte) 0xFF, byteBuf.getByte(0)); // 前5个字节不变
        assertEquals((byte) 0xFF, byteBuf.getByte(1));
        assertEquals((byte) 0xFF, byteBuf.getByte(2));
        assertEquals((byte) 0xFF, byteBuf.getByte(3));
        assertEquals((byte) 0xFF, byteBuf.getByte(4));
        assertEquals((byte) 0xAA, byteBuf.getByte(5)); // 第6个字节变为0xAA
        assertEquals((byte) 0x55, byteBuf.getByte(6)); // 第7个字节变为0x55

        // 重新创建BitsBuffer进行读取测试
        bitsBuffer = new BitsBufferImpl(byteBuf, 5, 40);

        // 验证读取
        assertEquals(1, bitsBuffer.readBit());
        assertEquals(0, bitsBuffer.readBit());
        assertEquals(1, bitsBuffer.readBit());
        assertEquals(0, bitsBuffer.readBit());
        assertEquals(1, bitsBuffer.readBit());
        assertEquals(0, bitsBuffer.readBit());
        assertEquals(1, bitsBuffer.readBit());
        assertEquals(0, bitsBuffer.readBit());

        assertEquals(0, bitsBuffer.readBit()); // 开始读第二个字节
        assertEquals(1, bitsBuffer.readBit());
        assertEquals(0, bitsBuffer.readBit());
        assertEquals(1, bitsBuffer.readBit());
        assertEquals(0, bitsBuffer.readBit());
        assertEquals(1, bitsBuffer.readBit());
        assertEquals(0, bitsBuffer.readBit());
        assertEquals(1, bitsBuffer.readBit());
    }

    @Test
    public void testByteOffsetWithBitOffset() {
        ByteBuf byteBuf = Unpooled.buffer(20).clear();

        // 先创建一个从字节0开始的BitsBuffer并写入3位
        BitsBuffer initialBuffer = new BitsBufferImpl(byteBuf, 80);
        initialBuffer.writeBit((byte) 1);
        initialBuffer.writeBit((byte) 1);
        initialBuffer.writeBit((byte) 0);

        // 现在创建一个从字节1开始的BitsBuffer
        BitsBuffer offsetBuffer = new BitsBufferImpl(byteBuf, 1, 40);

        // 写入一个字节
        offsetBuffer.writeByte((byte) 0xAA); // 10101010

        // 验证原始数据正确性
        assertEquals(1, initialBuffer.getBit(0));
        assertEquals(1, initialBuffer.getBit(1));
        assertEquals(0, initialBuffer.getBit(2));

        // 第0个字节应该是11000000 (0xC0)
        byte firstByte = byteBuf.getByte(0);
        // 由于Java中byte是有符号的，0xC0在补码形式下是-64
        assertEquals((byte) 0xC0, firstByte);

        // 第1个字节应该是10101010
        assertEquals((byte) 0xAA, byteBuf.getByte(1));

        // 测试slice操作能否正确处理字节偏移
        BitsBuffer sliced = offsetBuffer.slice(2, 4); // 从第1字节的第2位开始，取4位

        // 验证slice的内容(应该是1010)
        assertEquals(1, sliced.getBit(0));
        assertEquals(0, sliced.getBit(1));
        assertEquals(1, sliced.getBit(2));
        assertEquals(0, sliced.getBit(3));
    }

    @Test
    public void testByteAlignedOperation() {
        ByteBuf byteBuf = Unpooled.buffer(10).clear();
        BitsBuffer bitsBuffer = new BitsBufferImpl(byteBuf, 80);

        // 在字节边界写入字节
        bitsBuffer.writeByte((byte) 0xAA); // 10101010

        // 验证
        assertEquals(1, bitsBuffer.getBit(0));
        assertEquals(0, bitsBuffer.getBit(1));
        assertEquals(1, bitsBuffer.getBit(2));
        assertEquals(0, bitsBuffer.getBit(3));
        assertEquals(1, bitsBuffer.getBit(4));
        assertEquals(0, bitsBuffer.getBit(5));
        assertEquals(1, bitsBuffer.getBit(6));
        assertEquals(0, bitsBuffer.getBit(7));

        // 第一个字节的内容应该是0xAA
        assertEquals((byte) 0xAA, byteBuf.getByte(0));
    }

    @Test
    public void testUnalignedByteOperation() {
        ByteBuf byteBuf = Unpooled.buffer(10).clear();
        BitsBuffer bitsBuffer = new BitsBufferImpl(byteBuf, 80);

        // 先写入3位，使后续写入不对齐
        bitsBuffer.writeBit((byte) 1);
        bitsBuffer.writeBit((byte) 1);
        bitsBuffer.writeBit((byte) 0);

        // 在非字节边界写入字节
        bitsBuffer.writeByte((byte) 0xAA); // 10101010

        // 验证: 第一个字节前3位是110，后5位是10101
        assertEquals(1, bitsBuffer.getBit(0));
        assertEquals(1, bitsBuffer.getBit(1));
        assertEquals(0, bitsBuffer.getBit(2));
        assertEquals(1, bitsBuffer.getBit(3));
        assertEquals(0, bitsBuffer.getBit(4));
        assertEquals(1, bitsBuffer.getBit(5));
        assertEquals(0, bitsBuffer.getBit(6));
        assertEquals(1, bitsBuffer.getBit(7));

        // 第二个字节前3位是010
        assertEquals(0, bitsBuffer.getBit(8));
        assertEquals(1, bitsBuffer.getBit(9));
        assertEquals(0, bitsBuffer.getBit(10));
    }

    @Test
    public void testAlignedIntOperation() {
        ByteBuf byteBuf = Unpooled.buffer(10).clear();
        BitsBuffer bitsBuffer = new BitsBufferImpl(byteBuf, 80);

        // 写入整型数据
        int value = 0x12345678;
        bitsBuffer.writeInt(value);

        // 验证每个字节
        assertEquals((byte) 0x12, byteBuf.getByte(0));
        assertEquals((byte) 0x34, byteBuf.getByte(1));
        assertEquals((byte) 0x56, byteBuf.getByte(2));
        assertEquals((byte) 0x78, byteBuf.getByte(3));
    }

    @Test
    public void testUnalignedIntOperation() {
        ByteBuf byteBuf = Unpooled.buffer(10).clear();
        BitsBuffer bitsBuffer = new BitsBufferImpl(byteBuf, 80);

        // 写入4位，使后续不对齐
        bitsBuffer.writeBit((byte) 1);
        bitsBuffer.writeBit((byte) 0);
        bitsBuffer.writeBit((byte) 1);
        bitsBuffer.writeBit((byte) 0);

        // 写入整型
        int value = 0x12345678;
        bitsBuffer.writeInt(value);

        // 读取并验证每一位
        for (int i = 0; i < 4; i++) { // 跳过前4位
            bitsBuffer.readBit();
        }

        // 按字节验证整数的位
        for (int b = 0; b < 4; b++) {
            int expectedByte = (value >> (24 - b * 8)) & 0xFF;
            for (int i = 0; i < 8; i++) {
                int expectedBit = (expectedByte >> (7 - i)) & 0x01;
                assertEquals("字节 " + b + ", 位 " + i + " 不匹配", expectedBit, bitsBuffer.readBit());
            }
        }
    }

    @Test
    public void testSlice() {
        ByteBuf byteBuf = Unpooled.buffer(10).clear();
        BitsBuffer bitsBuffer = new BitsBufferImpl(byteBuf, 80);

        // 写入一些数据
        bitsBuffer.writeByte((byte) 0xAA); // 10101010
        bitsBuffer.writeByte((byte) 0x55); // 01010101

        // 从中间切片
        BitsBuffer slice = bitsBuffer.slice(4, 8);

        // 验证切片长度
        assertEquals(8, slice.length());

        // 验证切片内容
        assertEquals(1, slice.getBit(0)); // 原始位置4的值
        assertEquals(0, slice.getBit(1)); // 原始位置5的值
        assertEquals(1, slice.getBit(2)); // 原始位置6的值
        assertEquals(0, slice.getBit(3)); // 原始位置7的值
        assertEquals(0, slice.getBit(4)); // 原始位置8的值
        assertEquals(1, slice.getBit(5)); // 原始位置9的值
        assertEquals(0, slice.getBit(6)); // 原始位置10的值
        assertEquals(1, slice.getBit(7)); // 原始位置11的值
    }

    @Test
    public void testReadBits() {
        ByteBuf byteBuf = Unpooled.buffer(10).clear();
        BitsBuffer bitsBuffer = new BitsBufferImpl(byteBuf, 80);

        // 写入两个字节的数据
        bitsBuffer.writeByte((byte) 0xAA); // 10101010
        bitsBuffer.writeByte((byte) 0x55); // 01010101

        // 重置读取索引
        bitsBuffer = new BitsBufferImpl(byteBuf, 80);

        // 读取4位
        BitsBuffer readBits = bitsBuffer.readBits(4);

        // 验证读取的内容
        assertEquals(4, readBits.length());
        assertEquals(1, readBits.getBit(0));
        assertEquals(0, readBits.getBit(1));
        assertEquals(1, readBits.getBit(2));
        assertEquals(0, readBits.getBit(3));

        // 验证原缓冲区读取索引已经移动
        assertEquals(1, bitsBuffer.readBit()); // 下一位应该是0
    }

    @Test
    public void testExceptions() {
        ByteBuf byteBuf = Unpooled.buffer(2).clear();
        BitsBuffer bitsBuffer = new BitsBufferImpl(byteBuf, 16); // 2字节 = 16位

        // 写满缓冲区
        for (int i = 0; i < 16; i++) {
            bitsBuffer.writeBit((byte) (i % 2));
        }

        // 写溢出应该抛出异常
        try {
            bitsBuffer.writeBit((byte) 1);
            fail("应该抛出IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // 期望的异常
        }

        // 大量写入应该抛出异常
        bitsBuffer = new BitsBufferImpl(byteBuf, 16);
        try {
            bitsBuffer.writeInt(0); // 32位 > 16位
            fail("应该抛出IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // 期望的异常
        }

        // 越界读取应该抛出异常
        bitsBuffer = new BitsBufferImpl(byteBuf, 16);
        for (int i = 0; i < 16; i++) {
            bitsBuffer.readBit();
        }
        try {
            bitsBuffer.readBit();
            fail("应该抛出IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // 期望的异常
        }

        // 越界getBit应该抛出异常
        try {
            bitsBuffer.getBit(16);
            fail("应该抛出IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // 期望的异常
        }

        try {
            bitsBuffer.getBit(-1);
            fail("应该抛出IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // 期望的异常
        }

        // 无效slice应该抛出异常
        try {
            bitsBuffer.slice(10, 10);
            fail("应该抛出IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // 期望的异常
        }

        // 测试参数验证
        try {
            new BitsBufferImpl(byteBuf, -1, 16);
            fail("应该抛出IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // 期望的异常
        }
    }
}