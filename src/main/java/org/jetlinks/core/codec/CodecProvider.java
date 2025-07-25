package org.jetlinks.core.codec;

public interface CodecProvider {

     String getId();

     int getByteCount();

     Codec<?> create();
}
