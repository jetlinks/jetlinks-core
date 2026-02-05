package org.jetlinks.core.principal;

public interface TokenCredential extends Credential {

    String getAccessToken();


    static TokenCredential create(String token) {
        return () -> token;
    }
}
