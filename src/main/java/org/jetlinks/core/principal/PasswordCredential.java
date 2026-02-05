package org.jetlinks.core.principal;

public interface PasswordCredential extends Credential {

    String getUsername();

    char[] getPassword();

    static PasswordCredential create(String username,char[] password){
        return new SimplePasswordCredential(username,password);
    }

}
