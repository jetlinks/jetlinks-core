package org.jetlinks.core.ipc;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;
import java.util.function.Supplier;

@AllArgsConstructor
class DefaultIpcInvoker<REQ, RES> implements IpcInvoker<REQ, RES> {

    @Getter
    private final String name;

    private final Function<Publisher<REQ>, Flux<RES>> channelRequester;

    private final Supplier<Flux<RES>> noArgStreamRequester;
    private final Function<REQ, Flux<RES>> streamRequester;

    private final Supplier<Mono<RES>> noArgRequester;
    private final Function<REQ, Mono<RES>> requester;

    private final Supplier<Mono<Void>> noArgFireAndForgetRequester;
    private final Function<REQ, Mono<Void>> fireAndForgetRequester;

    private final Disposable disposable;

    @Override
    public Flux<RES> requestChannel(Publisher<REQ> req) {
        if (channelRequester == null) {
            return IpcInvoker.super.requestChannel(req);
        }
        return channelRequester.apply(req);
    }

    @Override
    public Flux<RES> requestStream() {
        if (noArgStreamRequester == null) {
            return IpcInvoker.super.requestStream();
        }
        return noArgStreamRequester.get();
    }

    @Override
    public Flux<RES> requestStream(REQ req) {
        if (streamRequester == null) {
            return IpcInvoker.super.requestStream(req);
        }
        return streamRequester.apply(req);
    }

    @Override
    public Mono<RES> request() {
        if (noArgRequester == null) {
            return IpcInvoker.super.request();
        }
        return noArgRequester.get();
    }

    @Override
    public Mono<RES> request(REQ req) {
        if (requester == null) {
            return IpcInvoker.super.request(req);
        }
        return requester.apply(req);
    }

    @Override
    public Mono<Void> fireAndForget(REQ req) {
        if (fireAndForgetRequester == null) {
            return IpcInvoker.super.fireAndForget(req);
        }
        return fireAndForgetRequester.apply(req);
    }

    @Override
    public Mono<Void> fireAndForget() {
        if (noArgFireAndForgetRequester == null) {
            return IpcInvoker.super.fireAndForget();
        }
        return noArgFireAndForgetRequester.get();
    }

    @Override
    public void dispose() {
        if (disposable != null) {
            disposable.dispose();
        }
    }

    public IpcInvokerBuilder<REQ, RES> copyToBuilder() {
        return IpcInvokerBuilder.<REQ, RES>newBuilder()
                .name(name)
                .forRequestChannel(channelRequester)
                .forRequestStream(streamRequester)
                .forRequestStream(noArgStreamRequester)
                .forRequest(requester)
                .forRequest(noArgRequester)
                .forFireAndForget(fireAndForgetRequester)
                .forFireAndForget(noArgFireAndForgetRequester)
                ;
    }

    @Override
    public String toString() {
        return "IpcInvoker{" +
                '\'' + name + '\'' +
                '}';
    }
}
