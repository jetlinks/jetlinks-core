package org.jetlinks.core.device.identity;

public interface PasswordCredential extends Credential {
    String getUsername();

    String getPassword();
}
