package org.jetlinks.core.ipc;

import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Function;
import java.util.function.Supplier;

@Deprecated
public interface IpcInvokerBuilder<REQ, RES> {

    IpcInvokerBuilder<REQ, RES> name(String name);

    IpcInvokerBuilder<REQ, RES> forFireAndForget(Supplier<Mono<Void>> requester);

    IpcInvokerBuilder<REQ, RES> forFireAndForget(Function<REQ, Mono<Void>> requester);

    IpcInvokerBuilder<REQ, RES> forRequest(Supplier<Mono<RES>> requester);

    IpcInvokerBuilder<REQ, RES> forRequest(Function<REQ, Mono<RES>> requester);

    IpcInvokerBuilder<REQ, RES> forRequestStream(Function<REQ, Flux<RES>> requester);

    IpcInvokerBuilder<REQ, RES> forRequestStream(Supplier<Flux<RES>> requester);

    IpcInvokerBuilder<REQ, RES> forRequestChannel(Function<Publisher<REQ>, Flux<RES>> requester);

    IpcInvokerBuilder<REQ, RES> doOnDispose(Disposable disposable);

    IpcInvokerBuilder<REQ, RES> timeout(Duration timeout);

    IpcInvoker<REQ, RES> build();

    static <REQ, RES> IpcInvoker<REQ, RES> forTimeout(Duration timeout, IpcInvoker<REQ, RES> invoker) {
        return new TimeoutIpcInvoker<>(timeout, invoker);
    }

    static <REQ, RES> IpcInvokerBuilder<REQ, RES> newBuilder() {
        return new DefaultIpcInvokerBuilder<>();
    }

    static <REQ, RES> IpcInvoker<REQ, RES> forFireAndForget(String name, Supplier<Mono<Void>> requester) {
        return IpcInvokerBuilder.<REQ, RES>newBuilder().name(name).forFireAndForget(requester).build();
    }

    static <REQ, RES> IpcInvoker<REQ, RES> forFireAndForget(String name, Function<REQ, Mono<Void>> requester) {
        return IpcInvokerBuilder.<REQ, RES>newBuilder().name(name).forFireAndForget(requester).build();
    }

    static <REQ, RES> IpcInvoker<REQ, RES> forRequest(String name, Supplier<Mono<RES>> requester) {
        return IpcInvokerBuilder.<REQ, RES>newBuilder().name(name).forRequest(requester).build();
    }

    static <REQ, RES> IpcInvoker<REQ, RES> forRequest(String name, Function<REQ, Mono<RES>> requester) {
        return IpcInvokerBuilder.<REQ, RES>newBuilder().name(name).forRequest(requester).build();
    }

    static <REQ, RES> IpcInvoker<REQ, RES> forRequestStream(String name, Function<REQ, Flux<RES>> requester) {
        return IpcInvokerBuilder.<REQ, RES>newBuilder().name(name).forRequestStream(requester).build();
    }

    static <REQ, RES> IpcInvoker<REQ, RES> forRequestStream(String name, Supplier<Flux<RES>> requester) {
        return IpcInvokerBuilder.<REQ, RES>newBuilder().name(name).forRequestStream(requester).build();
    }

    static <REQ, RES> IpcInvoker<REQ, RES> forRequestChannel(String name, Function<Publisher<REQ>, Flux<RES>> requester) {
        return IpcInvokerBuilder.<REQ, RES>newBuilder().name(name).forRequestChannel(requester).build();
    }

}
