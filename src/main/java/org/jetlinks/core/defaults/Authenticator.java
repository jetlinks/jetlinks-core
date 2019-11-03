package org.jetlinks.core.defaults;

import org.jetlinks.core.device.AuthenticationRequest;
import org.jetlinks.core.device.AuthenticationResponse;
import org.jetlinks.core.device.DeviceOperator;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

public interface Authenticator {

    Mono<AuthenticationResponse> authenticate(@Nonnull AuthenticationRequest request,
                                              @Nonnull DeviceOperator device);
}
