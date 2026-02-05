package org.jetlinks.core.principal;

public enum CredentialType {

    /**
     * @see TokenCredential
     */
    token,

    /**
     * @see PasswordCredential
     */
    password,

    /**
     * 不支持凭证
     */
    none

}
