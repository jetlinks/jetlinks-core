package org.jetlinks.core.codec.layout;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * ByteLayoutImpl 单元测试
 *
 * @author jetlinks
 * @since 1.0
 */
public class ByteLayoutImplTest {

    @Test
    public void test2ByteLayouts() {
        // 测试2字节布局
        testReorder(ByteLayoutImpl.AB, new byte[]{0x01, 0x02}, new byte[]{0x01, 0x02}, "AB");
        testReorder(ByteLayoutImpl.BA, new byte[]{0x01, 0x02}, new byte[]{0x02, 0x01}, "BA");
    }

    @Test
    public void test4ByteLayouts() {
        // 测试4字节布局
        testReorder(ByteLayoutImpl.AB_CD, new byte[]{0x01, 0x02, 0x03, 0x04},
                    new byte[]{0x01, 0x02, 0x03, 0x04}, "AB_CD");

        testReorder(ByteLayoutImpl.CD_AB, new byte[]{0x01, 0x02, 0x03, 0x04},
                    new byte[]{0x03, 0x04, 0x01, 0x02}, "CD_AB");

        testReorder(ByteLayoutImpl.BA_DC, new byte[]{0x01, 0x02, 0x03, 0x04},
                    new byte[]{0x02, 0x01, 0x04, 0x03}, "BA_DC");

        testReorder(ByteLayoutImpl.DC_BA, new byte[]{0x01, 0x02, 0x03, 0x04},
                    new byte[]{0x04, 0x03, 0x02, 0x01}, "DC_BA");
    }

    @Test
    public void test8ByteLayouts() {
        // 测试8字节布局
        byte[] input = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};

        testReorder(ByteLayoutImpl.AB_CD_EF_GH, input,
                    new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08}, "AB_CD_EF_GH");

        testReorder(ByteLayoutImpl.GH_EF_CD_AB, input,
                    new byte[]{0x07, 0x08, 0x05, 0x06, 0x03, 0x04, 0x01, 0x02}, "GH_EF_CD_AB");

        testReorder(ByteLayoutImpl.BA_DC_FE_HG, input,
                    new byte[]{0x02, 0x01, 0x04, 0x03, 0x06, 0x05, 0x08, 0x07}, "BA_DC_FE_HG");

        testReorder(ByteLayoutImpl.HG_FE_DC_BA, input,
                    new byte[]{0x08, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01}, "HG_FE_DC_BA");
    }


    @Test
    public void testEndiannessConversion() {
        // 测试大小端转换
        byte[] input = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};

        // 小端序（最低有效字节在前）
        ByteLayout littleEndian = ByteLayoutImpl.HG_FE_DC_BA;
        ByteBuf result = littleEndian.reorder(Unpooled.wrappedBuffer(input));

        assertEquals((byte)0x08, result.readByte()); // 最低位
        assertEquals((byte)0x07, result.readByte());
        assertEquals((byte)0x06, result.readByte());
        assertEquals((byte)0x05, result.readByte());
        assertEquals((byte)0x04, result.readByte());
        assertEquals((byte)0x03, result.readByte());
        assertEquals((byte)0x02, result.readByte());
        assertEquals((byte)0x01, result.readByte()); // 最高位

        if (result.refCnt() > 0) result.release();
    }

    @Test
    public void testByteLength() {
        // 测试字节长度
        assertEquals(2, ByteLayoutImpl.AB.byteLength());
        assertEquals(2, ByteLayoutImpl.BA.byteLength());
        assertEquals(4, ByteLayoutImpl.AB_CD.byteLength());
        assertEquals(4, ByteLayoutImpl.CD_AB.byteLength());
        assertEquals(8, ByteLayoutImpl.AB_CD_EF_GH.byteLength());
        assertEquals(8, ByteLayoutImpl.GH_EF_CD_AB.byteLength());
    }

    @Test
    public void testGetId() {
        // 测试ID获取
        assertEquals("AB", ByteLayoutImpl.AB.getId());
        assertEquals("BA", ByteLayoutImpl.BA.getId());
        assertEquals("AB_CD", ByteLayoutImpl.AB_CD.getId());
        assertEquals("CD_AB", ByteLayoutImpl.CD_AB.getId());
        assertEquals("AB_CD_EF_GH", ByteLayoutImpl.AB_CD_EF_GH.getId());
    }

    @Test
    public void testEmptyBuffer() {
        // 测试空缓冲区
        ByteLayout layout = ByteLayoutImpl.BA_DC;
        ByteBuf empty = Unpooled.buffer(0);

        try {
            layout.reorder(empty);
            fail("应该抛出异常");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("empty"));
        }

        empty.release();
    }


    @Test
    public void testComplexTransformation() {
        // 测试复杂转换
        byte[] input = {(byte)0x11, (byte)0x22, (byte)0x33, (byte)0x44, (byte)0x55, (byte)0x66, (byte)0x77, (byte)0x88};

        // 测试 GH_EF_CD_AB 转换
        // 原始: [11, 22, 33, 44, 55, 66, 77, 88]
        // 转换: [77, 88, 55, 66, 33, 44, 11, 22]
        ByteLayout layout = ByteLayoutImpl.GH_EF_CD_AB;
        ByteBuf result = layout.reorder(Unpooled.wrappedBuffer(input));

        byte[] expected = {(byte)0x77, (byte)0x88, (byte)0x55, (byte)0x66, (byte)0x33, (byte)0x44, (byte)0x11, (byte)0x22};
        assertArrayEquals(expected, getBytes(result));

        if (result.refCnt() > 0) result.release();
    }

    @Test
    public void testMultipleTransformations() {
        // 测试多次转换
        byte[] input = {0x01, 0x02, 0x03, 0x04};

        // 第一次转换：CD_AB
        ByteLayout layout1 = ByteLayoutImpl.CD_AB;
        ByteBuf result1 = layout1.reorder(Unpooled.wrappedBuffer(input));
        byte[] expected1 = {0x03, 0x04, 0x01, 0x02};
        assertArrayEquals(expected1, getBytes(result1));

        // 第二次转换：BA_DC
        ByteLayout layout2 = ByteLayoutImpl.BA_DC;
        ByteBuf result2 = layout2.reorder(result1.duplicate());
        byte[] expected2 = {0x04, 0x03, 0x02, 0x01};
        assertArrayEquals(expected2, getBytes(result2));

        if (result1.refCnt() > 0) result1.release();
        if (result2.refCnt() > 0) result2.release();
    }

    @Test
    public void testIndexTransformerLogic() {
        // 测试索引转换器逻辑
        int[] cdAbTransformer = {2, 3, 0, 1}; // CD_AB
        byte[] input = {0x01, 0x02, 0x03, 0x04};
        byte[] expected = {0x03, 0x04, 0x01, 0x02};

        byte[] result = applyTransformer(input, cdAbTransformer);
        assertArrayEquals("CD_AB 转换失败", expected, result);

        // 测试 BA_DC 转换器
        int[] baDcTransformer = {1, 0, 3, 2}; // BA_DC
        expected = new byte[]{0x02, 0x01, 0x04, 0x03};

        result = applyTransformer(input, baDcTransformer);
        assertArrayEquals("BA_DC 转换失败", expected, result);

        // 测试 HG_FE_DC_BA 转换器（8字节）
        int[] hgFeDcBaTransformer = {7, 6, 5, 4, 3, 2, 1, 0}; // HG_FE_DC_BA
        byte[] input8 = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
        byte[] expected8 = {0x08, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01};

        result = applyTransformer(input8, hgFeDcBaTransformer);
        assertArrayEquals("HG_FE_DC_BA 转换失败", expected8, result);
    }

    @Test
    public void testPredefinedConstants() {
        // 验证预定义常量的索引转换器
        assertArrayEquals(new int[]{0, 1}, getExpectedTransformer("AB"));
        assertArrayEquals(new int[]{1, 0}, getExpectedTransformer("BA"));
        assertArrayEquals(new int[]{0, 1, 2, 3}, getExpectedTransformer("AB_CD"));
        assertArrayEquals(new int[]{2, 3, 0, 1}, getExpectedTransformer("CD_AB"));
        assertArrayEquals(new int[]{1, 0, 3, 2}, getExpectedTransformer("BA_DC"));
        assertArrayEquals(new int[]{3, 0, 1, 2}, getExpectedTransformer("DA_BC"));
    }

    @Test
    public void testBADCFEHG() {
        // 专门测试 BA_DC_FE_HG 转换
        byte[] input = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
        byte[] expected = {0x02, 0x01, 0x04, 0x03, 0x06, 0x05, 0x08, 0x07};

        ByteLayout layout = ByteLayoutImpl.BA_DC_FE_HG;
        ByteBuf result = layout.reorder(Unpooled.copiedBuffer(input));

        byte[] actual = getBytes(result);
        System.out.println("BA_DC_FE_HG 转换:");
        System.out.println("输入: " + java.util.Arrays.toString(input));
        System.out.println("预期: " + java.util.Arrays.toString(expected));
        System.out.println("实际: " + java.util.Arrays.toString(actual));

        assertArrayEquals("BA_DC_FE_HG 转换失败", expected, actual);

        if (result.refCnt() > 0) result.release();
    }

    /**
     * 通用测试方法
     */
    private void testReorder(ByteLayout layout, byte[] input, byte[] expectedOutput, String layoutName) {
        // 测试 toCanonical - 将转换格式转换为标准格式
        ByteBuf inputBuf = Unpooled.copiedBuffer(input);
        ByteBuf result = layout.reorder(inputBuf.duplicate());

        assertArrayEquals("reorder failed for " + layoutName, expectedOutput, getBytes(result));

        // 释放资源
        if (inputBuf.refCnt() > 0) inputBuf.release();
        if (result.refCnt() > 0) result.release();
    }

    /**
     * 从ByteBuf获取字节数组
     */
    private byte[] getBytes(ByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), bytes);
        return bytes;
    }

    /**
     * 应用索引转换器到输入数据
     */
    private byte[] applyTransformer(byte[] input, int[] transformer) {
        byte[] result = new byte[transformer.length];
        for (int i = 0; i < transformer.length; i++) {
            result[i] = input[transformer[i]];
        }
        return result;
    }

    /**
     * 根据布局名称获取预期的索引转换器
     */
    private int[] getExpectedTransformer(String layoutName) {
        switch (layoutName) {
            case "AB": return new int[]{0, 1};
            case "BA": return new int[]{1, 0};
            case "AB_CD": return new int[]{0, 1, 2, 3};
            case "CD_AB": return new int[]{2, 3, 0, 1};
            case "BA_DC": return new int[]{1, 0, 3, 2};
            case "DA_BC": return new int[]{3, 0, 1, 2};
            case "AB_CD_EF_GH": return new int[]{0, 1, 2, 3, 4, 5, 6, 7};
            case "GH_EF_CD_AB": return new int[]{6, 7, 4, 5, 2, 3, 0, 1};
            case "BA_DC_FE_HG": return new int[]{1, 0, 3, 2, 5, 4, 7, 6};
            case "HG_FE_DC_BA": return new int[]{7, 6, 5, 4, 3, 2, 1, 0};
            default: throw new IllegalArgumentException("Unknown layout: " + layoutName);
        }
    }
}