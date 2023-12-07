// @ts-ignore
import {java} from "java";

declare namespace reactor.core.publisher {

    export class Publisher<T> {
    }

    export class Signal<T> {

        hasError(): boolean;

        hasValue(): boolean;

        get(): T;

        getContextView(): reactor.util.context.ContextView;

        getThrowable(): Error;

    }

    /**
     * FluxSink 类用于与 Flux（响应式流）进行交互，允许手动控制流的行为。
     * @template T - 流中的元素类型。
     */
    export class FluxSink<T> {

        /**
         * 向下游发送一个元素。
         * @param {T} t - 要发送的元素。
         * @returns {FluxSink<T>} - 当前 FluxSink 实例，以支持链式调用。
         */
        next(t: T): FluxSink<T>;

        /**
         * 向下游发送一个错误信号。
         * @param {Error} e - 要发送的错误。
         * @returns {FluxSink<T>} - 当前 FluxSink 实例，以支持链式调用。
         */
        error(e: Error): FluxSink<T>;

        /**
         * 向下游发送完成信号。
         * @returns {FluxSink<T>} - 当前 FluxSink 实例，以支持链式调用。
         */
        complete(): FluxSink<T>;

        /**
         * 获取当前上下文。
         * @returns {reactor.util.context.Context} - 当前上下文。
         */
        currentContext(): reactor.util.context.Context;

        /**
         * 从下游请求的元素数量。
         * @returns {number} - 从下游请求的元素数量。
         */
        requestedFromDownstream(): number;

        /**
         * 检查是否已取消订阅。
         * @returns {boolean} - 如果已取消订阅，则为 true；否则为 false。
         */
        isCancelled(): boolean;

        /**
         * 注册在 Flux 取消订阅时执行的回调函数。
         * @param {() => void} callback - 在取消订阅时执行的回调函数。
         */
        onCancel(callback: () => void): void;

        /**
         * 注册在 Flux 释放资源时执行的回调函数。
         * @param {() => void} callback - 在释放资源时执行的回调函数。
         */
        onDispose(callback: () => void): void;

        /**
         * 注册在下游请求元素时执行的回调函数。
         * @param {(n: number) => void} callback - 在下游请求元素时执行的回调函数。
         */
        onRequest(callback: (n: number) => void): void;
    }

    /**
     * 表示一个发布者 Publisher 的 Mono 类，用于处理反应式流中只包含一个元素的操作。
     * @template T - 发布者中的元素类型。
     */
    export class Mono<T> extends Publisher<T> {

        /**
         * 创建一个只包含指定值的 Mono。
         * @param {T} value - 要包含在 Mono 中的元素。
         * @returns {Mono<T>} - 包含指定元素的 Mono 实例。
         */
        static just<T>(value: T): Mono<T>;

        /**
         * 如果值不为空，则创建一个只包含指定值的 Mono；否则，创建一个空的 Mono。
         * @param {T} value - 要包含在 Mono 中的元素。
         * @returns {Mono<T>} - 包含指定元素的 Mono 实例，或者空的 Mono。
         */
        static justOrEmpty<T>(value: T): Mono<T>;

        /**
         * 创建一个空的 Mono。
         * @returns {Mono<T>} - 空的 Mono 实例。
         */
        static empty<T>(): Mono<T>;

        /**
         * 将两个 Mono 合并为一个新的 Mono，使用指定的转换器进行合并。
         * @template A - 第一个 Mono 的元素类型。
         * @template B - 第二个 Mono 的元素类型。
         * @template T - 合并后的元素类型。
         * @param {Mono<A>} left - 要合并的第一个 Mono。
         * @param {Mono<B>} right - 要合并的第二个 Mono。
         * @param {(a: A, b: B) => T} converter - 用于合并的转换器函数。
         * @returns {Mono<T>} - 合并后的 Mono 实例。
         */
        static zip<A, B, T>(left: Mono<A>, right: Mono<B>, converter: (a: A, b: B) => T): Mono<T>;

        /**
         * 映射每个元素并返回新的 Mono。
         * @template U - 映射后的元素类型。
         * @param {(val: T) => U} transfer - 用于映射的函数。
         * @returns {Mono<U>} - 包含映射后元素的新 Mono。
         */
        map<U>(transfer: (val: T) => U): Mono<U>;

        // ... 其他方法的注释

        /**
         * 判断 Mono 是否包含元素。
         * @returns {Mono<boolean>} - 表示是否包含元素的 Mono。
         */
        hasElement(): Mono<boolean>;

        /**
         * 将 Mono 转换为 Flux。
         * @returns {Flux<T>} - 转换后的 Flux 实例。
         */
        flux(): Flux<T>;

        /**
         * 表示 Mono 操作的结束，并返回 Mono。
         * @returns {Mono<void>} - 表示操作结束的 Mono。
         */
        then(): Mono<void>;

        /**
         * 表示 Mono 操作的结束，并返回包含结果的 Mono。
         * @template T - 结果的类型。
         * @param {Mono<T>} t - 包含结果的 Mono。
         * @returns {Mono<T>} - 包含结果的 Mono。
         */
        then<T>(t: Mono<T>): Mono<T>;

        /**
         * 表示 Mono 操作的结束，并返回指定的结果。
         * @template U - 结果的类型。
         * @param {U} t - 指定的结果。
         * @returns {Mono<U>} - 包含结果的 Mono。
         */
        thenReturn<U>(t: U): Mono<U>;

        /**
         * 表示 Mono 操作的结束，并返回另一个 Mono 或 Flux。
         * @template U - 结果的类型。
         * @param {Mono<U> | Flux<U>} t - 另一个 Mono 或 Flux。
         * @returns {Flux<U>} - 表示结果的 Flux。
         */
        thenMany<U>(t: Mono<U> | Flux<U>): Flux<U>;

        /**
         * 处理错误情况，根据错误回调返回新的 Mono。
         * @param {(error: Error) => Mono<T>} error - 用于处理错误的回调函数。
         * @returns {Mono<T>} - 处理错误后的新 Mono。
         */
        onErrorResume(error: (error: Error) => Mono<T>): Mono<T>;

        /**
         * 处理错误情况，根据错误回调返回新的 Mono。
         * @param {(error: Error) => Error} callback - 用于处理错误的回调函数。
         * @returns {Mono<T>} - 处理错误后的新 Mono。
         */
        onErrorMap(callback: (error: Error) => Error): Mono<T>;


        /**
         * 将当前流的上下文修改为指定的上下文。
         * @param {reactor.util.context.Context} context - 指定的上下文。
         * @returns {Mono<T>} - 具有新上下文的流。
         */
        contextWrite(context: reactor.util.context.Context): Mono<T>;

        /**
         * 根据指定的上下文转换函数修改当前流的上下文。
         * @param {(context: reactor.util.context.Context) => reactor.util.context.Context} call - 上下文转换函数。
         * @returns {Mono<T>} - 具有新上下文的流。
         */
        contextWrite(call: (context: reactor.util.context.Context) => reactor.util.context.Context): Mono<T>;

        /**
         * 注册一个在流中的每个元素被触发时执行的回调函数。
         * @param {(val: T) => void} callback - 元素触发时执行的回调函数。
         * @returns {Mono<T>} - 当前流。
         */
        doOnNext(callback: (val: T) => void): Mono<T>;

        /**
         * 注册一个在流完成时执行的回调函数。
         * @param {() => void} callback - 完成时执行的回调函数。
         * @returns {Mono<T>} - 当前流。
         */
        doOnComplete(callback: () => void): Mono<T>;

        /**
         * 注册一个在流发生错误时执行的回调函数。
         * @param {(error: Error) => void} callback - 错误时执行的回调函数。
         * @returns {Mono<T>} - 当前流。
         */
        doOnError(callback: (error: Error) => void): Mono<T>;

        /**
         * 注册一个在流被取消订阅时执行的回调函数。
         * @param {() => void} callback - 取消订阅时执行的回调函数。
         * @returns {Mono<T>} - 当前流。
         */
        doOnCancel(callback: () => void): Mono<T>;

        /**
         * 注册一个在流终止时（无论是正常终止还是出错）执行的回调函数。
         * @param {(signal: Signal<T>) => void} callback - 终止时执行的回调函数。
         * @returns {Mono<T>} - 当前流。
         */
        doFinally(callback: (signal: Signal<T>) => void): Mono<T>;

        /**
         * 注册一个在流终止后（包括正常终止和出错）执行的回调函数。
         * @param {() => void} callback - 终止后执行的回调函数。
         * @returns {Mono<T>} - 当前流。
         */
        doAfterTerminate(callback: () => void): Mono<T>;

        /**
         * 订阅当前流，返回用于取消订阅的 Disposable 对象。
         * @returns {reactor.core.Disposable} - 用于取消订阅的 Disposable 对象。
         */
        subscribe(): reactor.core.Disposable;

        /**
         * 订阅当前流，注册一个在每个元素触发时执行的回调函数。
         * @param {(val: T) => void} value - 元素触发时执行的回调函数。
         * @returns {reactor.core.Disposable} - 用于取消订阅的 Disposable 对象。
         */
        subscribe(value: (val: T) => void): reactor.core.Disposable;

        /**
         * 订阅当前流，注册在每个元素触发、出错和正常完成时执行的回调函数。
         * @param {(val: T) => void} value - 元素触发时执行的回调函数。
         * @param {(error: Error) => void} error - 错误时执行的回调函数。
         * @param {() => void} complete - 完成时执行的回调函数。
         * @returns {reactor.core.Disposable} - 用于取消订阅的 Disposable 对象。
         */
        subscribe(value: (val: T) => void,
                  error?: (error: Error) => void,
                  complete?: () => void): reactor.core.Disposable;

        //  block(): T;
    }

    /**
     * 表示一个发布者 Publisher 的 Flux 类，用于处理反应式流的操作。
     * @template T - 发布者中的元素类型。
     */
    export class Flux<T> extends Publisher<T> {

        /**
         * 创建一个包含指定元素的 Flux。
         * @param {...any} args - 要包含在 Flux 中的元素。
         * @returns {Flux<T>} - 包含指定元素的 Flux 实例。
         */
        static just<T>(...args: any): Flux<T>;

        /**
         * 从数组中创建一个 Flux。
         * @param {T[]} array - 包含元素的数组。
         * @returns {Flux<T>} - 包含数组元素的 Flux 实例。
         */
        static fromArray<T>(array: T[]): Flux<T>;

        /**
         * 从可迭代对象中创建一个 Flux。
         * @param {Iterable<T>} iterable - 包含元素的可迭代对象。
         * @returns {Flux<T>} - 包含可迭代对象元素的 Flux 实例。
         */
        static fromIterable<T>(iterable: java.lang.Iterable<T>): Flux<T>;

        /**
         * 合并多个 Flux，返回包含所有元素的 Flux。
         * @param {...Flux<any>} args - 要合并的 Flux 实例。
         * @returns {Flux<any>} - 合并后的 Flux 实例。
         */
        static merge(...args: Flux<any>[]): Flux<any>;

        /**
         * 连接多个 Flux，返回包含所有元素的 Flux。
         * @param {...Flux<any>} args - 要连接的 Flux 实例。
         * @returns {Flux<any>} - 连接后的 Flux 实例。
         */
        static concat(...args: Flux<any>[]): Flux<any>;

        /**
         * 创建一个Flux,并手动控制流的行为.
         *
         * ```js
         *  Flux.create(sink=>{
         *
         *      sink.next(1);
         *      sink.complete();
         *  })
         * ```
         *
         * @param callback - 用于创建 Flux 的回调函数。
         */
        static create<T>(callback: (sink: FluxSink<T>) => void): Flux<T>;

        /**
         * 映射每个元素并返回新的 Flux。
         * @template U - 映射后的元素类型。
         * @param {(val: T) => U} transfer - 用于映射的函数。
         * @returns {Flux<U>} - 包含映射后元素的新 Flux。
         */
        map<U>(transfer: (val: T) => U): Flux<U>;

        /**
         * 展平每个元素并返回新的 Flux。
         * @template U - 映射后的元素类型。
         * @param {(val: T) => Flux<U> | Mono<U>} transfer - 用于展平的函数。
         * @param {number} concurrency - 并发数。
         * @returns {U} - 展平后的元素。
         */
        flatMap<U>(transfer: (val: T) => Flux<U> | Mono<U>, concurrency?: number): U;

        /**
         * 连接映射后的元素并返回新的 Flux。
         * @template U - 映射后的元素类型。
         * @param {(val: T) => Flux<U> | Mono<U>} transfer - 用于映射的函数。
         * @returns {U} - 连接后的元素。
         */
        concatMap<U>(transfer: (val: T) => Flux<U> | Mono<U>): U;

        /**
         * 仅取前 n 个元素并返回新的 Flux。
         * @param {number} n - 要取的元素个数。
         * @returns {Flux<T>} - 包含前 n 个元素的新 Flux。
         */
        take(n: number): Flux<T>;

        /**
         * 跳过前 n 个元素并返回新的 Flux。
         * @param {number} n - 要跳过的元素个数。
         * @returns {Flux<T>} - 包含跳过前 n 个元素的新 Flux。
         */
        skip(n: number): Flux<T>;

        /**
         * 筛选元素并返回新的 Flux，只包含满足条件的元素。
         * @param {(val: T) => boolean} predicate - 用于筛选的条件函数。
         * @returns {Flux<T>} - 包含满足条件元素的新 Flux。
         */
        filter(predicate: (val: T) => boolean): Flux<T>;

        /**
         * 根据条件筛选元素并返回新的 Flux。
         * @param {(val: T) => Mono<boolean>} predicate - 用于筛选的条件函数。
         * @returns {Flux<T>} - 包含满足条件元素的新 Flux。
         */
        filterWhen(predicate: (val: T) => Mono<boolean>): Flux<T>;

        /**
         * 判断 Flux 是否包含元素。
         * @returns {Mono<boolean>} - 表示是否包含元素的 Mono。
         */
        hasElements(): Mono<boolean>;

        /**
         * 将元素收集为列表。
         * @returns {Mono<java.util.List<T>>} - 包含元素列表的 Mono。
         */
        collectList(): Mono<java.util.List<T>>;

        /**
         * 将元素按指定大小分组并返回新的 Flux。
         * @param {number} size - 分组大小。
         * @returns {Flux<java.util.List<T>>} - 包含分组元素的新 Flux。
         */
        buffer(size: number): Flux<java.util.List<T>>;

        /**
         * 将元素按指定大小和时间间隔分组并返回新的 Flux。
         * @param {number} size - 分组大小。
         * @param {java.time.Duration} timespan - 时间间隔。
         * @returns {Flux<java.util.List<T>>} - 包含分组元素的新 Flux。
         */
        bufferTimeout(size: number, timespan: java.time.Duration): Flux<java.util.List<T>>;

        /**
         * 将元素按指定大小分组并返回新的 Flux。
         * @param {number} size - 分组大小。
         * @returns {Flux<Flux<T>>} - 包含分组元素的新 Flux。
         */
        window(size: number): Flux<Flux<T>>;

        /**
         * 将元素按指定大小和跳过数量分组并返回新的 Flux。
         * @param {number} size - 分组大小。
         * @param {number} skip - 跳过数量。
         * @returns {Flux<Flux<T>>} - 包含分组元素的新 Flux。
         */
        window(size: number, skip: number): Flux<Flux<T>>;

        /**
         * 将元素按指定时间间隔分组并返回新的 Flux。
         * @param {java.time.Duration} windowingTimespan - 时间间隔。
         * @returns {Flux<Flux<T>>} - 包含分组元素的新 Flux。
         */
        window(windowingTimespan: java.time.Duration): Flux<Flux<T>>;

        /**
         * 将元素按指定时间间隔和开窗时间间隔分组并返回新的 Flux。
         * @param {java.time.Duration} windowingTimespan - 时间间隔。
         * @param {java.time.Duration} openWindowEvery - 开窗时间间隔。
         * @returns {Flux<Flux<T>>} - 包含分组元素的新 Flux。
         */
        window(windowingTimespan: java.time.Duration, openWindowEvery: java.time.Duration): Flux<Flux<T>>;

        /**
         * 根据键映射元素并返回新的 Flux，按键分组。
         * @template K - 键的类型。
         * @param {(val: T) => K} keyMapper - 用于映射键的函数。
         * @param {number} prefetch - 预加载元素数量。
         * @returns {Flux<GroupedFlux<K, T>>} - 包含分组元素的新 Flux。
         */
        groupBy<K>(keyMapper: (val: T) => K, prefetch?: number): Flux<GroupedFlux<K, T>>;

        /**
         * 根据键映射元素并返回新的 Flux，按键分组。
         * @template K - 键的类型。
         * @param {(val: T) => K} keyMapper - 用于映射键的函数。
         * @returns {Flux<GroupedFlux<K, T>>} - 包含分组元素的新 Flux。
         */
        groupBy<K>(keyMapper: (val: T) => K): Flux<GroupedFlux<K, T>>;

        /**
         * 将元素收集为键值对映射。
         * @template K - 键的类型。
         * @template V - 值的类型。
         * @param {(val: T) => K} keyMapper - 用于映射键的函数。
         * @param {(val: T) => V} valueMapper - 用于映射值的函数。
         * @returns {Mono<java.util.Map<K, V>>} - 包含映射结果的 Mono。
         */
        collectMap<K, V>(keyMapper: (val: T) => K, valueMapper: (val: T) => V): Mono<java.util.Map<K, V>>;

        /**
         * 处理错误情况，根据错误回调返回新的 Flux。
         * @param {(error: Error) => Publisher<T>} error - 用于处理错误的回调函数。
         * @returns {Flux<T>} - 处理错误后的新 Flux。
         */
        onErrorResume(error: (error: Error) => Publisher<T>): Flux<T>;

        /**
         * 处理错误情况，根据错误回调返回新的 Flux。
         * @param {(error: Error) => Error} callback - 用于处理错误的回调函数。
         * @returns {Flux<T>} - 处理错误后的新 Flux。
         */
        onErrorMap(callback: (error: Error) => Error): Flux<T>;

        /**
         * 转换 Flux，并返回新的 Flux。
         * @template U - 转换后的元素类型。
         * @param {(flux: Flux<T>) => Publisher<U>} transformer - 用于转换的函数。
         * @returns {Flux<U>} - 转换后的新 Flux。
         */
        transform<U>(transformer: (flux: Flux<T>) => Publisher<U>): Flux<U>;

        /**
         * 将 Flux 转换为指定类型。
         * @template U - 转换后的类型。
         * @param {(flux: Flux<T>) => U} transformer - 用于转换的函数。
         * @returns {U} - 转换后的结果。
         */
        as<U>(transformer: (flux: Flux<T>) => U): U;

        /**
         * 表示 Flux 操作的结束，并返回 Mono。
         * @returns {Mono<void>} - 表示操作结束的 Mono。
         */
        then(): Mono<void>;

        /**
         * 表示 Flux 操作的结束，并返回包含结果的 Mono。
         * @template U - 结果的类型。
         * @param {Mono<U>} t - 包含结果的 Mono。
         * @returns {Mono<U>} - 包含结果的 Mono。
         */
        then<U>(t: Mono<U>): Mono<U>;

        /**
         * 对Flux中的元素去重
         */
        distinct(): Flux<T>;

        /**
         * 统计Flux中的元素数量
         */
        count(): Mono<java.lang.Long>

        /**
         * 使用指定的key采集器进行去重
         * @param keySelector 采集器
         */
        distinct(keySelector: (val: T) => any): Flux<T>;

        /**
         * 使用指定的收集器收集数据
         * ```js
         *  flux
         *   .collect(java.util.stream.Collectors.toSet())
         *   .map(set=>{
         *
         *   })
         * ```
         * @param collector 收集器
         */
        collect<R>(collector: java.stream.Collector<T, any, R>): Mono<R>;


        /**
         * 将当前流的上下文修改为指定的上下文。
         * @param {reactor.util.context.Context} context - 指定的上下文。
         * @returns {Flux<T>} - 具有新上下文的流。
         */
        contextWrite(context: reactor.util.context.Context): Flux<T>;

        /**
         * 根据指定的上下文转换函数修改当前流的上下文。
         * @param {(context: reactor.util.context.Context) => reactor.util.context.Context} call - 上下文转换函数。
         * @returns {Flux<T>} - 具有新上下文的流。
         */
        contextWrite(call: (context: reactor.util.context.Context) => reactor.util.context.Context): Flux<T>;

        /**
         * 注册一个在流中的每个元素被触发时执行的回调函数。
         * @param {(val: T) => void} callback - 元素触发时执行的回调函数。
         * @returns {Flux<T>} - 当前流。
         */
        doOnNext(callback: (val: T) => void): Flux<T>;

        /**
         * 注册一个在流完成时执行的回调函数。
         * @param {() => void} callback - 完成时执行的回调函数。
         * @returns {Flux<T>} - 当前流。
         */
        doOnComplete(callback: () => void): Flux<T>;

        /**
         * 注册一个在流发生错误时执行的回调函数。
         * @param {(error: Error) => void} callback - 错误时执行的回调函数。
         * @returns {Flux<T>} - 当前流。
         */
        doOnError(callback: (error: Error) => void): Flux<T>;

        /**
         * 注册一个在流被取消订阅时执行的回调函数。
         * @param {() => void} callback - 取消订阅时执行的回调函数。
         * @returns {Flux<T>} - 当前流。
         */
        doOnCancel(callback: () => void): Flux<T>;

        /**
         * 注册一个在流终止时（无论是正常终止还是出错）执行的回调函数。
         * @param {(signal: Signal<T>) => void} callback - 终止时执行的回调函数。
         * @returns {Flux<T>} - 当前流。
         */
        doFinally(callback: (signal: Signal<T>) => void): Flux<T>;

        /**
         * 注册一个在流终止后（包括正常终止和出错）执行的回调函数。
         * @param {() => void} callback - 终止后执行的回调函数。
         * @returns {Flux<T>} - 当前流。
         */
        doAfterTerminate(callback: () => void): Flux<T>;

        /**
         * 订阅当前流，返回用于取消订阅的 Disposable 对象。
         * @returns {reactor.core.Disposable} - 用于取消订阅的 Disposable 对象。
         */
        subscribe(): reactor.core.Disposable;

        /**
         * 订阅当前流，注册一个在每个元素触发时执行的回调函数。
         * @param {(val: T) => void} value - 元素触发时执行的回调函数。
         * @returns {reactor.core.Disposable} - 用于取消订阅的 Disposable 对象。
         */
        subscribe(value: (val: T) => void): reactor.core.Disposable;

        /**
         * 订阅当前流，注册在每个元素触发、出错和正常完成时执行的回调函数。
         * @param {(val: T) => void} value - 元素触发时执行的回调函数。
         * @param {(error: Error) => void} error - 错误时执行的回调函数。
         * @param {() => void} complete - 完成时执行的回调函数。
         * @returns {reactor.core.Disposable} - 用于取消订阅的 Disposable 对象。
         */
        subscribe(value: (val: T) => void,
                  error?: (error: Error) => void,
                  complete?: () => void): reactor.core.Disposable;
    }


    export class GroupedFlux<K, T> extends Flux<T> {
        /**
         * 获取分组KEY
         */
        key(): K;
    }

}

declare namespace reactor.util.context {

    export class Context extends ContextView {

        static of(key: any, value: any): Context;

        static of(key: any, value: any,
                  key2: any, value2: any): Context;

        static of(key: any, value: any,
                  key2: any, value2: any,
                  key3: any, value3: any): Context;

        static of(key: any, value: any,
                  key2: any, value2: any,
                  key4: any, value4: any): Context;


        put<T>(key: string, value: T): Context;

        putNonNull<T>(key: string, value: T): Context;

        delete(key: string): Context;
    }

    export class ContextView {
        get<T>(key: string): T;

        getOrEmpty<T>(key: string): java.util.Optional<T>;

        getOrDefault<T>(key: string, defaultValue: T): T;

        hasKey(key: string): boolean;

        isEmpty(): boolean;

        size(): number;
    }

}

declare namespace reactor.core {

    export class Disposable {

        isDisposed(): boolean;

        dispose(): void

    }

}