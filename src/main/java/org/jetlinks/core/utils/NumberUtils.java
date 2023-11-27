package org.jetlinks.core.utils;

import java.math.BigDecimal;
import java.math.BigInteger;

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
            value *= (long) Math.pow(10, digDiff);
        } else if (digDiff < 0) {
            value /= (long) Math.pow(10, Math.abs(digDiff));
        }
        return value;
    }

    /**
     * 获取数字的小数位数
     *
     * @param number 数字
     * @return 位数
     */
    public static int numberOfPlace(Number number) {
        if (isIntNumber(number)) {
            return 0;
        }
        BigDecimal decimal;
        if (number instanceof BigDecimal) {
            decimal = ((BigDecimal) number);
        } else {
            decimal = new BigDecimal(String.valueOf(number));
        }
        String str = decimal.stripTrailingZeros().toPlainString();
        int index = str.indexOf(".");
        return index < 0 ? 0 : str.length() - index - 1;
    }

    /**
     * 判断是否为整型数字
     *
     * @param number 数字
     * @return 是否为整型数字
     */
    public static boolean isIntNumber(Number number) {
        return number instanceof Integer ||
                number instanceof Long ||
                number instanceof Byte ||
                number instanceof Short ||
                number instanceof BigInteger;
    }
}
