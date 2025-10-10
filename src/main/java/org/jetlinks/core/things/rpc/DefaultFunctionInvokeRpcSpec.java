package org.jetlinks.core.things.rpc;

import org.jetlinks.core.message.function.ThingFunctionInvokeMessage;
import org.jetlinks.core.message.function.ThingFunctionInvokeMessageReply;
import org.jetlinks.core.message.property.ReadThingPropertyMessage;
import org.jetlinks.core.message.property.ReadThingPropertyMessageReply;
import org.jetlinks.core.things.ThingRpcSupport;
import reactor.core.publisher.Mono;

/**
 * {@link InvokeFunctionRpcSpec} 的默认实现。
 *
 * <p>基于 {@link GenericThingRpcSpec} 提供的通用能力，仅负责类型装配。</p>
 */
class DefaultFunctionInvokeRpcSpec<Message extends ThingFunctionInvokeMessage<Reply>, Reply
    extends ThingFunctionInvokeMessageReply>
    extends GenericThingRpcSpec<InvokeFunctionRpcSpec<Message, Reply>,
    Message, Reply> implements InvokeFunctionRpcSpec<Message, Reply> {

    public DefaultFunctionInvokeRpcSpec(Mono<ThingRpcSupport> support,
                                        Message message) {
        super(support, message);
    }

}
