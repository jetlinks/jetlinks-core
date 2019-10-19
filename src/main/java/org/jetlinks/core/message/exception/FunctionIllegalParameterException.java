package org.jetlinks.core.message.exception;

import lombok.Getter;
import org.jetlinks.core.message.function.FunctionParameter;

public class FunctionIllegalParameterException extends IllegalParameterException {


    public FunctionIllegalParameterException(String parameter, String message) {

        super(parameter, message);
    }
}
