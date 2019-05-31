package org.jetlinks.core.device;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.function.Consumer;

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

    public AuthenticationResponse whenSuccess(Consumer<AuthenticationResponse> onSuccess){
        if(success){
            onSuccess.accept(this);
        }
        return this;
    }

    public AuthenticationResponse whenError(Consumer<AuthenticationResponse> onError){
        if(!success){
            onError.accept(this);
        }
        return this;
    }

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
