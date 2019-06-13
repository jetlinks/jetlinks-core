package org.jetlinks.core.device.registry;

import org.jetlinks.core.device.DeviceMessageSender;
import org.jetlinks.core.device.DeviceOperation;
import org.jetlinks.core.device.DeviceState;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.DeviceMessageReply;
import org.jetlinks.core.message.RepayableDeviceMessage;

import java.util.concurrent.CompletionStage;
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
     * <b>如果设备未连接到此服务器,则认为设备状态信息有误,将会调用{@link DeviceOperation#offline()}方法修改设备状态.</b>
     *
     * @param serverId              服务ID, 整个集群中应该唯一,与{@link DeviceOperation#getServerId()}对应
     * @param deviceMessageConsumer 设备消息消费者
     * @see DeviceMessageSender#send(RepayableDeviceMessage)
     * @see DeviceOperation#getServerId()
     */
    void handleMessage(String serverId, Consumer<DeviceMessage> deviceMessageConsumer);

    /**
     * 监听处理设备状态检查请求.
     * <p>
     * 场景: <br>
     * 1. 设备d1在服务server-1建立了连接,设备d1状态为{@link DeviceState#online}<br>
     * 2. 服务server-1由于服务器宕机,进程异常关闭,此时设备d1其实已经断开连接,但是设备d1状态未能正确修改为{@link DeviceState#offline}<br>
     * 3. 服务server-1完成重启,此时,设备d1的状态为{@link DeviceState#online},实际上设备d1并没有连接到此服务器<br>
     * 4. 此时在调用{@link DeviceOperation#checkState()}时,会触发此监听执行状态检查,
     * 如果设备未连接到此服务器,应该将状态修改为{@link DeviceState#offline}
     *
     * @param serverId 服务ID, 整个集群中应该唯一,与{@link DeviceOperation#getServerId()}对应
     * @param deviceId 要检查的设备ID消费者
     * @see DeviceOperation#checkState()
     * @see DeviceState#online
     * @see DeviceState#offline
     */
    void handleDeviceCheck(String serverId, Consumer<String> deviceId);

    /**
     * 回复消息. 当调用了 {@link  DeviceMessageSender#send(RepayableDeviceMessage)}方法后,无论方法时异步还是同步对,都需要对该消息进行回复.
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
    CompletionStage<Boolean> reply(DeviceMessageReply message);

    /**
     * 设置消息未异步
     *
     * @param messageId 消息ID
     */
    CompletionStage<Void> markMessageAsync(String messageId);

    /**
     * 判断消息是否未异步消息
     *
     * @param messageId 消息ID
     * @param reset 判断后是否重置
     * @return 是否未异步消息
     */
    CompletionStage<Boolean> messageIsAsync(String messageId, boolean reset);

    default CompletionStage<Boolean> messageIsAsync(String messageId) {
        return messageIsAsync(messageId, false);
    }
}
