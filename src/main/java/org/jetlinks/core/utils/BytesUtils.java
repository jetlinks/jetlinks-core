package org.jetlinks.core.utils;

/**
 * 字节转换工具
 *
 * @author zhouhao
 */
public class BytesUtils {

    /**
     * 高位字节数组转int,低字节在前.高位在后.
     * -------------------------------------
     * | 0-7位 | 8-15位 | 16-23位 | 24-31位 |
     * -------------------------------------
     *
     * @param src    数组
     * @param offset 偏移量
     * @param len    长度
     * @return 转换结果
     */
    public static int highBytesToInt(byte[] src, int offset, int len) {
        int n = 0;
        len = Math.min(len, 4);
        for (int i = 0; i < len; i++) {
            int left = i * 8;
            n += ((src[i + offset] & 0xFF) << left);
        }
        return n;
    }

    /**
     * 高位字节数组转long,低字节在前.高位在后.
     * ---------------------------------------------------------------------------
     * | 0-7位 | 8-15位 | 16-23位 | 24-31位 | 32-39位 | 40-47位 | 48-55位 | 56-63位 |
     * ---------------------------------------------------------------------------
     *
     * @param src    数组
     * @param offset 偏移量
     * @param len    长度
     * @return 转换结果
     */
    public static long highBytesToLong(byte[] src, int offset, int len) {
        long n = 0;
        len = Math.min(Math.min(len, src.length), 8);
        for (int i = 0; i < len; i++) {
            int left = i * 8;
            n += ((long) (src[i + offset] & 0xFF) << left);
        }
        return n;
    }

    public static int highBytesToInt(byte[] src) {
        return highBytesToInt(src, 0, src.length);
    }

    public static long highBytesToLong(byte[] src) {
        return highBytesToLong(src, 0, src.length);
    }


    /**
     * IEEE 754 字节数组转float, 低位字节在前,高位字节在后.
     *
     * @param src    数组
     * @param offset 偏移量
     * @param len    长度
     * @return 转换结果
     * @see this#highBytesToInt(byte[], int, int)
     * @see Float#intBitsToFloat(int)
     */
    public static float highBytesToFloat(byte[] src, int offset, int len) {
        return Float.intBitsToFloat(highBytesToInt(src, offset, len));
    }

    /**
     * IEEE 754 字节数组转double, 低位字节在前,高位字节在后.
     *
     * @param src    数组
     * @param offset 偏移量
     * @param len    长度
     * @return 转换结果
     * @see this#highBytesToLong(byte[], int, int)
     */
    public static double highBytesToDouble(byte[] src, int offset, int len) {
        return Double.longBitsToDouble(highBytesToLong(src, offset, len));
    }

    public static float highBytesToFloat(byte[] src) {
        return highBytesToFloat(src, 0, src.length);
    }

    public static double highBytesToDouble(byte[] src) {
        return highBytesToDouble(src, 0, src.length);
    }

    /**
     * 低位字节数组转int,高位字节在前,低字节在后.
     * ------------------------------------
     * | 31-24位 | 23-16位 | 15-8位 | 7-0位 |
     * ------------------------------------
     *
     * @param src    数组
     * @param offset 偏移量
     * @param len    长度
     * @return int值
     */
    public static int lowBytesToInt(byte[] src, int offset, int len) {
        int n = 0;
        len = Math.min(Math.min(len, src.length), 4);
        for (int i = 0; i < len; i++) {
            int left = i * 8;
            n += ((src[offset + len - i - 1] & 0xFF) << left);
        }
        return n;
    }

    /**
     * 低位字节数组转long,高位字节在前,低字节在后.
     * ---------------------------------------------------------------------------
     * | 63-56位 | 55-48位 | 47-40位 | 39-32位 | 31-24位 | 23-16位 | 15-8位 | 7-0位 |
     * ----------------------------------------------------------------------------
     *
     * @param src    数组
     * @param offset 偏移量
     * @param len    长度
     * @return int值
     */
    public static long lowBytesToLong(byte[] src, int offset, int len) {
        long n = 0;
        len = Math.min(Math.min(len, src.length), 8);
        for (int i = 0; i < len; i++) {
            int left = i * 8;
            n += ((long) (src[offset + len - i - 1] & 0xFF) << left);
        }
        return n;
    }

    public static float lowBytesToFloat(byte[] src, int offset, int len) {
        return Float.intBitsToFloat(lowBytesToInt(src, offset, len));
    }

    public static double lowBytesToDouble(byte[] src, int offset, int len) {
        return Double.longBitsToDouble(lowBytesToLong(src, offset, len));
    }


    /**
     * 低位字节数组转int,低字节在后
     * -------------------------------------
     * | 31-24位 | 23-17位 | 16-8位 | 7-0位 |
     * -------------------------------------
     *
     * @param src 字节数组
     * @return int值
     */
    public static int lowBytesToInt(byte[] src) {
        return lowBytesToInt(src, 0, src.length);
    }

    public static long lowBytesToLong(byte[] src) {
        return lowBytesToLong(src, 0, src.length);
    }

    public static float lowBytesToFloat(byte[] src) {
        return lowBytesToFloat(src, 0, src.length);
    }

    public static double lowBytesToDouble(byte[] src) {
        return lowBytesToDouble(src, 0, src.length);
    }


    /**
     * int转高位字节数组,低字节在前
     * ---------------------------------------
     * | 0-7位 | 8-15位 | 16-23位  |  24-31位 |
     * ---------------------------------------
     *
     * @param src 字节数组
     * @return bytes 值
     */
    public static byte[] toHighBytes(byte[] target, long src, int offset, int len) {
        for (int i = 0; i < len; i++) {
            target[offset + i] = (byte) (src >> (i * 8) & 0xff);
        }
        return target;
    }

    /**
     * int转高位字节数组,低字节在前
     * -------------------------------------------
     * |  0-7位  |  8-15位  |  16-23位  |  24-31位 |
     * -------------------------------------------
     *
     * @param src 字节数组
     * @return bytes 值
     */
    public static byte[] toHighBytes(int src) {
        return toHighBytes(new byte[4], src, 0, 4);
    }

    public static byte[] toHighBytes(long src) {
        return toHighBytes(new byte[8], src, 0, 8);
    }

    public static byte[] toHighBytes(double src) {
        return toHighBytes(Double.doubleToLongBits(src));
    }

    public static byte[] toHighBytes(float src) {
        return toHighBytes(Float.floatToIntBits(src));
    }

    /**
     * 转低位字节数组, 低字节在后
     * -------------------------------------
     * | 31-24位 | 23-16位 | 15-8位 | 7-0位 |
     * -------------------------------------
     *
     * @param src 字节数组
     * @return int值
     */
    public static byte[] toLowBytes(byte[] target, long src, int offset, int len) {
        for (int i = 0; i < len; i++) {
            target[offset + len - i - 1] = (byte) (src >> (i * 8) & 0xff);
        }
        return target;
    }

    /**
     * int转低位字节数组, 低字节在后
     * --------------------------------------------
     * |  31-24位 |  23-16位   |  15-8位 |   7-0位 |
     * --------------------------------------------
     *
     * @param src 字节数组
     * @return int值
     */
    public static byte[] toLowBytes(int src) {
        return toLowBytes(new byte[4], src, 0, 4);
    }


    public static byte[] toLowBytes(long src) {
        return toLowBytes(new byte[8], src, 0, 8);
    }


    public static byte[] toLowBytes(double src) {
        return toLowBytes(Double.doubleToLongBits(src));
    }

    public static byte[] toLowBytes(float src) {
        return toLowBytes(Float.floatToIntBits(src));
    }

}
