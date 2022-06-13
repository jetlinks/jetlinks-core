package org.jetlinks.core.ipc;

import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


/**
 * ICP执行器
 *
 * @param <REQ>
 * @param <RES>
 */
@Deprecated
public interface IpcInvoker<REQ, RES> extends Disposable {

    String getName();

    /**
     * 无参数,调用后不返回结果
     *
     * @return void
     */
    default Mono<Void> fireAndForget() {
        return Mono.error(new IpcException(IpcCode.unsupported));
    }

    /**
     * 有参数,调用后不返回结果
     *
     * @param req void
     * @return void
     */
    default Mono<Void> fireAndForget(REQ req) {
        return Mono.error(new IpcException(IpcCode.unsupported));
    }


    /**
     * 无参数,调用后返回结果
     *
     * @return 响应结果
     */
    default Mono<RES> request() {
        return Mono.error(new IpcException(IpcCode.unsupported));
    }

    /**
     * 有参数,调用后返回结果
     *
     * @param req 请求内容
     * @return 响应结果
     */
    default Mono<RES> request(REQ req) {
        return Mono.error(new IpcException(IpcCode.unsupported));
    }

    /**
     * 无参数,返回多个结果
     *
     * @return 结果流
     */
    default Flux<RES> requestStream() {
        return Flux.error(new IpcException(IpcCode.unsupported));
    }

    /**
     * 有参数,返回多个结果
     *
     * @param req 请求参数
     * @return 结果流
     */
    default Flux<RES> requestStream(REQ req) {
        return Flux.error(new IpcException(IpcCode.unsupported));
    }

    /**
     * 使用流作为参数进行请求并返回多个结果
     *
     * @param req 请求流
     * @return 结果流
     */
    default Flux<RES> requestChannel(Publisher<REQ> req) {
        return Flux.error(new IpcException(IpcCode.unsupported));
    }

    @Override
    default void dispose() {

    }

}