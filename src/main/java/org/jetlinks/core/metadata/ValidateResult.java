package org.jetlinks.core.metadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.function.Consumer;


/**
 * @author bsetfeng
 * @version 1.0
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ValidateResult {

    private boolean success;

    private Object value;

    private String errorMsg;

    public static ValidateResult success(Object value) {
        ValidateResult result = new ValidateResult();
        result.setSuccess(true);
        result.setValue(value);
        return result;
    }

    public static ValidateResult success() {
        ValidateResult result = new ValidateResult();
        result.setSuccess(true);
        return result;
    }

    public static ValidateResult fail(String message) {
        ValidateResult result = new ValidateResult();
        result.setSuccess(false);
        result.setErrorMsg(message);
        return result;
    }

    public Object assertSuccess(){
        if(!success){
            throw new IllegalArgumentException(errorMsg);
        }
        return value;
    }

    public void ifFail(Consumer<ValidateResult> resultConsumer) {
        if (!success) {
            resultConsumer.accept(this);
        }
    }
}
