package org.jetlinks.core.principal;

import org.jetlinks.core.Wrapper;

/**
 * 身份凭证
 *
 * @author zhouhao
 * @since 1.3.2
 */
public interface Principal extends Wrapper {

    /**
     * @return 身份信息
     */
    Identity identity();

    /**
     * @return 凭证
     */
    Credential credential();


    static Principal create(Identity identity, Credential credential) {
        return new SimplePrincipal(identity, credential);
    }
}
