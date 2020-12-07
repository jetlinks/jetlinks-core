package org.jetlinks.core.ipc;

import lombok.AllArgsConstructor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@AllArgsConstructor
class TimeoutIpcInvoker<REQ, RES> implements IpcInvoker<REQ, RES> {

    private final Duration timeout;
    private final IpcInvoker<REQ, RES> target;

    @Override
    public String getName() {
        return target.getName();
    }

    @Override
    public Mono<Void> fireAndForget() {
        return target.fireAndForget()
                     .timeout(timeout, Mono.error(() -> new IpcException(IpcCode.timeout)));
    }

    @Override
    public Mono<Void> fireAndForget(REQ req) {
        return target.fireAndForget(req)
                     .timeout(timeout, Mono.error(() -> new IpcException(IpcCode.timeout)));
    }

    @Override
    public Mono<RES> request() {
        return target.request()
                     .timeout(timeout, Mono.error(() -> new IpcException(IpcCode.timeout)));
    }

    @Override
    public Mono<RES> request(REQ req) {
        return target.request(req)
                     .timeout(timeout, Mono.error(() -> new IpcException(IpcCode.timeout)));
    }

    @Override
    public Flux<RES> requestStream() {
        return target.requestStream()
                     .timeout(timeout, Mono.error(() -> new IpcException(IpcCode.timeout)));
    }

    @Override
    public Flux<RES> requestStream(REQ req) {
        return target.requestStream(req)
                     .timeout(timeout, Mono.error(() -> new IpcException(IpcCode.timeout)));
    }

    @Override
    public Flux<RES> requestChannel(Publisher<REQ> req) {
        return target.requestChannel(req)
                     .timeout(timeout, Mono.error(() -> new IpcException(IpcCode.timeout)));
    }

    @Override
    public void dispose() {
        target.dispose();
    }

    @Override
    public boolean isDisposed() {
        return target.isDisposed();
    }
}
