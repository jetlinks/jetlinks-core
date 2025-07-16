package org.jetlinks.core.collector.exception;

import lombok.Getter;
import org.hswebframework.web.exception.I18nSupportException;

@Getter
public class ChannelException extends I18nSupportException.NoStackTrace {

    private final String channelId;

    public ChannelException(String channelId,
                            String code,
                            Object... args) {
        super(code, args);
        this.channelId = channelId;
    }

    public ChannelException(String channelId,
                            String code,
                            Throwable cause,
                            Object... args) {
        super(code, cause, args);
        this.channelId = channelId;
    }

}
