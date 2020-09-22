package org.jetlinks.core.message.codec.context;

import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.DeviceMessageReply;
import org.jetlinks.core.message.RepayableDeviceMessage;

import java.time.Duration;
import java.util.Optional;

/**
 * 编解码上下文,用于在设备侧无法使用{@link DeviceMessage#getMessageId()}来进行请求响应关联时
 * 通过在协议包中使用此接口来进行处理.
 * <p>
 * 例如:
 * <pre>
 *  //类成员变量
 *  CodecContext context = CodecContext.newContext();
 *
 *  public Publisher&lt;DeviceMessage&gt; decode(MessageDecodeContext ctx){
 *      //1.解码
 *
 *      //2.设备返回的消息ID
 *      String deviceMsgId =.....;
 *
 *      //3.去除缓存的消息并创建回复
 *      ReadPropertyMessageReply reply = context.&lt;ReadPropertyMessage&gt;removeDownstream(deviceMsgId)
 *                 .map(ReadPropertyMessage::newReply)
 *                  .orElseThrow(()->new NullPointerException("找不到下行指令"))
 *                  .success(Collections.singletonMap("test","1"));
 *
 *      return Mono.just(reply);
 *  }
 *
 *  public Publisher&lt;EncodedMessage&gt; encode(MessageEncodeContext ctx){
 *       //1.编码
 *
 *       //2.设备支持消息ID
 *       String deviceMsgId =....;
 *
 *       Message msg = ctx.getMessage();
 *       if(msg instanceof ReadPropertyMessage){
 *          ReadPropertyMessage readMsg = (ReadPropertyMessage)msg;
 *          context.cacheDownstream(deviceMsgId, readMsg, Duration.ofMinutes(1));
 *
 *          //编码要发给设备的指令
 *          return encodeReadProperty(readMsg);
 *       }
 *     }
 *
 * </pre>
 *
 * @author zhouhao
 * @since 1.1.1
 */
public interface CodecContext {

    static CodecContext newContext() {
        return new CacheCodecContext();
    }

    /**
     * 缓存下行消息
     *
     * @param key     key
     * @param message 下行消息
     * @param ttl     有效期
     */
    void cacheDownstream(Object key, RepayableDeviceMessage<? extends DeviceMessageReply> message, Duration ttl);

    /**
     * 缓存下行消息,默认30秒超时
     *
     * @param key     key
     * @param message 下行消息
     */
    default void cacheDownstream(Object key, RepayableDeviceMessage<? extends DeviceMessageReply> message) {
        cacheDownstream(key, message, Duration.ofSeconds(30));
    }

    /**
     * 根据key获取下行消息,可通过下行消息来构造消息回复
     *
     * @param key    key
     * @param remove 自动删除
     * @param <T>    下行消息类型
     * @return 下行消息
     * @see RepayableDeviceMessage#newReply()
     */
    <T extends RepayableDeviceMessage<? extends DeviceMessageReply>> Optional<T> getDownstream(Object key, boolean remove);

    /**
     * 根据key获取并删除下行消息,可通过下行消息来构造消息回复
     *
     * @param key key
     * @param <T> 下行消息类型
     * @return 下行消息
     * @see RepayableDeviceMessage#newReply()
     */
    default <T extends RepayableDeviceMessage<? extends DeviceMessageReply>> Optional<T> removeDownstream(Object key) {
        return getDownstream(key, true);
    }


}
