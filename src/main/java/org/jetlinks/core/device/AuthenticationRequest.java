package org.jetlinks.core.device;

import org.jetlinks.core.message.codec.Transport;

import java.io.Serializable;

public interface AuthenticationRequest extends Serializable {
    Transport getTransport();
}