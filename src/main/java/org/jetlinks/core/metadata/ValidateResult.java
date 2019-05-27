package org.jetlinks.core.metadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


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
    //说明
    private String  description;

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
        result.setDescription(message);
        return result;
    }
}
