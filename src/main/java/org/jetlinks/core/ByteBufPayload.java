package org.jetlinks.core;

import io.netty.buffer.ByteBuf;
import io.netty.util.Recycler;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;

@Slf4j
class ByteBufPayload implements Payload {

    private static final Recycler<ByteBufPayload> RECYCLER = new Recycler<ByteBufPayload>() {
        @Override
        protected ByteBufPayload newObject(Handle<ByteBufPayload> handle) {
            return new ByteBufPayload(handle);
        }
    };

    private final Recycler.Handle<ByteBufPayload> handle;

    ByteBufPayload(Recycler.Handle<ByteBufPayload> handle) {
        this.handle = handle;
    }

    private ByteBuf body;

    public static Payload of(ByteBuf body) {
        ByteBufPayload payload = RECYCLER.get();
        payload.body = body;
        return payload;
    }

    @Override
    public boolean release() {
        return handleRelease(ReferenceCountUtil.release(body));
    }

    @Override
    public boolean release(int dec) {
        return handleRelease(ReferenceCountUtil.release(body, dec));
    }

    @Override
    public Payload retain(int inc) {
        ReferenceCountUtil.retain(body, inc);
        return this;
    }

    @Override
    public Payload retain() {
        ReferenceCountUtil.retain(body);
        return this;
    }

    @Nonnull
    @Override
    public ByteBuf getBody() {
        return body;
    }

    protected boolean handleRelease(boolean release) {
        if (release) {
            body = null;
            handle.recycle(this);
        }
        return release;
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
