export namespace reactor.core.publisher {

    export class Mono<T> {

        static just<T>(value: T): Mono<T>;

        static justOrEmpty<T>(value: T): Mono<T>;

        static empty<T>(): Mono<T>;

        static zip<A, B, T>(left: Mono<A>, right: Mono<B>, converter: (a: A, b: B) => T): Mono<T>;

        map<U>(transfer: (val: T) => U): Mono<U>;

        flatMap<U>(transfer: (val: T) => Mono<U>): Mono<U>;

        filter(predicate: (val: T) => boolean): Mono<T>;

        filterWhen(predicate: (val: T) => Mono<boolean>): Mono<T>;

        hasElement(): Mono<boolean>;

        flux(): Flux<T>;

        then(): Mono<void>;

        then<T>(t: Mono<T>): Mono<T>;

        thenReturn<U>(t: U): Mono<U>;

        thenMany<U>(t: Mono<U> | Flux<U>): Flux<U>;

        //  block(): T;

    }

    export class Flux<T> {
        static just<T>(...args: any): Flux<T>;

        map<U>(transfer: (val: T) => U): Flux<U>;

        flatMap<U>(transfer: (val: T) => Flux<U> | Mono<U>): U;

        filter(predicate: (val: T) => boolean): Flux<T>;

        filterWhen(predicate: (val: T) => Mono<boolean>): Flux<T>;

        hasElements(): Mono<boolean>;

        then(): Mono<void>;

        then<U>(t: Mono<U>): Mono<U>;
    }


}