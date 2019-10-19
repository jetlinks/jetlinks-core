package org.jetlinks.core.device;

import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.DeviceMessageReply;
import org.jetlinks.core.message.RepayableDeviceMessage;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 设备消息处理器,用于接收并处理来自其他服务发往设备的消息并回复.
 * <p>
 * 此接口通常由设备网关服务使用,用户基本上无需使用此接口.
 *
 * @author zhouhao
 * @since 1.0.0
 */
public interface DeviceMessageHandler {

    /**
     * 监听处理发往设备的消息,当使用{@link DeviceMessageSender#send(RepayableDeviceMessage)}往设备发送消息时,
     * 参数<code>Consumer</code>将会收到此消息并执行相应到处理辑如:消息转码并发送到设备.<br>
     *
     * <b>如果设备未连接到此服务器,则认为设备状态信息有误,将会调用{@link DeviceOperator#offline()}方法修改设备状态.</b>
     *
     * @param serverId              服务ID, 整个集群中应该唯一,与{@link DeviceOperator#getConnectionServerId()}对应
     * @param deviceMessageConsumer 设备消息消费者
     * @see DeviceMessageSender#send(RepayableDeviceMessage)
     * @see DeviceOperator#getConnectionServerId()
     */
    void handleMessage(String serverId, Consumer<DeviceMessage> deviceMessageConsumer);

    Mono<Map<String, Byte>> getDeviceState(String serviceId, Collection<String> deviceIdList);

    /**
     * 回复消息. 当调用了 {@link  DeviceMessageSender#send(RepayableDeviceMessage)}方法后,无论方法是异步还是同步对,都需要对该消息进行回复.
     *
     * <ul>
     * <li>回复和请求的{@link DeviceMessageReply#getMessageId()}必须一致</li>
     * <li>请求和响应应该都是成对的,如:{@link org.jetlinks.core.message.property.ReadPropertyMessage}和{@link org.jetlinks.core.message.property.ReadPropertyMessageReply}</li>
     * <li>⚠️ 不推荐返回自己定义的实现,而返回统一的实现</li>
     * </ul>
     *
     * @param message 回复的对象
     * @return 异步发送接口
     * @see RepayableDeviceMessage#newReply()
     * @see org.jetlinks.core.message.property.ReadPropertyMessageReply
     * @see org.jetlinks.core.message.function.FunctionInvokeMessageReply
     */
    Mono<Boolean> reply(DeviceMessageReply message);

    Flux<DeviceMessageReply> handleReply(String messageId, Duration timeout);

    Mono<Integer> send(String serverId, Publisher<? extends DeviceMessage> message);

}
