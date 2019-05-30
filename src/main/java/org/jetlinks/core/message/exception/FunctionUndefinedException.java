package org.jetlinks.core.message.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FunctionUndefinedException extends IllegalArgumentException {
    private String function;

    public FunctionUndefinedException(String function,String message){
        super(message);
        this.function=function;
    }
}
