package org.jetlinks.core.message;

import org.jetlinks.core.enums.ErrorCode;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

public interface ThingMessageReply extends ThingMessage{

    //是否成功
    boolean isSuccess();

    //业务码,具体由设备定义
    @Nullable
    String getCode();

    //错误消息
    @Nullable
    String getMessage();

    //设置失败
    ThingMessageReply error(ErrorCode errorCode);

    //设置失败
    ThingMessageReply error(Throwable err);

    //设置物类型和物ID
    ThingMessageReply thingId(String type,String thingId);

    //设置成功
    ThingMessageReply success();

    //设置业务码
    ThingMessageReply code(@NotNull String code);

    //设置消息
    ThingMessageReply message(@NotNull String message);

    //根据另外的消息填充对应属性
    ThingMessageReply from(@NotNull Message message);

    //设置消息ID
    ThingMessageReply messageId(@NotNull String messageId);

    //设置时间戳
    ThingMessageReply timestamp(@NotNull long timestamp);

    //添加头
    @Override
    ThingMessageReply addHeader(@NotNull String header, @NotNull Object value);

    @Override
    default <T> ThingMessageReply addHeader(@NotNull HeaderKey<T> header, @NotNull T value) {
        addHeader(header.getKey(), value);
        return this;
    }

    @Override
    default ThingMessageReply copy() {
        return (ThingMessageReply)ThingMessage.super.copy();
    }
}
