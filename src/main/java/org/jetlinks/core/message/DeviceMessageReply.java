package org.jetlinks.core.message;

import org.jetlinks.core.enums.ErrorCode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


/**
 * 设备消息回复
 *
 * @author zhouhao
 * @since 1.0.0
 */
public interface DeviceMessageReply extends DeviceMessage, ThingMessageReply {

    //是否成功
    boolean isSuccess();

    //业务码,具体由设备定义
    @Nullable
    String getCode();

    //错误消息
    @Nullable
    String getMessage();

    //设置失败
    DeviceMessageReply error(ErrorCode errorCode);

    //设置失败
    default DeviceMessageReply error(Throwable err) {
        ThingMessageReply.super.error(err);
        return this;
    }

    //设置设备ID
    DeviceMessageReply deviceId(String deviceId);

    //设置成功
    DeviceMessageReply success();

    //设置业务码
    DeviceMessageReply code(@Nonnull String code);

    //设置消息
    DeviceMessageReply message(@Nonnull String message);

    //根据另外的消息填充对应属性
    DeviceMessageReply from(@Nonnull Message message);

    //设置消息ID
    DeviceMessageReply messageId(@Nonnull String messageId);

    @Override
    DeviceMessageReply timestamp(long timestamp);

    //添加头
    @Override
    DeviceMessageReply addHeader(@Nonnull String header, @Nonnull Object value);

    @Override
    default DeviceMessageReply thingId(String type, String thingId) {
        deviceId(thingId);
        return this;
    }

    @Override
    default <T> DeviceMessageReply addHeader(@Nonnull HeaderKey<T> header, @Nonnull T value) {
        addHeader(header.getKey(), value);
        return this;
    }

    @Override
    default DeviceMessageReply copy() {
        return (DeviceMessageReply) ThingMessageReply.super.copy();
    }
}
