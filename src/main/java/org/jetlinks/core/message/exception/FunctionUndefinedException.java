package org.jetlinks.core.message.exception;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.exception.I18nSupportException;

@Getter
@Setter
public class FunctionUndefinedException extends I18nSupportException {
    private String function;

    public FunctionUndefinedException(String function){
        super("validation.function_undefined",function);
        this.function=function;
    }
}
