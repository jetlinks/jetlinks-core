package org.jetlinks.core.codec.internal.bcd;

import org.jetlinks.core.utils.StringBuilderUtils;

/**
 * BCD (Binary-Coded Decimal) 转换工具类.
 * 支持 Packed BCD (打包 BCD) 和 Unpacked BCD (非打包 BCD) 两种编码格式.
 * <p>
 * Packed BCD: 每 4 位 (bit) 表示一个 0-9 的十进制数字, 一个字节存储两个数字.
 * <p>
 * Unpacked BCD: 每个字节存储一个 0-9 的十进制数字, 高位通常补 0.
 *
 * @author zhouhao
 * @since 1.2
 */
public class BcdUtils {

    /**
     * 将 16 位 Packed BCD 编码解码为整数.
     * 例如: 0x1234 -> 1234
     *
     * @param bcd 16 位 BCD 编码
     * @return 解码后的十进制整数
     */
    public static int decodeBcd(short bcd) {
        return ((bcd >> 12) & 0xF) * 1000 +
            ((bcd >> 8) & 0xF) * 100 +
            ((bcd >> 4) & 0xF) * 10 +
            (bcd & 0xF);
    }

    /**
     * 将十进制整数编码为 16 位 Packed BCD.
     * 例如: 1234 -> 0x1234
     *
     * @param val 待编码的整数 (0-9999)
     * @return 16 位 BCD 编码
     */
    public static short encodeBcd(int val) {
        return (short) (((val / 1000) << 12) |
            ((val % 1000 / 100) << 8) |
            ((val % 100 / 10) << 4) |
            (val % 10));
    }

    /**
     * 将 8 位 Packed BCD 编码解码为整数.
     * 例如: 0x12 -> 12
     *
     * @param bcd 8 位 BCD 编码
     * @return 解码后的十进制整数
     */
    public static int decodeBcd(byte bcd) {
        return ((bcd >> 4) & 0xF) * 10 +
            (bcd & 0xF);
    }

    /**
     * 将十进制整数编码为 8 位 Packed BCD.
     * 例如: 12 -> 0x12
     *
     * @param val 待编码的整数 (0-99)
     * @return 8 位 BCD 编码
     */
    public static byte encodeBcd8(int val) {
        return (byte) (((val / 10 % 10) << 4) |
            (val % 10));
    }

    /**
     * 将 32 位 Packed BCD 编码解码为整数.
     * 例如: 0x12345678 -> 12345678
     *
     * @param bcd 32 位 BCD 编码
     * @return 解码后的十进制整数
     */
    public static int decodeBcd(int bcd) {
        return ((bcd >> 28) & 0xF) * 10000000 +
            ((bcd >> 24) & 0xF) * 1000000 +
            ((bcd >> 20) & 0xF) * 100000 +
            ((bcd >> 16) & 0xF) * 10000 +
            ((bcd >> 12) & 0xF) * 1000 +
            ((bcd >> 8) & 0xF) * 100 +
            ((bcd >> 4) & 0xF) * 10 +
            (bcd & 0xF);
    }

    /**
     * 将十进制整数编码为 32 位 Packed BCD.
     * 例如: 12345678 -> 0x12345678
     *
     * @param val 待编码的整数 (0-99999999)
     * @return 32 位 BCD 编码
     */
    public static int encodeBcd32(int val) {
        return (((val / 10000000) % 10) << 28) |
            (((val / 1000000) % 10) << 24) |
            (((val / 100000) % 10) << 20) |
            (((val / 10000) % 10) << 16) |
            (((val / 1000) % 10) << 12) |
            (((val / 100) % 10) << 8) |
            (((val / 10) % 10) << 4) |
            (val % 10);
    }

    /**
     * 将 48 位 Packed BCD 编码解码为长整数.
     * 例如: 0x123456789012L -> 123456789012L
     *
     * @param bcd 48 位 BCD 编码
     * @return 解码后的十进制长整数
     */
    public static long decodeBcd64(long bcd) {
        return ((bcd >> 44) & 0xF) * 100000000000L +
            ((bcd >> 40) & 0xF) * 10000000000L +
            ((bcd >> 36) & 0xF) * 1000000000L +
            ((bcd >> 32) & 0xF) * 100000000L +
            ((bcd >> 28) & 0xF) * 10000000L +
            ((bcd >> 24) & 0xF) * 1000000L +
            ((bcd >> 20) & 0xF) * 100000L +
            ((bcd >> 16) & 0xF) * 10000L +
            ((bcd >> 12) & 0xF) * 1000L +
            ((bcd >> 8) & 0xF) * 100L +
            ((bcd >> 4) & 0xF) * 10L +
            (bcd & 0xF);
    }

    /**
     * 将十进制长整数编码为 48 位 Packed BCD.
     * 例如: 123456789012L -> 0x123456789012L
     *
     * @param val 待编码的长整数 (0-999999999999)
     * @return 48 位 BCD 编码
     */
    public static long encodeBcd48(long val) {
        return (((val / 100000000000L) % 10) << 44) |
            (((val / 10000000000L) % 10) << 40) |
            (((val / 1000000000L) % 10) << 36) |
            (((val / 100000000L) % 10) << 32) |
            (((val / 10000000L) % 10) << 28) |
            (((val / 1000000L) % 10) << 24) |
            (((val / 100000L) % 10) << 20) |
            (((val / 10000L) % 10) << 16) |
            (((val / 1000L) % 10) << 12) |
            (((val / 100L) % 10) << 8) |
            (((val / 10L) % 10) << 4) |
            (val % 10);
    }

    /**
     * 将 16 位 Unpacked BCD 编码解码为整数.
     * 每个字节存储一个十进制数字. 例如: 0x0102 -> 12
     *
     * @param bcd 16 位非打包 BCD 编码
     * @return 解码后的十进制整数
     */
    public static int decodeUnpackedBcd(short bcd) {
        return ((bcd >> 8) & 0xF) * 10 + (bcd & 0xF);
    }

    /**
     * 将十进制整数编码为 16 位 Unpacked BCD.
     * 每个字节存储一个十进制数字. 例如: 12 -> 0x0102
     *
     * @param val 待编码的整数 (0-99)
     * @return 16 位非打包 BCD 编码
     */
    public static short encodeUnpackedBcd16(int val) {
        return (short) (((val / 10 % 10) << 8) | (val % 10));
    }

    /**
     * 将 32 位 Unpacked BCD 编码解码为整数.
     * 每个字节存储一个十进制数字. 例如: 0x01020304 -> 1234
     *
     * @param bcd 32 位非打包 BCD 编码
     * @return 解码后的十进制整数
     */
    public static int decodeUnpackedBcd(int bcd) {
        return ((bcd >> 24) & 0xF) * 1000 +
            ((bcd >> 16) & 0xF) * 100 +
            ((bcd >> 8) & 0xF) * 10 +
            (bcd & 0xF);
    }

    /**
     * 将十进制整数编码为 32 位 Unpacked BCD.
     * 每个字节存储一个十进制数字. 例如: 1234 -> 0x01020304
     *
     * @param val 待编码的整数 (0-9999)
     * @return 32 位非打包 BCD 编码
     */
    public static int encodeUnpackedBcd32(int val) {
        return (((val / 1000) % 10) << 24) |
            (((val / 100) % 10) << 16) |
            (((val / 10) % 10) << 8) |
            (val % 10);
    }

    /**
     * 从 {@link io.netty.buffer.ByteBuf} 中读取指定长度的 Packed BCD 编码并转换为字符串.
     * <p>
     * 例如: 0x12, 0x34 -> "1234"
     *
     * @param buf        ByteBuf
     * @param byteLength 读取的字节长度
     * @return 解码后的字符串
     */
    public static String readBcdString(io.netty.buffer.ByteBuf buf, int byteLength) {
        return readBcdString(buf, byteLength, byteLength * 2);
    }

    /**
     * 从 {@link io.netty.buffer.ByteBuf} 中读取指定长度的 Packed BCD 编码并转换为指定位数的字符串.
     * <p>
     * 例如: 0x01, 0x34, digitLength=3 -> "134"
     * <p>
     * 如果 {@code digitLength} 小于等于 0, 则自动舍弃开头的 '0'.
     *
     * @param buf         ByteBuf
     * @param byteLength  读取的字节长度
     * @param digitLength 实际的数字位数, 如果 <= 0 则自动舍弃开头的 '0'
     * @return 解码后的字符串
     */
    public static String readBcdString(io.netty.buffer.ByteBuf buf, int byteLength, int digitLength) {
        return StringBuilderUtils.buildString(
            buf, byteLength, digitLength,
            (b, len, dl, builder) -> {
                int skip = dl > 0 ? (len * 2 - dl) : -1;
                boolean started = false;
                for (int i = 0; i < len; i++) {
                    byte val = b.readByte();
                    for (int j = 0; j < 2; j++) {
                        int nibble = (j == 0) ? ((val >> 4) & 0x0F) : (val & 0x0F);
                        int index = i * 2 + j;
                        if (skip >= 0) {
                            if (index >= skip) {
                                builder.append((char) (nibble + '0'));
                            }
                        } else {
                            if (nibble != 0 || started || index == len * 2 - 1) {
                                builder.append((char) (nibble + '0'));
                                started = true;
                            }
                        }
                    }
                }
            });
    }

    /**
     * 将字符串编码为 Packed BCD 并写入 {@link io.netty.buffer.ByteBuf}.
     * <p>
     * 例如: "1234" -> 0x12, 0x34
     *
     * @param buf        ByteBuf
     * @param value      待编码的字符串 (仅包含数字)
     * @param byteLength 写入的字节长度, 如果字符串长度不足则在左侧补 0.
     */
    public static void writeBcdString(io.netty.buffer.ByteBuf buf, String value, int byteLength) {
        int strLen = value.length();
        int maxLen = byteLength * 2;
        for (int i = 0; i < byteLength; i++) {
            int highIdx = strLen - (maxLen - (i * 2));
            int lowIdx = strLen - (maxLen - (i * 2 + 1));

            int high = highIdx >= 0 ? value.charAt(highIdx) - '0' : 0;
            int low = lowIdx >= 0 ? value.charAt(lowIdx) - '0' : 0;

            buf.writeByte(((high & 0x0F) << 4) | (low & 0x0F));
        }
    }
}
