package org.jetlinks.core.codec.layout;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetlinks.core.codec.Codecs;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 模拟 PLC 数据解析场景测试
 */
public class PLCCodecScenarioTest {

    @Test
    public void testModbusFloatCDAB() {
        // 模拟 Modbus 读取 4 字节 Float，通常使用 CD AB 布局 (Little-endian words, Big-endian within words)
        // 或者是 ABCD 交换高低字。
        // 例如：Float 123.456 在标准大端 (ABCD) 是 0x42 F6 E9 79
        // 在 CDAB (Modbus 常用) 下是 0xE9 79 42 F6

        byte[] input = {(byte) 0xE9, (byte) 0x79, (byte) 0x42, (byte) 0xF6};
        ByteBuf buf = Unpooled.wrappedBuffer(input);

        // 1. 使用 ByteLayout 进行重排
        ByteLayout layout = ByteLayouts.CD_AB; // 或者使用 WORD_SWAP_2
        layout.reorder(buf);

        // 重排后应为 0x42 F6 E9 79
        assertEquals((byte)0x42, buf.getByte(0));
        assertEquals((byte)0xF6, buf.getByte(1));
        assertEquals((byte)0xE9, buf.getByte(2));
        assertEquals((byte)0x79, buf.getByte(3));

        // 2. 使用 Codec 进行解码
        Float val = (Float) Codecs.Internal.Ieee754Float32.decode(buf);

        assertEquals(123.456f, val, 0.001f);
    }

    @Test
    public void testModbusInt32DCBA() {
        // 模拟 4 字节整数，完全反转 (Little-endian)
        // 123456789 (0x07 5B CD 15) -> 0x15 CD 5B 07
        byte[] input = {0x15, (byte)0xCD, 0x5B, 0x07};
        ByteBuf buf = Unpooled.wrappedBuffer(input);

        ByteLayouts.LITTLE_ENDIAN.reorder(buf);

        Integer val = Codecs.Internal.INT32.decode(buf);
        assertEquals(Integer.valueOf(123456789), val);
    }

    @Test
    public void testScaledInt16() {
        // 模拟温度采集，寄存器值为 255 (0x00 FF)，缩放因子 0.1 -> 25.5
        byte[] input = {0x00, (byte)0xFF};
        ByteBuf buf = Unpooled.wrappedBuffer(input);

        // 假设是大端，不需要重排
        Float val = Codecs.Internal.FixedPointScaled10.decode(buf);
        assertEquals(25.5f, val, 0.01f);
    }
}
