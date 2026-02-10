package org.jetlinks.core.principal;

/**
 * 待认证主体
 */
public interface AuthenticationPrincipal extends Principal {


    static AuthenticationPrincipal create(Identity identity, Credential credential) {
        return new SimpleAuthenticationPrincipal(identity, credential);
    }

}
