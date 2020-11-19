package org.jetlinks.core;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
class ByteBufPayload implements Payload {

    private final ByteBuf body;

    public static Payload of(ByteBuf body) {
        return new ByteBufPayload(body);
    }

    @Nonnull
    @Override
    public ByteBuf getBody() {
        return body;
    }

    @Override
    protected void finalize() throws Throwable {
        int refCnt = ReferenceCountUtil.refCnt(body);
        if (refCnt != 0) {
            log.debug("payload {} was not release properly, release() was not called before it's garbage-collected. refCnt={}", body, refCnt);
        }
        super.finalize();
    }
}
