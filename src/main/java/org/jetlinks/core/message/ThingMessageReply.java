package org.jetlinks.core.message;

import org.hswebframework.web.exception.ValidationException;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.utils.SerializeUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public interface ThingMessageReply extends ThingMessage {

    //是否成功
    boolean isSuccess();

    //业务码,具体由设备定义
    @Nullable
    String getCode();

    //错误消息
    @Nullable
    String getMessage();

    //设置失败
    default ThingMessageReply error(String errorCode, String msg) {
        return success(false)
            .code(errorCode)
            .message(msg);
    }

    //设置失败
    ThingMessageReply error(ErrorCode errorCode);

    //设置失败
    default ThingMessageReply error(Throwable e) {
        return success(false)
            .error(ErrorCode.of(e))
            .message(e.getMessage())
            .addHeader("errorType", e.getClass().getName())
            .addHeader("errorMessage", e.getMessage());
    }

    //设置物类型和物ID
    ThingMessageReply thingId(String type, String thingId);

    //设置成功
    ThingMessageReply success();

    ThingMessageReply success(boolean success);

    //设置业务码
    ThingMessageReply code(@Nonnull String code);

    //设置消息
    ThingMessageReply message(@Nonnull String message);

    //根据另外的消息填充对应属性
    ThingMessageReply from(@Nonnull Message message);

    //设置消息ID
    ThingMessageReply messageId(@Nonnull String messageId);

    //设置时间戳
    ThingMessageReply timestamp(@Nonnull long timestamp);

    //添加头
    @Override
    ThingMessageReply addHeader(@Nonnull String header, @Nonnull Object value);

    @Override
    default <T> ThingMessageReply addHeader(@Nonnull HeaderKey<T> header, @Nonnull T value) {
        addHeader(header.getKey(), value);
        return this;
    }

    @Override
    default ThingMessageReply copy() {
        return (ThingMessageReply) ThingMessage.super.copy();
    }

    @Override
    default void writeExternal(ObjectOutput out) throws IOException {
        ThingMessage.super.writeExternal(out);
        out.writeBoolean(isSuccess());
        SerializeUtils.writeNullableUTF(getCode(), out);
        SerializeUtils.writeNullableUTF(getMessage(), out);
    }

    @Override
    default void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        ThingMessage.super.readExternal(in);
        this.success(in.readBoolean());
        this.code(SerializeUtils.readNullableUTF(in));
        this.message(SerializeUtils.readNullableUTF(in));
    }

    /**
     * 断言请求是否成功,如果失败则抛出异常
     *
     * @throws DeviceOperationException error
     * @since 1.2.2
     */
    default void assertSuccess() throws DeviceOperationException {
        if (!isSuccess()) {
            throw new DeviceOperationException
                .NoStackTrace(ErrorCode.of(getCode()).orElse(ErrorCode.UNKNOWN), getMessage());
        }
    }
}
