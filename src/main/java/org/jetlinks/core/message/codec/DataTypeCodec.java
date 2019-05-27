package org.jetlinks.core.message.codec;

/**
 * @version 1.0
 **/
public interface DataTypeCodec<I,O> {
    O encode(I data);

    I decode(O data);
}
