declare module java.lang {

    // @ts-ignore
    class Object extends Function {



        private arguments: any;
        private caller: any;
        private length: any;
        private prototype: any;
        private name: any;

        private toString(): string;

        private apply<T>(this: new () => T, thisArg: T): void;
        private apply<T, A extends any[]>(this: new (...args: A) => T, thisArg: T, args: A): void;

        private call<T, A extends any[]>(this: new (...args: A) => T, thisArg: T, ...args: A): void;

        private bind<T>(this: T, thisArg: any): T;
        private bind<A extends any[], B extends any[], R>(this: new (...args: [...A, ...B]) => R, thisArg: any, ...args: A): new (...args: B) => R;

    }

    // @ts-ignore
    class JavaNumber extends Number {

        private static arguments: any;
        private static caller: any;
        private static length: any;
        private static prototype: any;
        private static name: any;

        private static EPSILON: number;
        private static MAX_SAFE_INTEGER: number;
        private static MIN_SAFE_INTEGER: number;

        private static NaN: number;
        private static NEGATIVE_INFINITY: number;

        private static POSITIVE_INFINITY: number;

        /**
         * 获取基本类型 byte 表示的值。
         *
         * @returns {byte} - 基本类型 byte 表示的值。
         */
        byteValue(): byte;

        /**
         * 获取基本类型 short 表示的值。
         *
         * @returns {short} - 基本类型 short 表示的值。
         */
        shortValue(): short;

        /**
         * 获取基本类型 int 表示的值。
         *
         * @returns {int} - 基本类型 int 表示的值。
         */
        intValue(): int;

        /**
         * 获取基本类型 long 表示的值。
         *
         * @returns {long} - 基本类型 long 表示的值。
         */
        longValue(): long;

        /**
         * 获取基本类型 float 表示的值。
         *
         * @returns {float} - 基本类型 float 表示的值。
         */
        floatValue(): float;

        /**
         * 获取基本类型 double 表示的值。
         *
         * @returns {double} - 基本类型 double 表示的值。
         */
        doubleValue(): double;

        private static apply<T>(this: new () => T, thisArg: T): void;
        private static apply<T, A extends any[]>(this: new (...args: A) => T, thisArg: T, args: A): void;

        private static call<T, A extends any[]>(this: new (...args: A) => T, thisArg: T, ...args: A): void;

        private static bind<T>(this: T, thisArg: any): T;
        private static bind<A extends any[], B extends any[], R>(this: new (...args: [...A, ...B]) => R, thisArg: any, ...args: A): new (...args: B) => R;

    }

    class Character extends java.lang.Object {
        private static prototype: any;

        // @ts-ignore
        public static readonly MAX_VALUE: char;
        // @ts-ignore
        public static readonly MIN_VALUE: char;
    }

    // @ts-ignore
    class Byte extends JavaNumber {
        private static prototype: any;
        // @ts-ignore
        public static readonly MAX_VALUE: byte;
        // @ts-ignore
        public static readonly MIN_VALUE: byte;
    }

    // @ts-ignore
    class Short extends JavaNumber {
        private static prototype: any;
        // @ts-ignore
        public static readonly MAX_VALUE: short;
        // @ts-ignore
        public static readonly MIN_VALUE: short;
    }

    // @ts-ignore
    class Integer extends JavaNumber {
        private static prototype: any;
        // @ts-ignore
        public static readonly MAX_VALUE: int;
        // @ts-ignore
        public static readonly MIN_VALUE: int;
    }

    // @ts-ignore
    class Long extends JavaNumber {
        private static prototype: any;
        // @ts-ignore
        public static MAX_VALUE: long;
        // @ts-ignore
        public static MIN_VALUE: long;
    }

    // @ts-ignore
    class Double extends JavaNumber {
        private static prototype: any;
        // @ts-ignore
        public static readonly MAX_VALUE: double;
        // @ts-ignore
        public static readonly MIN_VALUE: double;
    }

    // @ts-ignore
    class Float extends JavaNumber {
        private static prototype: any;
        // @ts-ignore
        public static readonly MAX_VALUE: float;
        // @ts-ignore
        public static readonly MIN_VALUE: float;
    }


    interface Iterable<E> extends java.lang.Object {

        forEach(action: (element: E) => void): void;

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
        private static prototype: any;

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

    import Entry = java.util.Map.Entry;

    module stream {
        // @ts-ignore
        class Stream<E> extends java.lang.Object {

            /**
             * 过滤流中的元素，仅保留满足给定条件的元素。
             * @param {(element: E) => boolean} call - 用于过滤的条件函数。
             * @returns {Stream<E>} - 包含满足条件的元素的新流。
             */
            filter(call: (element: E) => boolean): Stream<E>;

            /**
             * 将流中的每个元素映射为另一种类型。
             * @template U - 映射后的元素类型。
             * @param {(element: E) => U | null} call - 用于映射的函数。
             * @returns {Stream<U>} - 包含映射后元素的新流。
             */
            map<U>(call: (element: E) => U | null): Stream<U>;

            /**
             * 将流中的每个元素映射为另一个流，并将这些流连接成一个新流。
             * @template U - 映射后的元素类型。
             * @param {(element: E) => Stream<U>} call - 用于映射的函数，返回一个流。
             * @returns {Stream<U>} - 包含映射后元素的新流。
             */
            flatMap<U>(call: (element: E) => Stream<U>): Stream<U>;

            /**
             * 遍历流中的每个元素并执行指定的操作。
             * @param {(element: E) => void} call - 要执行的操作。
             */
            forEach(call: (element: E) => void): void;

            /**
             * 将流中的元素转换为数组。
             * @returns {E[]} - 包含流中元素的数组。
             */
            toArray(): E[];

            /**
             * 返回流中的第一个元素的 Optional 包装。
             * @returns {java.util.Optional<E>} - 包含第一个元素的 Optional 对象。
             */
            findFirst(): java.util.Optional<E>;

            /**
             * 返回流中的任意元素的 Optional 包装。
             * @returns {java.util.Optional<E>} - 包含任意元素的 Optional 对象。
             */
            findAny(): java.util.Optional<E>;

            /**
             * 将流中的元素使用给定的收集器进行收集。
             * ```js
             * var set = list
             *   .stream()
             *   .collect(java.util.stream.Collectors.toSet());
             * ```
             * @template R - 收集的结果类型。
             * @template A - 中间结果类型。
             * @param {Collector<E, A, R>} collector - 用于收集元素的收集器。
             * @returns {R} - 收集的结果。
             */
            collect<R, A>(collector: Collector<E, A, R>): R;

            /**
             * 将流中的元素使用给定的供应商、累加器和组合器进行收集。
             * @template R - 收集的结果类型。
             * @param {() => R} supplier - 提供结果容器的函数。
             * @param {(r: R, e: E) => void} accumulator - 将元素添加到结果容器的函数。
             * @param {(r: R, r2: R) => void} combiner - 合并两个结果容器的函数。
             * @returns {R} - 收集的结果。
             */
            collect<R>(supplier: () => R,
                       accumulator: (r: R, e: E) => void,
                       combiner: (r: R, r2: R) => void): R;

            /**
             * 使用给定的累加器函数逐个减少流中的元素。
             * ```js
             *  var val = list
             *      .stream()
             *      .reduce((a,b)=>a+b)
             *      .orElse(0);
             * ```
             * @param {(r: E, e: E) => E} accumulator - 用于逐个减少元素的累加器函数。
             * @returns {java.util.Optional<E>} - 包含减少的结果的 Optional 对象。
             */
            reduce(accumulator: (r: E, e: E) => E): java.util.Optional<E>;

            /**
             * 使用给定的身份元素和累加器函数逐个减少流中的元素。
             * @param {E} identity - 身份元素，用于初始化累加器的值。
             * @param {(r: E, e: E) => E} accumulator - 用于逐个减少元素的累加器函数。
             * @returns {E} - 减少的结果。
             */
            reduce(identity: E,
                   accumulator: (r: E, e: E) => E): E;

            /**
             * 使用给定的身份元素、累加器函数和组合器函数逐个减少流中的元素。
             * @template R - 减少的结果类型。
             * @param {R} identity - 身份元素，用于初始化累加器的值。
             * @param {(r: R, e: E) => R} accumulator - 用于逐个减少元素的累加器函数。
             * @param {(r: R, r2: R) => void} combiner - 合并两个累加器的函数。
             * @returns {R} - 减少的结果。
             */
            reduce<R>(identity: R,
                      accumulator: (r: R, e: E) => R,
                      combiner: (r: R, r2: R) => void): R;
        }

        /**
         * @see java.stream.Collectors
         */
        interface Collector<T, A, R> extends java.lang.Object {

            supplier(): () => A;

            accumulator(): (a: A, e: T) => void;

            combiner(): (a: A, a2: A) => void;

            finisher(): (a: A) => R;

        }

        /**
         * 提供用于收集流元素的静态工具类 Collectors。
         */
        class Collectors extends java.lang.Object {

            /**
             * 创建一个收集器，将流元素收集到列表中。
             * @template T - 流元素的类型。
             * @template R - 列表元素的类型。
             * @returns {Collector<T, any, java.util.List<R>>} - 列表类型的收集器。
             */
            static toList<T, R>(): Collector<T, any, java.util.List<R>>;

            /**
             * 创建一个收集器，将流元素收集到集合中。
             * @template T - 流元素的类型。
             * @template R - 集合元素的类型。
             * @returns {Collector<T, any, java.util.Set<R>>} - 集合类型的收集器。
             */
            static toSet<T, R>(): Collector<T, any, java.util.Set<R>>;

            /**
             * 创建一个收集器，将流元素按指定的键值映射函数收集到 Map 中。
             * @template T - 流元素的类型。
             * @template K - Map 键的类型。
             * @template U - Map 值的类型。
             * @param {(e: T) => K} keyMapper - 用于映射键的函数。
             * @param {(e: T) => U} valueMapper - 用于映射值的函数。
             * @param {(left: U, right: U) => U} mergeFunction - 用于合并值的函数。
             * @returns {Collector<T, any, java.util.Map<K, U>>} - Map 类型的收集器。
             */
            static toMap<T, K, U>(keyMapper: (e: T) => K,
                                  valueMapper: (e: T) => U,
                                  mergeFunction: (left: U, right: U) => U): Collector<T, any, java.util.Map<K, U>>;

            /**
             * 创建一个收集器，将流元素按指定的键值映射函数收集到 Map 中。
             * @template T - 流元素的类型。
             * @template K - Map 键的类型。
             * @template U - Map 值的类型。
             * @param {(e: T) => K} keyMapper - 用于映射键的函数。
             * @param {(e: T) => U} valueMapper - 用于映射值的函数。
             * @returns {Collector<T, any, java.util.Map<K, U>>} - Map 类型的收集器。
             */
            static toMap<T, K, U>(keyMapper: (e: T) => K,
                                  valueMapper: (e: T) => U): Collector<T, any, java.util.Map<K, U>>;
        }


    }

    namespace Map {
        interface Entry<K, V> extends java.lang.Object {

            getKey(): K;

            getValue(): V;

            setValue(value: V): void;

        }
    }

    export class Map<K, V> extends java.lang.Object {

        /**
         * 根据键获取映射中的值。
         * @param {K} key - 要查找的键。
         * @returns {V | null} - 与键关联的值，如果键不存在则返回 null。
         */
        get(key: K): V | null;

        /**
         * 根据键获取映射中的值，如果键不存在则返回默认值。
         * @param {K} key - 要查找的键。
         * @param {V} defaultValue - 键不存在时返回的默认值。
         * @returns {V} - 与键关联的值，如果键不存在则返回默认值。
         */
        getOrDefault(key: K, defaultValue: V): V;

        /**
         * 将键值对添加到映射中。
         * @param {K} key - 要添加的键。
         * @param {V} value - 要添加的值。
         */
        put(key: K, value: V): void;

        /**
         * 当且仅当映射中不存在指定键时，才将指定值与指定键关联。
         * @param {K} key - 要添加的键。
         * @param {V} value - 要添加的值。
         */
        putIfAbsent(key: K, value: V): void;

        /**
         * 当且仅当映射中不存在指定键时，才使用提供的函数计算值并将其与指定键关联。
         * @param {K} key - 要计算值的键。
         * @param {(key: K) => V} call - 用于计算值的函数。
         * @returns {V} - 与键关联的值。
         */
        computeIfAbsent(key: K, call: (key: K) => V): V;

        /**
         * 当且仅当映射中存在指定键时，才使用提供的函数计算值并将其与指定键关联。
         * @param {K} key - 要计算值的键。
         * @param {(key: K, value: V) => V} call - 用于计算值的函数。
         * @returns {V} - 与键关联的值。
         */
        computeIfPresent(key: K, call: (key: K, value: V) => V): V;

        /**
         * 检查映射是否包含指定键。
         * @param {K} key - 要检查的键。
         * @returns {boolean} - 如果映射包含指定键，则返回 true；否则返回 false。
         */
        containsKey(key: K): boolean;

        /**
         * 检查映射是否包含指定值。
         * @param {V} value - 要检查的值。
         * @returns {boolean} - 如果映射包含指定值，则返回 true；否则返回 false。
         */
        containsValue(value: V): boolean;

        /**
         * 清空映射，移除所有键值对。
         */
        clear(): void;

        /**
         * 根据提供的函数计算值并将其与指定键关联。
         * @param {K} key - 要计算值的键。
         * @param {(key: K, value: V) => V} call - 用于计算值的函数。
         * @returns {V} - 与键关联的新值。
         */
        compute(key: K, call: (key: K, value: V) => V): V;

        /**
         * 移除映射中指定键的值。
         * @param {K} key - 要移除的键。
         * @returns {V | null} - 与键关联的值，如果键不存在则返回 null。
         */
        remove(key: K): V | null;

        /**
         * 遍历映射中的每个键值对并执行指定的操作。
         * @param {(key: K, value: V) => void} call - 要执行的操作。
         */
        forEach(call: (key: K, value: V) => void): void;

        /**
         * 获取映射中的元素数量。
         * @returns {number} - 映射中的元素数量。
         */
        size(): number;

        /**
         * 获取映射的键值对集合。
         * @returns {java.util.Set<Entry<K, V>>} - 包含映射键值对的集合。
         */
        entrySet(): java.util.Set<Entry<K, V>>;

        /**
         * 获取映射的键集合。
         * @returns {java.util.Set<K>} - 包含映射键的集合。
         */
        keySet(): java.util.Set<K>;

        /**
         * 获取映射的值集合。
         * @returns {java.util.Collection<V>} - 包含映射值的集合。
         */
        values(): java.util.Collection<V>;
    }

    // @ts-ignore
    export class Collection<E> extends Array<E> implements java.lang.Iterable<E> {

        private length: number;

        private concat(...items: ConcatArray<E>[]): E[];

        private concat(...items: (E | ConcatArray<E>)[]): E[];

        private copyWithin(target: number, start: number, end?: number): this;

        // @ts-ignore
        private entries(): IterableIterator<[number, E]>;

        private every<S extends E>(predicate: (value: E, index: number, array: E[]) => value is S, thisArg?: any): this is S[];

        private find(predicate: (value: E, index: number, obj: E[]) => boolean, thisArg?: any): E | undefined;

        private findIndex(predicate: (value: E, index: number, obj: E[]) => boolean, thisArg?: any): number;

        // @ts-ignore
        private keys(): IterableIterator<number>;

        private fill(value: E, start?: number, end?: number): this;

        private filter<S extends E>(predicate: (value: E, index: number, array: E[]) => value is S, thisArg?: any): S[];

        private indexOf(searchElement: E, fromIndex?: number): number;

        private lastIndexOf(searchElement: E, fromIndex?: number): number;

        // @ts-ignore
        private join(separator?: string): void;

        private map<U>(callbackfn: (value: E, index: number, array: E[]) => U, thisArg?: any): U[];

        private pop(): E | undefined;

        private push(...items: E[]): number;

        private reduce(callbackfn: (previousValue: E, currentValue: E, currentIndex: number, array: E[]) => E): E;

        private reduceRight(callbackfn: (previousValue: E, currentValue: E, currentIndex: number, array: E[]) => E): E;

        private reverse(): E[];

        private shift(): E | undefined;

        private unshift(...items: E[]): number;

        private slice(start?: number, end?: number): E[];

        private some(predicate: (value: E, index: number, array: E[]) => boolean, thisArg?: any): boolean;

        private includes(searchElement: E, fromIndex?: number): boolean;
        private flatMap(callback: (t: this, value: any, index: number, array: any[]) => E | readonly E[], thisArg?: this): boolean;
        private flat<U,D>(t: this,depth?: D): U[];

        private toLocaleString(): string ;

        // @ts-ignore
        private values(): IterableIterator<E>;

        private splice(start: number, deleteCount?: number): E[] ;

        private indexOf(searchElement: E, fromIndex?: number): number;

        private lastIndexOf(searchElement: E, fromIndex?: number): number;


        private static arguments: any;
        private static caller: any;
        private static length: any;
        private static prototype: any;
        private static name: any;

        private static from(): any;

        private static isArray(): any;

        private static of(): any;

        private static toString(): string;

        private static apply<T>(this: new () => T, thisArg: T): void;
        private static apply<T, A extends any[]>(this: new (...args: A) => T, thisArg: T, args: A): void;

        private static call<T, A extends any[]>(this: new (...args: A) => T, thisArg: T, ...args: A): void;

        private static bind<T>(this: T, thisArg: any): T;
        private static bind<A extends any[], B extends any[], R>(this: new (...args: [...A, ...B]) => R, thisArg: any, ...args: A): new (...args: B) => R;

        /**
         * 返回集合中的元素数量。
         * @returns {number} - 集合中的元素数量。
         */
        size(): int;

        /**
         * 检查集合是否为空。
         * @returns {boolean} - 如果集合为空，则返回 true；否则返回 false。
         */
        isEmpty(): boolean;

        /**
         * 检查集合是否包含指定元素。
         * @param {object} element - 要检查的元素。
         * @returns {boolean} - 如果集合包含指定元素，则返回 true；否则返回 false。
         */
        contains(element: object): boolean;

        /**
         * 返回包含集合所有元素的数组。
         * @returns {E[]} - 包含集合所有元素的数组。
         */
        toArray(): E[];

        /**
         * 返回集合的流（stream），用于进行各种操作。
         * @returns {java.util.stream.Stream<E>} - 集合的流。
         */
        stream(): java.util.stream.Stream<E>;

        /**
         * 检查集合是否包含另一个集合的所有元素。
         * @param {Collection<E>} c - 要检查的另一个集合。
         * @returns {boolean} - 如果集合包含另一个集合的所有元素，则返回 true；否则返回 false。
         */
        containsAll(c: Collection<E>): boolean;

        /**
         * 将另一个集合的所有元素添加到当前集合。
         * @param {Collection<E>} c - 要添加的另一个集合。
         * @returns {boolean} - 如果集合发生更改，则返回 true；否则返回 false。
         */
        addAll(c: Collection<E>): boolean;

        /**
         * 仅保留当前集合和另一个集合的共有元素，移除其他元素。
         * @param {Collection<E>} c - 用于保留元素的另一个集合。
         * @returns {boolean} - 如果集合发生更改，则返回 true；否则返回 false。
         */
        retainAll(c: Collection<E>): boolean;

        /**
         * 从当前集合中移除与另一个集合相同的所有元素。
         * @param {Collection<E>} c - 要从当前集合中移除的元素集合。
         * @returns {boolean} - 如果集合发生更改，则返回 true；否则返回 false。
         */
        removeAll(c: Collection<E>): boolean;

        /**
         * 对集合中的每个元素执行指定的操作。
         * @param {(element: E) => void} action - 要执行的操作。
         */
        // @ts-ignore
        forEach(action: (element: E) => void): void;

        /**
         * 将指定元素添加到集合中。
         * @param {object} element - 要添加的元素。
         * @returns {boolean} - 如果集合发生更改，则返回 true；否则返回 false。
         */
        add(element: object): boolean;

        /**
         * 从集合中移除指定的元素。
         * @param {object} element - 要移除的元素。
         * @returns {boolean} - 如果集合包含指定元素并成功移除，则返回 true；否则返回 false。
         */
        remove(element: object): boolean;

        /**
         * 清空集合，移除所有元素。
         */
        clear(): void;


        // @ts-ignore
       private sort(compareFn?: (a: E, b: E) => number) : void;
    }

    export class Set<E> extends java.util.Collection<E> {
        private static prototype: any;
    }

    // @ts-ignore
    export class List<E> extends Collection<E> {
        private static prototype: any;

        get(index: int): E | null;

        set(index: int, element: E): E | null;

        indexOf(searchElement: E): number;

        lastIndexOf(searchElement: E): number;

        subList(fromIndex: int, toIndex: int): List<E>;

        // @ts-ignore
        sort(compareFn?: (a: E, b: E) => number) : void;
    }

    export class HashSet<E> extends Set<E> {
        constructor(collection: Collection<E>);

        constructor();
    }

    export class NavigableSet<E> extends Set<E> {

        /**
         * 返回此集合中严格小于给定元素的最大元素，如果没有这样的元素则返回 null。
         * @param {E} e - 指定的元素。
         * @returns {E | null} - 小于指定元素的最大元素，如果没有则返回 null。
         */
        lower(e: E): E | null;

        /**
         * 返回此集合中小于或等于给定元素的最大元素，如果没有这样的元素则返回 null。
         * @param {E} e - 指定的元素。
         * @returns {E | null} - 小于或等于指定元素的最大元素，如果没有则返回 null。
         */
        floor(e: E): E | null;

        /**
         * 返回此集合中大于或等于给定元素的最小元素，如果没有这样的元素则返回 null。
         * @param {E} e - 指定的元素。
         * @returns {E | null} - 大于或等于指定元素的最小元素，如果没有则返回 null。
         */
        ceiling(e: E): E | null;

        /**
         * 返回此集合中严格大于给定元素的最小元素，如果没有这样的元素则返回 null。
         * @param {E} e - 指定的元素。
         * @returns {E | null} - 大于指定元素的最小元素，如果没有则返回 null。
         */
        higher(e: E): E | null;

        /**
         * 检索并移除第一个（最低）元素，如果集合为空则返回 null。
         * @returns {E | null} - 第一个元素，如果集合为空则返回 null。
         */
        pollFirst(): E | null;

        /**
         * 检索并移除最后一个（最高）元素，如果集合为空则返回 null。
         * @returns {E | null} - 最后一个元素，如果集合为空则返回 null。
         */
        pollLast(): E | null;

        /**
         * 返回此集合中元素范围从 fromElement 到 toElement 的部分视图。
         * @param {E} fromElement - 视图的起始元素。
         * @param {boolean} fromInclusive - 如果为 true，则在视图中包括起始元素。
         * @param {E} toElement - 视图的结束元素。
         * @param {boolean} toInclusive - 如果为 true，则在视图中包括结束元素。
         * @returns {NavigableSet<E>} - 在此集合中指定范围的视图。
         */
        subSet(fromElement: E, fromInclusive: boolean, toElement: E, toInclusive: boolean): NavigableSet<E>;

        /**
         * 返回此集合中元素严格小于 toElement 的部分视图。
         * @param {E} toElement - 视图的上限。
         * @param {boolean} inclusive - 如果为 true，则在视图中包括上限元素。
         * @returns {NavigableSet<E>} - 在此集合中指定范围的视图。
         */
        headSet(toElement: E, inclusive: boolean): NavigableSet<E>;

        /**
         * 返回此集合中元素大于或等于 fromElement 的部分视图。
         * @param {E} fromElement - 视图的下限。
         * @param {boolean} inclusive - 如果为 true，则在视图中包括下限元素。
         * @returns {NavigableSet<E>} - 在此集合中指定范围的视图。
         */
        tailSet(fromElement: E, inclusive: boolean): NavigableSet<E>;
    }

    export class NavigableMap<K, V> extends Map<K, V> {

        /**
         * 返回小于给定键的最大键值对，如果没有则返回 `null`。
         * @param {K} key - 给定的键。
         * @returns {Map.Entry<K, V>} - 小于给定键的最大键值对，如果没有则返回 `null`。
         */
        lowerEntry(key: K): Map.Entry<K, V> | null;

        /**
         * 返回小于给定键的最大键，如果没有则返回 `null`。
         * @param {K} key - 给定的键。
         * @returns {K} - 小于给定键的最大键，如果没有则返回 `null`。
         */
        lowerKey(key: K): K | null;

        /**
         * 返回小于等于给定键的最大键值对，如果没有则返回 `null`。
         * @param {K} key - 给定的键。
         * @returns {Map.Entry<K, V>} - 小于等于给定键的最大键值对，如果没有则返回 `null`。
         */
        floorEntry(key: K): Map.Entry<K, V> | null;

        /**
         * 返回小于等于给定键的最大键，如果没有则返回 `null`。
         * @param {K} key - 给定的键。
         * @returns {K} - 小于等于给定键的最大键，如果没有则返回 `null`。
         */
        floorKey(key: K): K | null;

        /**
         * 返回大于等于给定键的最小键值对，如果没有则返回 `null`。
         * @param {K} key - 给定的键。
         * @returns {Map.Entry<K, V>} - 大于等于给定键的最小键值对，如果没有则返回 `null`。
         */
        ceilingEntry(key: K): Map.Entry<K, V> | null;

        /**
         * 返回大于等于给定键的最小键，如果没有则返回 `null`。
         * @param {K} key - 给定的键。
         * @returns {K} - 大于等于给定键的最小键，如果没有则返回 `null`。
         */
        ceilingKey(key: K): K | null;

        /**
         * 返回大于给定键的最小键值对，如果没有则返回 `null`。
         * @param {K} key - 给定的键。
         * @returns {Map.Entry<K, V>} - 大于给定键的最小键值对，如果没有则返回 `null`。
         */
        higherEntry(key: K): Map.Entry<K, V> | null;

        /**
         * 返回大于给定键的最小键，如果没有则返回 `null`。
         * @param {K} key - 给定的键。
         * @returns {K} - 大于给定键的最小键，如果没有则返回 `null`。
         */
        higherKey(key: K): K | null;

        /**
         * 返回第一个（最小的）键值对，如果地图为空则返回 `null`。
         * @returns {Map.Entry<K, V>} - 第一个键值对，如果地图为空则返回 `null`。
         */
        firstEntry(): Map.Entry<K, V> | null;

        /**
         * 返回最后一个（最大的）键值对，如果地图为空则返回 `null`。
         * @returns {Map.Entry<K, V>} - 最后一个键值对，如果地图为空则返回 `null`。
         */
        lastEntry(): Map.Entry<K, V> | null;

        /**
         * 移除并返回第一个（最小的）键值对，如果地图为空则返回 `null`。
         * @returns {Map.Entry<K, V>} - 第一个键值对，如果地图为空则返回 `null`。
         */
        pollFirstEntry(): Map.Entry<K, V> | null;

        /**
         * 移除并返回最后一个（最大的）键值对，如果地图为空则返回 `null`。
         * @returns {Map.Entry<K, V>} - 最后一个键值对，如果地图为空则返回 `null`。
         */
        pollLastEntry(): Map.Entry<K, V> | null;

        /**
         * 返回地图的逆序视图。
         * @returns {NavigableMap<K, V>} - 逆序视图。
         */
        descendingMap(): NavigableMap<K, V>;

        /**
         * 返回地图键的可导航集合。
         * @returns {NavigableSet<K>} - 键的可导航集合。
         */
        navigableKeySet(): NavigableSet<K>;

        /**
         * 返回键的逆序视图。
         * @returns {NavigableSet<K>} - 键的逆序视图。
         */
        descendingKeySet(): NavigableSet<K>;

        /**
         * 返回地图的子视图，范围从 `fromKey` 到 `toKey`。
         * @param {K} fromKey - 起始键。
         * @param {boolean} fromInclusive - 是否包含起始键。
         * @param {K} toKey - 结束键。
         * @param {boolean} toInclusive - 是否包含结束键。
         * @returns {NavigableMap<K, V>} - 地图的子视图。
         */
        subMap(fromKey: K, fromInclusive: boolean, toKey: K, toInclusive: boolean): NavigableMap<K, V>;

        /**
         * 返回地图的头部视图，范围从地图的开始到 `toKey`。
         * @param {K} toKey - 结束键。
         * @param {boolean} inclusive - 是否包含结束键。
         * @returns {NavigableMap<K, V>} - 地图的头部视图。
         */
        headMap(toKey: K, inclusive: boolean): NavigableMap<K, V>;

        /**
         * 返回地图的尾部视图，范围从 `fromKey` 到地图的结束。
         * @param {K} fromKey - 起始键。
         * @param {boolean} inclusive - 是否包含起始键。
         * @returns {NavigableMap<K, V>} - 地图的尾部视图。
         */
        tailMap(fromKey: K, inclusive: boolean): NavigableMap<K, V>;

        /**
         * 返回地图的子视图，范围从 `fromKey` 到 `toKey`。
         * @param {K} fromKey - 起始键。
         * @param {K} toKey - 结束键。
         * @returns {NavigableMap<K, V>} - 地图的子视图。
         */
        subMap(fromKey: K, toKey: K): NavigableMap<K, V>;

        /**
         * 返回地图的头部视图，范围从地图的开始到 `toKey`。
         * @param {K} toKey - 结束键。
         * @returns {NavigableMap<K, V>} - 地图的头部视图。
         */
        headMap(toKey: K): NavigableMap<K, V>;

        /**
         * 返回地图的尾部视图，范围从 `fromKey` 到地图的结束。
         * @param {K} fromKey - 起始键。
         * @returns {NavigableMap<K, V>} - 地图的尾部视图。
         */
        tailMap(fromKey: K): NavigableMap<K, V>;
    }

    class TreeSet<E> extends NavigableSet<E> {
        private static prototype: any;
    }

    class TreeMap<K, V> extends NavigableMap<K, V> {
        private static prototype: any;
    }

    class HashMap<K, V> extends Map<K, V> {
        private static prototype: any;
    }

    class ArrayList<E> extends List<E> {
        private static prototype: any;
        constructor(collection: Collection<E>);

        constructor();
    }

    /**
     * 表示一个包装可选值的 Optional 类。
     * @template T - 包装的元素类型。
     */
    class Optional<T> {

        /**
         * 创建一个包含指定值的 Optional 实例。
         * @param {T} val - 要包装的值。
         * @returns {Optional<T>} - 包含指定值的 Optional 实例。
         */
        static of<T>(val: T): Optional<T>;

        /**
         * 创建一个空的 Optional 实例。
         * @returns {Optional<T>} - 空的 Optional 实例。
         */
        static empty<T>(): Optional<T>;

        /**
         * 创建一个包含指定值的 Optional 实例，如果值为 null，则创建一个空的 Optional。
         * @param {T} val - 要包装的值。
         * @returns {Optional<T>} - 包含指定值的 Optional 实例，或者空的 Optional。
         */
        static ofNullable<T>(val: T): Optional<T>;

        /**
         * 根据给定的条件筛选 Optional 的值。
         * @param {(value: T) => boolean} call - 用于筛选的条件函数。
         * @returns {T} - 符合条件的值，如果不符合条件，则抛出异常。
         */
        filter(call: (value: T) => boolean): T;

        /**
         * 映射 Optional 的值，并返回新的类型。
         * @template U - 映射后的类型。
         * @param {(value: T) => U | null} call - 用于映射的函数。
         * @returns {U} - 映射后的值，如果原始值为 null，则返回 null。
         */
        map<U>(call: (value: T) => U | null): U;

        /**
         * 展平 Optional 的值，并返回新的 Optional。
         * @template U - 展平后的类型。
         * @param {(value: T) => Optional<U>} call - 用于展平的函数。
         * @returns {U} - 展平后的值。
         */
        flatMap<U>(call: (value: T) => Optional<U>): U;

        /**
         * 如果 Optional 的值为 null，则返回指定的默认值。
         * @param {T | null} defaultValue - 默认值。
         * @returns {T} - Optional 的值或默认值。
         */
        orElse<T>(defaultValue: T | null): T;

        /**
         * 如果 Optional 的值为 null，则执行指定的函数并返回其结果作为值。
         * @param {() => T} call - 用于获取默认值的函数。
         * @returns {T} - Optional 的值或通过函数获取的默认值。
         */
        orElseGet(call: () => T): T;
    }

}

declare module java.time {
    export class Duration {

        /**
         * 创建一个持续时间，表示指定的毫秒数。
         * @param {number} millis - 指定的毫秒数。
         * @returns {Duration} - 新的持续时间。
         */
        static ofMillis(millis: number): Duration;

        /**
         * 创建一个持续时间，表示指定的秒数。
         * @param {number} seconds - 指定的秒数。
         * @returns {Duration} - 新的持续时间。
         */
        static ofSeconds(seconds: number): Duration;

        /**
         * 创建一个持续时间，表示指定的分钟数。
         * @param {number} minutes - 指定的分钟数。
         * @returns {Duration} - 新的持续时间。
         */
        static ofMinutes(minutes: number): Duration;

        /**
         * 创建一个持续时间，表示指定的小时数。
         * @param {number} hours - 指定的小时数。
         * @returns {Duration} - 新的持续时间。
         */
        static ofHours(hours: number): Duration;

        /**
         * 创建一个持续时间，表示指定的天数。
         * @param {number} days - 指定的天数。
         * @returns {Duration} - 新的持续时间。
         */
        static ofDays(days: number): Duration;

        /**
         * 将持续时间转换为总秒数。
         * @returns {number} - 持续时间的总秒数。
         */
        toSeconds(): number;

        /**
         * 将持续时间转换为总毫秒数。
         * @returns {number} - 持续时间的总毫秒数。
         */
        toMillis(): number;

        /**
         * 将持续时间转换为总纳秒数。
         * @returns {number} - 持续时间的总纳秒数。
         */
        toNanos(): number;

    }
}