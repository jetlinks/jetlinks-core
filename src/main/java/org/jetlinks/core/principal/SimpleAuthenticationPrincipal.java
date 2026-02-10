package org.jetlinks.core.principal;

record SimpleAuthenticationPrincipal(Identity identity, Credential credential) implements AuthenticationPrincipal {
}
