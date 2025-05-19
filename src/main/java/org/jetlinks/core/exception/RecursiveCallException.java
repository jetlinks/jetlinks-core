package org.jetlinks.core.exception;

import lombok.Getter;
import org.hswebframework.web.exception.I18nSupportException;

@Getter
public class RecursiveCallException extends I18nSupportException.NoStackTrace {

    private final String operation;
    private final int maxRecursive;

    public RecursiveCallException(String operation, int maxRecursive) {
        super("error.recursive_call", operation, maxRecursive);
        this.operation = operation;
        this.maxRecursive = maxRecursive;
    }
}
