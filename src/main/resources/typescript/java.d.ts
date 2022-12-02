
declare module java.lang {
    class JavaNumber extends Number {
        byteValue(): Byte;

        shortValue(): Short;

        intValue(): Integer;

        longValue(): Long;

        floatValue(): Float;

        doubleValue(): Double;
    }

    class Character {}
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


declare interface byte extends java.lang.Byte {
}

declare interface short extends java.lang.Short {
}

declare interface char extends java.lang.Character{}

declare interface int extends java.lang.Integer {
}

declare interface long extends java.lang.Long {
}

declare interface float extends java.lang.Float {
}

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

