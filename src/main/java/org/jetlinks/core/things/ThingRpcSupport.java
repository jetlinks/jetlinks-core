package org.jetlinks.core.things;

import org.jetlinks.core.message.ThingMessage;
import reactor.core.publisher.Flux;

/**
 * 物消息RPC支持,用于发送消息给物.并获取返回.
 *
 * @author zhouhao
 * @since 1.0
 */
public interface ThingRpcSupport {

    /**
     * 执行RPC
     *
     * @param message 消息
     * @return 回复
     */
    Flux<? extends ThingMessage> call(ThingMessage message);

}
