package org.jetlinks.core.message.exception;

import lombok.Getter;

@Getter
public class IllegalParameterException extends IllegalArgumentException {
    private String parameter;

    public IllegalParameterException(String parameter, String message) {
        super(message);
        this.parameter = parameter;
    }

}
