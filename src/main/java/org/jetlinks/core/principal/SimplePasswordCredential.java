package org.jetlinks.core.principal;

record SimplePasswordCredential(String username, char[] password) implements PasswordCredential {

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public char[] getPassword() {
        return password;
    }
}
