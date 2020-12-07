package org.jetlinks.core.ipc;

import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public class DefaultIpcInvokerBuilder<REQ, RES> implements IpcInvokerBuilder<REQ, RES> {
    private String name;

    private Function<Publisher<REQ>, Flux<RES>> channelRequester;

    private Supplier<Flux<RES>> noArgStreamRequester;
    private Function<REQ, Flux<RES>> streamRequester;

    private Supplier<Mono<RES>> noArgRequester;
    private Function<REQ, Mono<RES>> requester;

    private Supplier<Mono<Void>> noArgFireAndForgetRequester;
    private Function<REQ, Mono<Void>> fireAndForgetRequester;


    private Disposable disposable;

    private Duration timeout;

    @Override
    public IpcInvokerBuilder<REQ, RES> timeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    @Override
    public IpcInvokerBuilder<REQ, RES> doOnDispose(Disposable disposable) {
        this.disposable = disposable;
        return this;
    }

    @Override
    public IpcInvokerBuilder<REQ, RES> name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public IpcInvokerBuilder<REQ, RES> forFireAndForget(Supplier<Mono<Void>> requester) {
        this.noArgFireAndForgetRequester = Objects.requireNonNull(requester);
        return this;
    }

    @Override
    public IpcInvokerBuilder<REQ, RES> forFireAndForget(Function<REQ, Mono<Void>> requester) {
        this.fireAndForgetRequester = Objects.requireNonNull(requester);
        return this;
    }

    @Override
    public IpcInvokerBuilder<REQ, RES> forRequest(Supplier<Mono<RES>> requester) {
        this.noArgRequester = Objects.requireNonNull(requester);
        return this;
    }

    @Override
    public IpcInvokerBuilder<REQ, RES> forRequest(Function<REQ, Mono<RES>> requester) {
        this.requester = Objects.requireNonNull(requester);
        return this;
    }

    @Override
    public IpcInvokerBuilder<REQ, RES> forRequestStream(Function<REQ, Flux<RES>> requester) {
        this.streamRequester = Objects.requireNonNull(requester);
        return this;
    }

    @Override
    public IpcInvokerBuilder<REQ, RES> forRequestStream(Supplier<Flux<RES>> requester) {
        this.noArgStreamRequester = Objects.requireNonNull(requester);
        return this;
    }

    @Override
    public IpcInvokerBuilder<REQ, RES> forRequestChannel(Function<Publisher<REQ>, Flux<RES>> requester) {
        this.channelRequester = Objects.requireNonNull(requester);
        return this;
    }

    @Override
    public IpcInvoker<REQ, RES> build() {
        IpcInvoker<REQ, RES> invoker = new DefaultIpcInvoker<>(
                name,
                channelRequester,
                noArgStreamRequester,
                streamRequester,
                noArgRequester,
                requester,
                noArgFireAndForgetRequester,
                fireAndForgetRequester,
                disposable
        );
        if (timeout != null) {
            invoker = new TimeoutIpcInvoker<>(timeout, invoker);
        }
        return invoker;
    }
}
