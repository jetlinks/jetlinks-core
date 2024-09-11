package org.jetlinks.core.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

public class NumberUtils {

    /**
     * 转换为有效精度的浮点数
     * <ul>
     *     <li>convertEffectiveScale(0.001234,2) -> 0.0012</li>
     *     <li>convertEffectiveScale(1.2132,2) -> 1.21</li>
     * </ul>
     *
     * @param origin 原始数字
     * @param scale  有效精度
     * @return 转换后的值
     */
    public static double convertEffectiveScale(double origin, int scale) {
        if (origin == 0) {
            return origin;
        }
        // 取整数部分
        double integerPart;
        if (origin > 0) {
            integerPart = Math.floor(origin);
        } else {
            integerPart = Math.ceil(origin);
        }
        // 取小数部分
        double decimalPart = origin - integerPart;
        // 小数部分为0直接返回
        if (decimalPart == 0) {
            return origin;
        }
        //原始值小于-1或者大于1,不用计算有效精度.
        if (integerPart != 0) {
            return BigDecimal
                .valueOf(origin)
                .setScale(scale, RoundingMode.HALF_UP)
                .doubleValue();
        }
        int n = scale;
        double temp = decimalPart;
        if (temp < 0) {
            for (; temp > -1 ; n++) {
                temp *= 10;
            }
        } else {
            for (;temp < 1 ; n++) {
                temp *= 10;
            }
        }
        return new BigDecimal(decimalPart)
            .setScale(n - 1, RoundingMode.HALF_UP)
            .doubleValue();
    }

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
