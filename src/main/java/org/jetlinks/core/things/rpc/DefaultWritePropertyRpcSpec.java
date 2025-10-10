package org.jetlinks.core.things.rpc;

import org.jetlinks.core.message.property.WriteThingPropertyMessage;
import org.jetlinks.core.message.property.WriteThingPropertyMessageReply;
import org.jetlinks.core.things.ThingRpcSupport;
import reactor.core.publisher.Mono;

/**
 * {@link WritePropertyRpcSpec} 的默认实现。
 *
 * <p>基于 {@link GenericThingRpcSpec} 提供的通用能力，仅负责类型装配。</p>
 */
class DefaultWritePropertyRpcSpec<Message extends WriteThingPropertyMessage<Reply>, Reply
    extends WriteThingPropertyMessageReply>
    extends GenericThingRpcSpec<WritePropertyRpcSpec<Message, Reply>,
    Message, Reply> implements WritePropertyRpcSpec<Message, Reply> {

    public DefaultWritePropertyRpcSpec(Mono<ThingRpcSupport> support,
                                       Message message) {
        super(support, message);
    }
}


