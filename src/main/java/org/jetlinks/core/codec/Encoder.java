package org.jetlinks.core.codec;

import org.jetlinks.core.Payload;

public interface Encoder<T> {

    Payload encode(T body);

}
