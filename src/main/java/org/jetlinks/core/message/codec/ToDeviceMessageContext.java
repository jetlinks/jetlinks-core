package org.jetlinks.core.message.codec;

import org.jetlinks.core.server.session.DeviceSession;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

/**
 * 发送给设备的上下文,在设备已经在平台中建立会话后,平台下发的指令都会使用此上下文接口
 *
 * @author zhouhao
 * @since 1.0.0
 */
public interface ToDeviceMessageContext extends MessageEncodeContext {

    /**
     * 直接发送消息给设备
     *
     * @param message 消息
     * @return 是否成功
     */
    Mono<Boolean> sendToDevice(@Nonnull EncodedMessage message);

    /**
     * 断开设备与平台的连接
     *
     * @return void
     */
    Mono<Void> disconnect();

    /**
     * @return 获取设备会话
     */
    @Nonnull
    DeviceSession getSession();
}
