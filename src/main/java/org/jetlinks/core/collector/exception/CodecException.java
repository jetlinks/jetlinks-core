package org.jetlinks.core.collector.exception;

import org.hswebframework.web.exception.I18nSupportException;

public class CodecException extends I18nSupportException.NoStackTrace{

    public CodecException(String code, Throwable cause, Object... args) {
        super(code, cause, args);
    }

}
