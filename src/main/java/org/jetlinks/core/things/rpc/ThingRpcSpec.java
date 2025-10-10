package org.jetlinks.core.things.rpc;

import org.jetlinks.core.message.HeaderKey;
import org.jetlinks.core.message.Headers;
import org.jetlinks.core.message.RepayableThingMessage;
import org.jetlinks.core.message.ThingMessageReply;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 物的 RPC 规范定义。用于在构造具体的物消息调用时，以流式 API 方式
 * 配置消息头、消息 ID、异步标记等参数，并最终执行调用获取 {@link Reply}。
 *
 * <p>该接口通过自引用泛型 Self 保持链式调用时的具体类型，避免在子类中丢失类型信息。</p>
 *
 * <p>常见使用示例（伪代码）：</p>
 * <pre>
 *   ReadPropertyRpcSpec.create(rpcSupport, message)
 *       .timeout(Duration.ofSeconds(5))
 *       .header("traceId", traceId)
 *       .mapErrorToReply(true)
 *       .execute();
 * </pre>
 *
 * @param <Self>    自引用类型，用于保持链式 API 返回准确类型
 * @param <Message> 可被回复的物消息类型
 * @param <Reply>   回复消息类型
 */
public interface ThingRpcSpec<Self extends ThingRpcSpec<Self, Message, Reply>,
    Message extends RepayableThingMessage<Reply>,
    Reply extends ThingMessageReply> {

    /**
     * 设置调用超时时间。
     * 等价于设置 {@link Headers#timeout} 头，单位毫秒。
     *
     * @param timeoutMs 超时时间
     * @return this
     */
    default Self timeout(long timeoutMs) {
        return custom(msg -> msg.addHeader(Headers.timeout, timeoutMs));
    }

    /**
     * 指定自定义消息 ID。
     *
     * @param messageId 消息ID
     * @return this
     */
    default Self messageId(String messageId) {
        return custom(msg -> msg.messageId(messageId));
    }

    /**
     * 设置是否异步执行。
     * 等价于设置 {@link Headers#async} 头。
     *
     * @param async 是否异步
     * @return this
     */
    default Self async(boolean async) {
        return custom(msg -> msg.addHeader(Headers.async, async));
    }

    /**
     * 设置一个强类型 Header。
     *
     * @param header HeaderKey
     * @param value  值
     * @param <T>    值类型
     * @return this
     */
    default <T> Self header(HeaderKey<T> header, T value) {
        return custom(msg -> msg.addHeader(header, value));
    }

    /**
     * 设置一个字符串 Key 的 Header。
     *
     * @param header 头名称
     * @param value  值
     * @return this
     */
    default Self header(String header, Object value) {
        return custom(msg -> msg.addHeader(header, value));
    }

    /**
     * 批量设置 Header。
     *
     * @param headers 头键值对
     * @return this
     */
    default Self headers(Map<String, Object> headers) {
        return custom(msg -> headers.forEach(msg::addHeader));
    }

    /**
     * 将执行错误转换为 {@link Reply} 返回，而不是抛出异常。
     * 便于统一以 Reply 处理错误场景。
     *
     * @param mapErrorToReply 是否映射错误为 Reply
     * @return this
     */
    Self mapErrorToReply(boolean mapErrorToReply);

    /**
     * 对底层 {@link Message} 进行自定义处理。
     *
     * @param handler 处理器
     * @return this
     */
    Self custom(Consumer<Message> handler);

    /**
     * 异步执行 RPC 并返回 {@link Reply}。
     *
     * @return Mono of Reply
     */
    Mono<Reply> execute();

    /**
     * 阻塞执行 RPC 并返回 {@link Reply}，阻塞时间受 {@link Headers#timeout} 影响。
     * <p>
     * 注意: 避免在响应式环境中调用此方法.
     *
     * @return Reply
     */
    Reply executeBlocking();

}
