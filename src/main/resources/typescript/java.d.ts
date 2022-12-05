declare module java.lang {
    class JavaNumber extends Number {
        byteValue(): byte;

        shortValue(): short;

        intValue(): int;

        longValue(): long;

        floatValue(): float;

        doubleValue(): double;
    }

    class Character {
    }

    class Byte extends JavaNumber {
    }

    class Short extends JavaNumber {
    }

    class Integer extends JavaNumber {
    }

    class Long extends JavaNumber {
    }

    class Double extends JavaNumber {
    }

    class Float extends JavaNumber {
    }

}

declare module java.math {

    enum RoundingMode {
        UP,
        DOWN,
        CEILING,
        FLOOR,
        HALF_UP,
        HALF_DOWN,
        HALF_EVEN,
        UNNECESSARY
    }

    /**
     * 高精度数字类
     */
    class BigDecimal extends java.lang.JavaNumber {

        /**
         * 创建一个新的BigDecimal
         * @param value string | double | int | long
         */
        constructor(value: string | double | int | long);

        /**
         * 根据long或者double值创建一个BigDecimal
         * @param val value
         */
        static valueOf(val: long | double): java.math.BigDecimal;


        /**
         * 加法运算,并返回新的 BigDecimal.
         *
         *  bigDecimal = bigDecimal.add(anotherBigDecimal);
         *
         * @param augend 加数
         */
        add(augend: java.math.BigDecimal): java.math.BigDecimal;

        /**
         * 减法运算,并返回新的 BigDecimal.
         *
         *  bigDecimal = bigDecimal.subtract(anotherBigDecimal);
         *
         * @param subtrahend 减数
         */
        subtract(subtrahend: java.math.BigDecimal): java.math.BigDecimal;

        /**
         * 乘法运算,并返回新的 BigDecimal.
         *
         * bigDecimal = bigDecimal.multiply(anotherBigDecimal);
         *
         * @param multiplicand 乘数
         */
        multiply(multiplicand: java.math.BigDecimal): java.math.BigDecimal;

        /**
         * 除法运算,并返回新的 BigDecimal.
         *
         *  bigDecimal = bigDecimal.divide(anotherBigDecimal,2,java.math.RoundingMode.HALF_UP);
         *
         * @param divisor 除数
         * @param scale 保留小数位数
         * @param roundMod 保留小数位数的方式
         */
        divide(divisor: java.math.BigDecimal, scale: int, roundMod: java.math.RoundingMode): java.math.BigDecimal;

        /**
         * 返回当前 BigDecimal 的精度.
         */
        scale(): int;

        /**
         * 设置 BigDecimal 的精度,并返回新的BigDecimal.
         * @param scale 保留小数位数
         */
        setScale(scale: int): java.math.BigDecimal;

        /**
         * 设置 BigDecimal 的精度,并返回新的BigDecimal.
         * @param scale 保留小数位数
         * @param roundMod  保留小数位数的方式
         */
        setScale(scale: int, roundMod: java.math.RoundingMode): java.math.BigDecimal;
    }
}

/**
 * 8位整数
 */
declare interface byte extends java.lang.Byte {
}

/**
 * 16位整数
 */
declare interface short extends java.lang.Short {
}

/**
 * 单字符
 */
declare interface char extends java.lang.Character {
}

/**
 * 32位整数
 */
declare interface int extends java.lang.Integer {
}

/**
 * 64位整数
 */
declare interface long extends java.lang.Long {
}

/**
 * 32位浮点数
 */
declare interface float extends java.lang.Float {
}

/**
 * 64位浮点数
 */
declare interface double extends java.lang.Double {
}


declare module java.util {

    class Map<K, V> {
        get(key: K): V | null;

        getOrDefault(key: K, defaultValue: V): V;

        put(key: K, value: V): void;

        putIfAbsent(key: K, value: V): void;

        remove(key: K): V | null;

        forEach(call: (key: K, value: V) => void): void


    }

    class List<E> {
        size(): number;

        add(element: E): void;

        addAll(lst: List<E>): void;

        remove(element: E): void;

        forEach(callback: (element: E) => void): void;
    }

    class HashMap<K, V> extends Map<K, V> {

    }


    class ArrayList<E> extends List<E> {

    }

    class Optional<T> {

        static of<T>(val: T): Optional<T>;

        static ofNullable<T>(val: T): Optional<T>;

        filter(call: (value: T) => boolean): T;

        map<U>(call: (value: T) => U | null): U;

        flatMap<U>(call: (value: T) => Optional<U>): U;

        orElse<T>(defaultValue: T | null): T

        orElseGet(call: () => T): T;
    }

}

