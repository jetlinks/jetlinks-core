package org.jetlinks.core.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

/**
 * CharsetUtils 单元测试
 */
public class CharsetUtilsTest {

    @Test
    public void testIsHumanFriendlyWithCodePoint() {
        // 测试常见的可打印字符
        assertTrue("字母应该是人类友好的", CharsetUtils.isHumanFriendly('A'));
        assertTrue("数字应该是人类友好的", CharsetUtils.isHumanFriendly('1'));
        assertTrue("中文字符应该是人类友好的", CharsetUtils.isHumanFriendly('中'));
        assertTrue("标点符号应该是人类友好的", CharsetUtils.isHumanFriendly('!'));

        // 测试友好的空白字符
        assertTrue("空格应该是人类友好的", CharsetUtils.isHumanFriendly(' '));
        assertTrue("制表符应该是人类友好的", CharsetUtils.isHumanFriendly('\t'));
        assertTrue("换行符应该是人类友好的", CharsetUtils.isHumanFriendly('\n'));
        assertTrue("回车符应该是人类友好的", CharsetUtils.isHumanFriendly('\r'));
        assertTrue("不间断空格应该是人类友好的", CharsetUtils.isHumanFriendly(0x00A0));

        // 测试控制字符
        assertFalse("NULL字符不应该是人类友好的", CharsetUtils.isHumanFriendly(0));
        assertFalse("DEL字符不应该是人类友好的", CharsetUtils.isHumanFriendly(127));
        assertFalse("ESC字符不应该是人类友好的", CharsetUtils.isHumanFriendly(27));

        // 测试特殊Unicode字符
        assertFalse("替换字符不应该是人类友好的", CharsetUtils.isHumanFriendly(0xFFFD));
        assertFalse("私有使用区字符不应该是人类友好的", CharsetUtils.isHumanFriendly(0xE000));

        // 测试无效的码点
        assertFalse("无效码点不应该是人类友好的", CharsetUtils.isHumanFriendly(-1));
        assertFalse("超出范围的码点不应该是人类友好的", CharsetUtils.isHumanFriendly(0x110000));
    }

    @Test
    public void testIsHumanFriendlyWithString() {
        // 测试空字符串和null
        assertTrue("空字符串应该是人类友好的", CharsetUtils.isHumanFriendly(""));
        assertTrue("null字符串应该是人类友好的", CharsetUtils.isHumanFriendly((String) null));

        // 测试正常字符串
        assertTrue("英文字符串应该是人类友好的", CharsetUtils.isHumanFriendly("Hello World"));
        assertTrue("中文字符串应该是人类友好的", CharsetUtils.isHumanFriendly("你好世界"));
        assertTrue("混合字符串应该是人类友好的", CharsetUtils.isHumanFriendly("Hello 世界 123!"));
        assertTrue("包含换行的字符串应该是人类友好的", CharsetUtils.isHumanFriendly("Line1\nLine2"));
        assertTrue("包含制表符的字符串应该是人类友好的", CharsetUtils.isHumanFriendly("Col1\tCol2"));

        // 测试包含控制字符的字符串
        assertFalse("包含NULL的字符串不应该是人类友好的", CharsetUtils.isHumanFriendly("Hello\0World"));
        assertFalse("包含DEL的字符串不应该是人类友好的", CharsetUtils.isHumanFriendly("Hello\u007FWorld"));
        assertFalse("包含ESC的字符串不应该是人类友好的", CharsetUtils.isHumanFriendly("Hello\u001BWorld"));

        // 测试Emoji字符
        assertTrue("Emoji应该是人类友好的", CharsetUtils.isHumanFriendly("Hello 😊"));
        assertTrue("复杂Emoji应该是人类友好的", CharsetUtils.isHumanFriendly("👨‍👩‍👧‍👦"));
    }

    @Test
    public void testIsHumanFriendlyWithByteArray() {
        // 测试空数组和null
        assertTrue("空字节数组应该是人类友好的", CharsetUtils.isHumanFriendly((byte[]) null));
        assertTrue("空字节数组应该是人类友好的", CharsetUtils.isHumanFriendly(new byte[0]));

        // 测试正常字符串的字节
        assertTrue("英文字符串字节应该是人类友好的",
                   CharsetUtils.isHumanFriendly("Hello World".getBytes(StandardCharsets.UTF_8)));
        assertTrue("中文字符串字节应该是人类友好的",
                   CharsetUtils.isHumanFriendly("你好世界".getBytes(StandardCharsets.UTF_8)));

        // 测试包含控制字符的字节
        assertFalse("包含NULL的字节数组不应该是人类友好的",
                    CharsetUtils.isHumanFriendly("Hello\0World".getBytes(StandardCharsets.UTF_8)));

        // 测试无效的UTF-8字节序列
        assertFalse("无效UTF-8字节序列不应该是人类友好的",
                    CharsetUtils.isHumanFriendly(new byte[]{(byte) 0xFF, (byte) 0xFE}));
    }

    @Test
    public void testIsHumanFriendlyWithByteBuf() {
        // 测试空ByteBuf和null
        assertTrue("null ByteBuf应该是人类友好的", CharsetUtils.isHumanFriendly((ByteBuf) null));

        ByteBuf emptyBuf = Unpooled.buffer(0);
        assertTrue("空ByteBuf应该是人类友好的", CharsetUtils.isHumanFriendly(emptyBuf));
        emptyBuf.release();

        // 测试正常字符串的ByteBuf
        ByteBuf englishBuf = Unpooled.copiedBuffer("Hello World", StandardCharsets.UTF_8);
        assertTrue("英文字符串ByteBuf应该是人类友好的", CharsetUtils.isHumanFriendly(englishBuf));
        englishBuf.release();

        ByteBuf chineseBuf = Unpooled.copiedBuffer("你好世界", StandardCharsets.UTF_8);
        assertTrue("中文字符串ByteBuf应该是人类友好的", CharsetUtils.isHumanFriendly(chineseBuf));
        chineseBuf.release();

        // 测试包含控制字符的ByteBuf
        ByteBuf controlBuf = Unpooled.copiedBuffer("Hello\0World", StandardCharsets.UTF_8);
        assertFalse("包含控制字符的ByteBuf不应该是人类友好的", CharsetUtils.isHumanFriendly(controlBuf));
        controlBuf.release();

        // 测试包含换行符的ByteBuf
        ByteBuf newlineBuf = Unpooled.copiedBuffer("Line1\nLine2", StandardCharsets.UTF_8);
        assertTrue("包含换行符的ByteBuf应该是人类友好的", CharsetUtils.isHumanFriendly(newlineBuf));
        newlineBuf.release();
    }

    @Test
    public void testIsHumanFriendlyWithByteBufRange() {
        ByteBuf buf = Unpooled.copiedBuffer("Hello\0World", StandardCharsets.UTF_8);

        // 测试有效范围
        assertTrue("Hello部分应该是人类友好的", CharsetUtils.isHumanFriendly(buf, 0, 5));
        assertFalse("包含NULL的部分不应该是人类友好的", CharsetUtils.isHumanFriendly(buf, 0, 6));
        assertTrue("World部分应该是人类友好的", CharsetUtils.isHumanFriendly(buf, 6, 5));

        // 测试边界条件
        assertTrue("长度为0应该是人类友好的", CharsetUtils.isHumanFriendly(buf, 0, 0));
        assertFalse("负数索引不应该是人类友好的", CharsetUtils.isHumanFriendly(buf, -1, 5));
        assertFalse("超出范围不应该是人类友好的", CharsetUtils.isHumanFriendly(buf, 0, 100));

        buf.release();
    }

    @Test
    public void testFilterNonHumanFriendly() {
        // 测试null和空字符串
        assertNull("null应该返回null", CharsetUtils.filterNonHumanFriendly(null));
        assertEquals("空字符串应该返回空字符串", "", CharsetUtils.filterNonHumanFriendly(""));

        // 测试不需要过滤的字符串
        String cleanString = "Hello World 你好世界 123!";
        assertEquals("干净的字符串应该保持不变", cleanString, CharsetUtils.filterNonHumanFriendly(cleanString));

        // 测试需要过滤的字符串
        String dirtyString = "Hello\0World\u001B!";
        String expectedFiltered = "HelloWorld!";
        assertEquals("应该过滤掉控制字符", expectedFiltered, CharsetUtils.filterNonHumanFriendly(dirtyString));

        // 测试带替换字符的过滤
        String expectedReplaced = "Hello?World?!";
        assertEquals("应该用?替换控制字符", expectedReplaced,
                     CharsetUtils.filterNonHumanFriendly(dirtyString, "?"));

        // 测试保留友好的空白字符
        String whitespaceString = "Line1\nLine2\tCol";
        assertEquals("应该保留友好的空白字符", whitespaceString,
                     CharsetUtils.filterNonHumanFriendly(whitespaceString));
    }

    @Test
    public void testUnicodeSupport() {
        // 测试各种Unicode字符
        assertTrue("日文字符应该是人类友好的", CharsetUtils.isHumanFriendly("こんにちは"));
        assertTrue("韩文字符应该是人类友好的", CharsetUtils.isHumanFriendly("안녕하세요"));
        assertTrue("阿拉伯文字符应该是人类友好的", CharsetUtils.isHumanFriendly("مرحبا"));
        assertTrue("俄文字符应该是人类友好的", CharsetUtils.isHumanFriendly("Привет"));
        assertTrue("希腊文字符应该是人类友好的", CharsetUtils.isHumanFriendly("Γεια σας"));

        // 测试数学符号
        assertTrue("数学符号应该是人类友好的", CharsetUtils.isHumanFriendly("∑∏∫√"));

        // 测试货币符号
        assertTrue("货币符号应该是人类友好的", CharsetUtils.isHumanFriendly("$€¥£"));

        // 测试箭头符号
        assertTrue("箭头符号应该是人类友好的", CharsetUtils.isHumanFriendly("←→↑↓"));
    }

    @Test
    public void testEdgeCases() {
        // 测试高位代理对（Emoji等）
        String emojiString = "👋🌍";
        assertTrue("Emoji字符串应该是人类友好的", CharsetUtils.isHumanFriendly(emojiString));

        ByteBuf emojiBuf = Unpooled.copiedBuffer(emojiString, StandardCharsets.UTF_8);
        assertTrue("Emoji ByteBuf应该是人类友好的", CharsetUtils.isHumanFriendly(emojiBuf));
        emojiBuf.release();

        // 测试长字符串
        StringBuilder longString = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longString.append("测试");
        }
        assertTrue("长字符串应该是人类友好的", CharsetUtils.isHumanFriendly(longString.toString()));

        // 测试混合内容
        String mixedContent = "正常文本\u0000控制字符\u001B更多文本";
        assertFalse("混合内容包含控制字符时不应该是人类友好的", CharsetUtils.isHumanFriendly(mixedContent));

        String filtered = CharsetUtils.filterNonHumanFriendly(mixedContent);
        assertEquals("过滤后应该只剩正常文本", "正常文本控制字符更多文本", filtered);
    }

    @Test
    public void testSpecialWhitespaceCharacters() {
        // 测试各种空白字符
        assertTrue("普通空格应该是人类友好的", CharsetUtils.isHumanFriendly(' '));
        assertTrue("制表符应该是人类友好的", CharsetUtils.isHumanFriendly('\t'));
        assertTrue("换行符应该是人类友好的", CharsetUtils.isHumanFriendly('\n'));
        assertTrue("回车符应该是人类友好的", CharsetUtils.isHumanFriendly('\r'));
        assertTrue("不间断空格应该是人类友好的", CharsetUtils.isHumanFriendly('\u00A0'));

        // 测试其他空白字符（应该被过滤）
        assertFalse("垂直制表符不应该是人类友好的", CharsetUtils.isHumanFriendly('\u000B'));
        assertFalse("换页符不应该是人类友好的", CharsetUtils.isHumanFriendly('\u000C'));
    }

    @Test
    public void testPerformance() {
        // 性能测试 - 确保方法能够快速处理大量数据
        StringBuilder largeText = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeText.append("这是一个性能测试字符串，包含中文和English字符。");
        }

        long startTime = System.currentTimeMillis();
        boolean result = CharsetUtils.isHumanFriendly(largeText.toString());
        long endTime = System.currentTimeMillis();

        assertTrue("大文本应该是人类友好的", result);
        assertTrue("处理时间应该在合理范围内", (endTime - startTime) < 1000); // 小于1秒
    }
}