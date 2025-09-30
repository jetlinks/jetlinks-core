package org.jetlinks.core.things;

import org.jetlinks.core.message.RepayableThingMessage;
import org.jetlinks.core.message.ThingMessage;
import org.jetlinks.core.message.ThingMessageReply;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 物消息RPC支持,用于发送消息给物.并获取返回.
 *
 * @author zhouhao
 * @since 1.0
 */
public interface ThingRpcSupport {


    /**
     * 执行RPC,并获取对应类型的回复.
     *
     * @param request request
     * @param <T>     回复类型
     * @param <R>     请求类型
     * @return reply
     */
    @SuppressWarnings("unchecked")
    default <T extends ThingMessageReply, R extends RepayableThingMessage<T>> Flux<T> call(R request) {
        return this
            .call((ThingMessage) request)
            .map(reply -> (T) reply);
    }


    /**
     * 执行RPC
     *
     * @param message 消息
     * @return 回复
     */
    Flux<? extends ThingMessage> call(ThingMessage message);

}
