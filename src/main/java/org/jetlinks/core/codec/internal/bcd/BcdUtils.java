package org.jetlinks.core.codec.internal.bcd;

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
}
