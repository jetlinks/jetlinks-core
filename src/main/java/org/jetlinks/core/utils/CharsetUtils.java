package org.jetlinks.core.utils;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;

/**
 * 字符集工具类，用于判断字符是否对人类友好、可打印
 */
public class CharsetUtils {

    /**
     * 判断整个ByteBuf中的内容是否为人类友好的可打印字符
     *
     * @param byteBuf 要检查的ByteBuf
     * @return 如果所有字符都是人类友好的则返回true，否则返回false
     */
    public static boolean isHumanFriendly(ByteBuf byteBuf) {
        if (byteBuf == null || byteBuf.readableBytes() == 0) {
            return true;
        }
        return isHumanFriendly(byteBuf, byteBuf.readerIndex(), byteBuf.readableBytes());
    }

    /**
     * 判断ByteBuf中指定范围的内容是否为人类友好的可打印字符
     *
     * @param byteBuf 要检查的ByteBuf
     * @param index   起始位置
     * @param length  检查长度
     * @return 如果指定范围内的所有字符都是人类友好的则返回true，否则返回false
     */
    public static boolean isHumanFriendly(ByteBuf byteBuf, int index, int length) {
        if (byteBuf == null || length <= 0) {
            return true;
        }

        // 检查索引范围是否有效
        if (index < 0 || index + length > byteBuf.capacity()) {
            return false;
        }

        try {
            // 将字节转换为UTF-8字符串进行检查
            byte[] bytes = new byte[length];
            byteBuf.getBytes(index, bytes);
            String str = new String(bytes, StandardCharsets.UTF_8);

            return isHumanFriendly(str);
        } catch (Throwable e) {
            // 如果转换失败，说明不是有效的UTF-8编码
            return false;
        }
    }

    /**
     * 判断字符串是否为人类友好的可打印字符
     *
     * @param str 要检查的字符串
     * @return 如果字符串中的所有字符都是人类友好的则返回true，否则返回false
     */
    public static boolean isHumanFriendly(String str) {
        if (str == null || str.isEmpty()) {
            return true;
        }
        for (int i = 0; i < str.length(); ) {
            int codePoint = str.codePointAt(i);
            if (!isHumanFriendly(codePoint)) {
                return false;
            }
            i += Character.charCount(codePoint);
        }

        return true;
    }

    /**
     * 判断字符数组是否为人类友好的可打印字符
     *
     * @param bytes 要检查的字节数组
     * @return 如果字节数组表示的字符串中所有字符都是人类友好的则返回true，否则返回false
     */
    public static boolean isHumanFriendly(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return true;
        }

        try {
            String str = new String(bytes, StandardCharsets.UTF_8);
            return isHumanFriendly(str);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断单个Unicode码点是否为人类友好的可打印字符
     *
     * @param codePoint Unicode码点
     * @return 如果码点表示人类友好的字符则返回true，否则返回false
     */
    public static boolean isHumanFriendly(int codePoint) {
        // 检查是否为有效的Unicode码点
        if (!Character.isValidCodePoint(codePoint)) {
            return false;
        }

        // 特殊处理一些常用的友好字符，优先判断
        if (isCommonWhitespace(codePoint)) {
            return true;
        }

        // 获取Unicode块
        Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
        if (block == null) {
            return false;
        }

        // 排除不友好的控制字符和特殊字符
        if (isControlOrSpecialBlock(block)) {
            return false;
        }

        // 检查字符类型
        int type = Character.getType(codePoint);

        // 排除私有使用区字符、代理字符、未分配字符
        if (type == Character.PRIVATE_USE ||
            type == Character.SURROGATE ||
            type == Character.UNASSIGNED) {
            return false;
        }

        // 对于格式字符，只允许一些特定的友好格式字符（如零宽连接符等）
        if (type == Character.FORMAT) {
            // 零宽连接符 (ZWJ) - 用于组合复杂Emoji
            if (codePoint == 0x200D) {
                return true;
            }
            // 零宽非连接符 (ZWNJ) - 用于某些语言的正确显示
            if (codePoint == 0x200C) {
                return true;
            }
            // 其他格式字符通常不友好
            return false;
        }

        // 对于控制字符，只排除非友好的控制字符
        if (type == Character.CONTROL) {
            // 已经在上面处理了友好的空白字符，这里排除其他控制字符
            return false;
        }

        // 排除其他不可见字符
        if (type == Character.LINE_SEPARATOR ||
            type == Character.PARAGRAPH_SEPARATOR ||
            type == Character.SPACE_SEPARATOR) {
            // 只允许常见的空白字符
            return codePoint == ' ' || codePoint == 0x00A0; // 普通空格和不间断空格
        }

        return true;
    }

    /**
     * 检查Unicode块是否为控制或特殊字符块
     */
    private static boolean isControlOrSpecialBlock(Character.UnicodeBlock block) {
        return block == Character.UnicodeBlock.SPECIALS || // � U+FFFD 等
               block == Character.UnicodeBlock.CONTROL_PICTURES ||
               block == Character.UnicodeBlock.PRIVATE_USE_AREA ||
               block == Character.UnicodeBlock.HIGH_SURROGATES ||
               block == Character.UnicodeBlock.HIGH_PRIVATE_USE_SURROGATES ||
               block == Character.UnicodeBlock.LOW_SURROGATES ||
               block == Character.UnicodeBlock.SUPPLEMENTARY_PRIVATE_USE_AREA_A ||
               block == Character.UnicodeBlock.SUPPLEMENTARY_PRIVATE_USE_AREA_B;
    }

    /**
     * 检查是否为常见的友好空白字符
     */
    private static boolean isCommonWhitespace(int codePoint) {
        return codePoint == ' ' ||      // 空格
               codePoint == '\t' ||     // 制表符
               codePoint == '\n' ||     // 换行符
               codePoint == '\r' ||     // 回车符
               codePoint == 0x00A0;     // 不间断空格
    }

    /**
     * 过滤字符串中的非人类友好字符，用替换字符替换
     *
     * @param str 原始字符串
     * @param replacement 替换字符，如果为null则直接删除非友好字符
     * @return 过滤后的字符串
     */
    public static String filterNonHumanFriendly(String str, String replacement) {
        if (str == null || str.isEmpty()) {
            return str;
        }

       return StringBuilderUtils.buildString(
            str,replacement,((_str, _replacement, sb) -> {
                for (int i = 0; i < _str.length(); ) {
                    int codePoint = _str.codePointAt(i);
                    if (isHumanFriendly(codePoint)) {
                        sb.appendCodePoint(codePoint);
                    } else if (_replacement != null) {
                        sb.append(_replacement);
                    }
                    i += Character.charCount(codePoint);
                }
            })
        );
    }

    /**
     * 过滤字符串中的非人类友好字符，直接删除
     *
     * @param str 原始字符串
     * @return 过滤后的字符串
     */
    public static String filterNonHumanFriendly(String str) {
        return filterNonHumanFriendly(str, null);
    }
}
