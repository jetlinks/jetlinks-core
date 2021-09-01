package org.jetlinks.core.exception;

import lombok.Getter;
import org.hswebframework.web.exception.I18nSupportException;

import java.util.Set;

@Getter
public class CyclicDependencyException extends I18nSupportException {

    private final Set<?> ids;

    public CyclicDependencyException(Set<?> ids) {
        super("error.cyclic_dependence");
        this.ids = ids;
    }

}
