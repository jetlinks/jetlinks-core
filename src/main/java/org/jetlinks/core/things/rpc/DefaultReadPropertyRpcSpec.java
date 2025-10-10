package org.jetlinks.core.things.rpc;

import org.jetlinks.core.message.property.ReadThingPropertyMessage;
import org.jetlinks.core.message.property.ReadThingPropertyMessageReply;
import org.jetlinks.core.things.ThingRpcSupport;
import reactor.core.publisher.Mono;

/**
 * {@link ReadPropertyRpcSpec} 的默认实现。
 *
 * <p>基于 {@link GenericThingRpcSpec} 提供的通用能力，仅负责类型装配。</p>
 */
class DefaultReadPropertyRpcSpec<Message extends ReadThingPropertyMessage<Reply>, Reply
    extends ReadThingPropertyMessageReply>
    extends GenericThingRpcSpec<ReadPropertyRpcSpec<Message, Reply>,
    Message, Reply> implements ReadPropertyRpcSpec<Message, Reply> {

    public DefaultReadPropertyRpcSpec(Mono<ThingRpcSupport> support,
                                      Message message) {
        super(support, message);
    }

}
