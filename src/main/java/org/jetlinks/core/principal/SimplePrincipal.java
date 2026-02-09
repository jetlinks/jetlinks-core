package org.jetlinks.core.principal;

record SimplePrincipal(Identity identity, Credential credential) implements Principal {
}
