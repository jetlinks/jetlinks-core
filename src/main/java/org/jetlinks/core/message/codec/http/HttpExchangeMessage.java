package org.jetlinks.core.message.codec.http;

import io.netty.buffer.ByteBuf;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.Function;


/**
 * 可响应的http消息,在{@link HttpExchangeMessage#payload()}有结果之前,
 * {@link HttpExchangeMessage#getPayload()}
 * 和
 * {@link HttpExchangeMessage#multiPart()}可能为空。
 * <p>
 * 如果需要获取请求体,请使用{@link HttpExchangeMessage#payload()}获取.
 * <p>
 * 如果需要获取文件上传信息,请使用{@link HttpExchangeMessage#multiPartAsync()}获取.
 *
 * @author zhouhao
 * @see HttpRequestMessage
 * @see HttpResponseMessage
 * @see SimpleHttpResponseMessage
 * @since 1.0.2
 */
public interface HttpExchangeMessage extends HttpRequestMessage {

    /**
     * 创建一个HttpExchangeMessage,当请求体读取完成时,会调用responseHandler进行响应.
     *
     * @param request         请求
     * @param responseHandler 响应处理器
     * @return HttpExchangeMessage
     */
    static HttpExchangeMessage create(HttpRequestMessage request,
                                      Function<HttpResponseMessage, Mono<Void>> responseHandler) {
        return new SimpleHttpExchangeMessage(request, responseHandler);
    }

    /**
     * 异步获取请求体内容. 大部分场景都需要对http请求进行合法性校验.
     * 由于一些非法请求可能会构造比较大的数据导致内存溢出,因此需要异步获取请求体内容.
     * <p>
     * 先校验请求,再读取请求体.
     *
     * <pre>{@code
     *   this
     *   .validate(message)//验证请求
     *   .then(message.payload())//读取payload
     *   .map(this::doDecode)//解码
     * }</pre>
     *
     * @return 响应消息
     * @since 2.0
     */
    default Mono<ByteBuf> payload() {
        return Mono.just(getPayload());
    }


    /**
     * 异步获取文件上传内容
     *
     * @return 文件上传内容
     */
    default Mono<MultiPart> multiPartAsync() {
        return payload()
            .then(Mono.defer(() -> Mono.justOrEmpty(HttpRequestMessage.super.multiPart())));
    }

    /**
     * 请使用 {@link HttpExchangeMessage#multiPartAsync()}获取
     *
     * @return 文件上传信息
     */
    @Deprecated
    @Override
    default Optional<MultiPart> multiPart() {
        return HttpRequestMessage.super.multiPart();
    }

    /**
     * 请使用 {@link  HttpExchangeMessage#payload()}方法来获取
     *
     * @return 已解析的请求体
     * @deprecated 已弃用, 请使用 payload() 方法替代
     */
    @Nonnull
    @Override
    @Deprecated
    ByteBuf getPayload();


    @Nonnull
    Mono<Void> response(@Nonnull HttpResponseMessage message);

    default Mono<Void> ok(@Nonnull String message) {
        return response(
            SimpleHttpResponseMessage
                .builder()
                .contentType(MediaType.APPLICATION_JSON)
                .status(200)
                .body(message)
                .build()
        );
    }

    default Mono<Void> error(int status, @Nonnull String message) {
        return response(SimpleHttpResponseMessage.builder()
                                                 .contentType(MediaType.APPLICATION_JSON)
                                                 .status(status)
                                                 .body(message)
                                                 .build());
    }

    default SimpleHttpResponseMessage.SimpleHttpResponseMessageBuilder newResponse() {
        return SimpleHttpResponseMessage.builder();
    }
}
