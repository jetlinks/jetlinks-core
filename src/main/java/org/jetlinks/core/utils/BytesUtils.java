package org.jetlinks.core.utils;

/**
 * 字节转换工具
 *
 * @author zhouhao
 */
public class BytesUtils {

    /**
     * 小端模式转int,低字节在前.高在后.
     * -------------------------------------
     * | 0-7位 | 8-15位 | 16-23位 | 24-31位 |
     * -------------------------------------
     *
     * @param src    数组
     * @param offset 偏移量
     * @param len    长度
     * @return 转换结果
     */
    public static int leToInt(byte[] src, int offset, int len) {
        int n = 0;
        len = Math.min(len, 4);
        for (int i = 0; i < len; i++) {
            int left = i * 8;
            n += ((src[i + offset] & 0xFF) << left);
        }
        return n;
    }

    public static int leToInt(byte[] src) {
        return leToInt(src, 0, src.length);
    }

    /**
     * 小端模式转long,低字节在前.高在后.
     * ---------------------------------------------------------------------------
     * | 0-7位 | 8-15位 | 16-23位 | 24-31位 | 32-39位 | 40-47位 | 48-55位 | 56-63位 |
     * ---------------------------------------------------------------------------
     *
     * @param src    数组
     * @param offset 偏移量
     * @param len    长度
     * @return 转换结果
     */
    public static long leToLong(byte[] src, int offset, int len) {
        long n = 0;
        len = Math.min(Math.min(len, src.length), 8);
        for (int i = 0; i < len; i++) {
            int left = i * 8;
            n += ((long) (src[i + offset] & 0xFF) << left);
        }
        return n;
    }

    public static long leToLong(byte[] src) {
        return highBytesToLong(src, 0, src.length);
    }

    @Deprecated
    public static int highBytesToInt(byte[] src, int offset, int len) {
        return leToInt(src, offset, len);
    }

    @Deprecated
    public static long highBytesToLong(byte[] src, int offset, int len) {
        return leToLong(src, offset, len);
    }
    @Deprecated
    public static int highBytesToInt(byte[] src) {
        return highBytesToInt(src, 0, src.length);
    }

    @Deprecated
    public static long highBytesToLong(byte[] src) {
        return highBytesToLong(src, 0, src.length);
    }


    /**
     * 小端模式,IEEE 754 字节数组转float,低位字节在前,高位字节在后.
     *
     * @param src    数组
     * @param offset 偏移量
     * @param len    长度
     * @return 转换结果
     * @see this#highBytesToInt(byte[], int, int)
     * @see Float#intBitsToFloat(int)
     */
    public static float leToFloat(byte[] src, int offset, int len) {
        return Float.intBitsToFloat(leToInt(src, offset, len));
    }

    public static float leToFloat(byte[] src) {
        return leToFloat(src, 0, src.length);
    }

    /**
     * 小端模式,IEEE 754 字节数组转double,低位字节在前,高位字节在后.
     *
     * @param src    数组
     * @param offset 偏移量
     * @param len    长度
     * @return 转换结果
     * @see this#highBytesToLong(byte[], int, int)
     */
    public static double leToDouble(byte[] src, int offset, int len) {
        return Double.longBitsToDouble(leToLong(src, offset, len));
    }

    public static double leToDouble(byte[] src) {
        return leToDouble(src, 0, src.length);
    }

    @Deprecated
    public static float highBytesToFloat(byte[] src, int offset, int len) {
        return leToFloat(src, offset, len);
    }

    @Deprecated
    public static double highBytesToDouble(byte[] src, int offset, int len) {
        return Double.longBitsToDouble(highBytesToLong(src, offset, len));
    }

    @Deprecated
    public static float highBytesToFloat(byte[] src) {
        return leToFloat(src, 0, src.length);
    }

    @Deprecated
    public static double highBytesToDouble(byte[] src) {
        return highBytesToDouble(src, 0, src.length);
    }

    /**
     * 大端模式,字节数组转int,高位字节在前,低字节在后.
     * ------------------------------------
     * | 31-24位 | 23-16位 | 15-8位 | 7-0位 |
     * ------------------------------------
     *
     * @param src    数组
     * @param offset 偏移量
     * @param len    长度
     * @return int值
     */
    public static int beToInt(byte[] src, int offset, int len) {
        int n = 0;
        len = Math.min(Math.min(len, src.length), 4);
        for (int i = 0; i < len; i++) {
            int left = i * 8;
            n += ((src[offset + len - i - 1] & 0xFF) << left);
        }
        return n;
    }

    @Deprecated
    public static int lowBytesToInt(byte[] src, int offset, int len) {
        return beToInt(src, offset, len);
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
    public static long beToLong(byte[] src, int offset, int len) {
        long n = 0;
        len = Math.min(Math.min(len, src.length), 8);
        for (int i = 0; i < len; i++) {
            int left = i * 8;
            n += ((long) (src[offset + len - i - 1] & 0xFF) << left);
        }
        return n;
    }


    public static float beToFloat(byte[] src, int offset, int len) {
        return Float.intBitsToFloat(beToInt(src, offset, len));
    }

    public static double beToDouble(byte[] src, int offset, int len) {
        return Double.longBitsToDouble(beToLong(src, offset, len));
    }

    @Deprecated
    public static long lowBytesToLong(byte[] src, int offset, int len) {
        return beToLong(src, offset, len);
    }

    @Deprecated
    public static float lowBytesToFloat(byte[] src, int offset, int len) {
        return Float.intBitsToFloat(lowBytesToInt(src, offset, len));
    }

    @Deprecated
    public static double lowBytesToDouble(byte[] src, int offset, int len) {
        return beToDouble(src, offset, len);
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
    public static int beToInt(byte[] src) {
        return beToInt(src, 0, src.length);
    }

    @Deprecated
    public static int lowBytesToInt(byte[] src) {
        return lowBytesToInt(src, 0, src.length);
    }

    public static long beToLong(byte[] src) {
        return beToLong(src, 0, src.length);
    }

    @Deprecated
    public static long lowBytesToLong(byte[] src) {
        return lowBytesToLong(src, 0, src.length);
    }

    public static float beToFloat(byte[] src) {
        return beToFloat(src, 0, src.length);
    }

    @Deprecated
    public static float lowBytesToFloat(byte[] src) {
        return lowBytesToFloat(src, 0, src.length);
    }

    public static double beToDouble(byte[] src) {
        return beToDouble(src, 0, src.length);
    }

    @Deprecated
    public static double lowBytesToDouble(byte[] src) {
        return lowBytesToDouble(src, 0, src.length);
    }


    /**
     * int转小端模式字节数组,低字节在前
     * ---------------------------------------
     * | 0-7位 | 8-15位 | 16-23位  |  24-31位 |
     * ---------------------------------------
     *
     * @param number 数字值
     * @param target 目标数组
     * @param offset 偏移量
     * @param len    总长度
     * @return bytes 原始值
     */
    public static byte[] numberToLe(byte[] target, Number number, int offset, int len) {
        long src = number.longValue();
        for (int i = 0; i < len; i++) {
            target[offset + i] = (byte) (src >> (i * 8) & 0xff);
        }
        return target;
    }

    @Deprecated
    public static byte[] toHighBytes(byte[] target, long src, int offset, int len) {
        return numberToBe(target, src, offset, len);
    }

    /**
     * int转小端模式字节数组,低字节在前
     * -------------------------------------------
     * |  0-7位  |  8-15位  |  16-23位  |  24-31位 |
     * -------------------------------------------
     *
     * @param number 数字值
     * @param target 目标数组
     * @param offset 填充偏移量
     * @return bytes 原始值
     */
    public static byte[] intToLe(byte[] target, int number, int offset) {
        return numberToLe(target, number, offset, 4);
    }

    public static byte[] intToLe(int src) {
        return intToLe(new byte[4], src, 0);
    }

    public static byte[] shortToLe(byte[] target, short number, int offset) {
        return numberToLe(target, number, offset, 2);
    }

    public static byte[] shortToLe(short src) {
        return shortToLe(new byte[2], src, 0);
    }

    public static byte[] longToLe(byte[] target, long number, int offset) {
        return numberToLe(target, number, offset, 8);
    }

    public static byte[] longToLe(long src) {
        return longToLe(new byte[8], src, 0);
    }

    public static byte[] floatToLe(byte[] target, float number, int offset) {
        return intToBe(target, Float.floatToIntBits(number), offset);
    }

    public static byte[] floatToLe(float src) {
        return intToLe(Float.floatToIntBits(src));
    }

    public static byte[] doubleToLe(byte[] target, double number, int offset) {
        return longToLe(target, Double.doubleToLongBits(number), offset);
    }

    public static byte[] doubleToLe(double src) {
        return longToLe(Double.doubleToLongBits(src));
    }

    @Deprecated
    public static byte[] toHighBytes(int src) {
        return intToLe(src);
    }

    @Deprecated
    public static byte[] toHighBytes(long src) {
        return longToLe(src);
    }

    @Deprecated
    public static byte[] toHighBytes(double src) {
        return doubleToLe(src);
    }

    @Deprecated
    public static byte[] toHighBytes(float src) {
        return intToLe(Float.floatToIntBits(src));
    }


    /**
     * 数字大端模式转字节数组, 低字节在后
     * -------------------------------------
     * | 31-24位 | 23-16位 | 15-8位 | 7-0位 |
     * -------------------------------------
     *
     * @param number 数字值
     * @param target 目标数组
     * @param offset 偏移量
     * @param len    总长度
     * @return int值
     */
    public static byte[] numberToBe(byte[] target, Number number, int offset, int len) {
        long src = number.longValue();
        for (int i = 0; i < len; i++) {
            target[offset + len - i - 1] = (byte) (src >> (i * 8) & 0xff);
        }
        return target;
    }

    @Deprecated
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
     * @param number 数字值
     * @param target 目标数组
     * @param offset 偏移量
     * @return bytes 原始值
     */
    public static byte[] intToBe(byte[] target, int number, int offset) {
        return numberToBe(target, number, offset, 4);
    }

    public static byte[] intToBe(int src) {
        return intToBe(new byte[4], src, 0);
    }

    public static byte[] shortToBe(byte[] target, short number, int offset) {
        return numberToBe(target, number, offset, 2);
    }

    public static byte[] shortToBe(short src) {
        return shortToBe(new byte[2], src, 0);
    }

    public static byte[] longToBe(byte[] target, long number, int offset) {
        return numberToBe(target, number, offset, 8);
    }

    public static byte[] longToBe(long src) {
        return longToBe(new byte[8], src, 0);
    }

    public static byte[] floatToBe(byte[] target, float number, int offset) {
        return intToBe(target, Float.floatToIntBits(number), offset);
    }

    public static byte[] floatToBe(float src) {
        return intToBe(Float.floatToIntBits(src));
    }

    public static byte[] doubleToBe(byte[] target, double number, int offset) {
        return longToBe(target, Double.doubleToLongBits(number), offset);
    }

    public static byte[] doubleToBe(double src) {
        return longToBe(Double.doubleToLongBits(src));
    }

    /**
     * 反转数组. [4,3,2,1] => [1,2,3,4]
     *
     * @param data   数据
     * @param offset 偏移量
     * @param length 长度
     * @return 转换后到数组
     */
    public static byte[] reverse(byte[] data, int offset, int length) {

        int len = Math.min(data.length - 1, length);
        for (int i = 0; i < length / 2; i++) {
            int idx = i + offset;
            int rev = len - i;
            byte temp = data[idx];
            data[idx] = data[rev];
            data[rev] = temp;
        }

        return data;
    }

    public static byte[] reverse(byte[] data) {
        return reverse(data, 0, data.length);
    }


    @Deprecated
    public static byte[] toLowBytes(int src) {
        return toLowBytes(new byte[4], src, 0, 4);
    }

    @Deprecated
    public static byte[] toLowBytes(long src) {
        return toLowBytes(new byte[8], src, 0, 8);
    }

    @Deprecated
    public static byte[] toLowBytes(double src) {
        return toLowBytes(Double.doubleToLongBits(src));
    }

    @Deprecated
    public static byte[] toLowBytes(float src) {
        return toLowBytes(Float.floatToIntBits(src));
    }


}
