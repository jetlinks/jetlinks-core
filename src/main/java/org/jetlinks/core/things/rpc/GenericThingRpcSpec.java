package org.jetlinks.core.things.rpc;

import lombok.RequiredArgsConstructor;
import org.jetlinks.core.message.Headers;
import org.jetlinks.core.message.RepayableThingMessage;
import org.jetlinks.core.message.ThingMessageReply;
import org.jetlinks.core.things.ThingRpcSupport;
import org.jetlinks.core.utils.Reactors;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * 通用的 {@link ThingRpcSpec} 实现，封装了基于 {@link ThingRpcSupport}
 * 的执行流程与通用配置能力。
 *
 * <p>通过组合 {@code support} 与具体的 {@code message}，提供链式配置能力，
 * 并在 {@link #execute()} 中完成调用与应答处理；当开启
 * {@link #mapErrorToReply(boolean)} 时，会在错误发生时构造一个 {@link Reply}
 * 并附带错误信息返回，便于上层统一处理。</p>
 */
@RequiredArgsConstructor
public class GenericThingRpcSpec<Self extends ThingRpcSpec<Self, Message, Reply>,
    Message extends RepayableThingMessage<Reply>,
    Reply extends ThingMessageReply> implements ThingRpcSpec<Self, Message, Reply> {

    private final Mono<ThingRpcSupport> support;

    private final Message message;

    private boolean mapErrorToReply;

    @Override
    public Self mapErrorToReply(boolean mapErrorToReply) {
        this.mapErrorToReply = mapErrorToReply;
        return caseSelf();
    }

    @Override
    public Self custom(Consumer<Message> handler) {
        handler.accept(message);
        return caseSelf();
    }

    @Override
    public Mono<Reply> execute() {
        Mono<Reply> executor =
            support
                .flatMap(support -> support
                    .call(message)
                    .map(msg -> (Reply) msg)
                    .singleOrEmpty());

        if (mapErrorToReply) {
            return executor
                .onErrorResume(err -> {
                    Reply reply = message.newReply();
                    reply.error(err);
                    return Mono.just(reply);
                });
        }

        return executor;
    }

    @Override
    public Reply executeBlocking() {
        long timeout = message.getOrAddHeaderDefault(Headers.timeout);
        return Reactors.await(
            execute(),
            Duration.ofMillis(timeout));
    }

    protected Self caseSelf() {
        return (Self) this;
    }
}
