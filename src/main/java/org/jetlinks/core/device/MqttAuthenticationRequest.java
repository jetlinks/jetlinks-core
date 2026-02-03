package org.jetlinks.core.device;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.core.device.identity.Credential;
import org.jetlinks.core.device.identity.PasswordCredential;
import org.jetlinks.core.message.codec.Transport;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MqttAuthenticationRequest implements AuthenticationRequest, PasswordCredential {
    private String clientId;

    private String username;

    private String password;

    private Transport transport;
}
