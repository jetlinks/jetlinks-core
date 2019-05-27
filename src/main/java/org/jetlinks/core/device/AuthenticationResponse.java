package org.jetlinks.core.device;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
@ToString
public class AuthenticationResponse {
    private boolean success;

    private int code;

    private String message;

    public static AuthenticationResponse success() {
        AuthenticationResponse response = new AuthenticationResponse();
        response.success = true;
        response.code = 200;
        response.message = "授权通过";
        return response;
    }

    public static AuthenticationResponse error(int code, String message) {
        AuthenticationResponse response = new AuthenticationResponse();
        response.success = false;
        response.code = code;
        response.message = message;
        return response;
    }


}
