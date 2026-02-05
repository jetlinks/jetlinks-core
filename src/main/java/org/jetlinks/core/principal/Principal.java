package org.jetlinks.core.principal;

import org.jetlinks.core.Wrapper;

public interface Principal extends Wrapper {

    Identity identity();

    Credential credential();


    static Principal create(Identity identity, Credential credential) {
        return new SimplePrincipal(identity, credential);
    }
}
