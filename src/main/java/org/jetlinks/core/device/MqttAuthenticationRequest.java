package org.jetlinks.core.device;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MqttAuthenticationRequest implements AuthenticationRequest {
    private String clientId;

    private String username;

    private String password;
}
