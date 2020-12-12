package org.jetlinks.core.utils;

public class NumberUtils {

    /**
     * 将数字转为固定长度,超过长度截断,不足长度补0.
     *
     * <pre> fixLength(123,4) => 1230 </pre>
     * <pre> fixLength(123,2) => 12 </pre>
     *
     * @param value  值
     * @param length 长度
     * @return 长度值
     */
    public static long fixLength(long value, int length) {
        int len = (int) Math.log10(value) + 1;
        int digDiff = length - len;

        if (digDiff > 0) {
            value *= Math.pow(10, digDiff);
        } else if (digDiff < 0) {
            value /= Math.pow(10, Math.abs(digDiff));
        }
        return value;
    }
}
