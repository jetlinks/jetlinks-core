package org.jetlinks.core.message.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParameterUndefinedException extends IllegalArgumentException {
    private String parameter;

    public ParameterUndefinedException(String parameter, String message){
        super(message);
        this.parameter=parameter;
    }
}
