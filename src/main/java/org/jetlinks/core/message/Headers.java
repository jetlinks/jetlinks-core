package org.jetlinks.core.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.core.metadata.DefaultValueWrapper;
import org.jetlinks.core.metadata.NullValueWrapper;
import org.jetlinks.core.metadata.ValueWrapper;

import java.util.function.Consumer;

@Getter
@AllArgsConstructor
public enum Headers {
    /**
     * 强制回复,忽略是否异步消息
     */
    forceReply("force-reply", true),

    /**
     * 是否支持异步
     *
     * @see org.jetlinks.core.message.function.FunctionInvokeMessageReply
     */
    asyncSupport("async-support", true),

    /**
     * 是否异步
     *
     * @see FunctionInvokeMessageSender#async()
     */
    async("async", true);

    private final String header;
    private final Object value;

    public ValueWrapper get(DeviceMessage message) {
        if (message == null) {
            return NullValueWrapper.instance;
        }
        return message.getHeader(getHeader())
                .<ValueWrapper>map(DefaultValueWrapper::new)
                .orElse(NullValueWrapper.instance);
    }

    public <T extends DeviceMessage> Consumer<T> setter() {
        return message -> message.addHeader(header, value);
    }

    public <T extends DeviceMessage> Consumer<T> clear() {
        return message -> message.removeHeader(header);
    }
}
