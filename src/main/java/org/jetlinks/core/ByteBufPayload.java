package org.jetlinks.core;

import io.netty.buffer.ByteBuf;
import io.netty.util.Recycler;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.utils.RecyclerUtils;

import javax.annotation.Nonnull;

@Slf4j
class ByteBufPayload implements Payload {

    public static boolean POOL_ENABLED = Boolean.parseBoolean(System.getProperty("jetlinks.eventbus.payload.pool.enabled","true"));

    private static final Recycler<ByteBufPayload> RECYCLER = RecyclerUtils.newRecycler(ByteBufPayload.class,ByteBufPayload::new);

    private final Recycler.Handle<ByteBufPayload> handle;

    ByteBufPayload(Recycler.Handle<ByteBufPayload> handle) {
        this.handle = handle;
    }

    private ByteBuf body;

    private String caller;

    static Payload of(ByteBuf body) {
        ByteBufPayload payload;
        if (POOL_ENABLED) {
            try {
                payload = RECYCLER.get();

            } catch (Exception e) {
                payload = new ByteBufPayload(null);
            }
        } else {
            payload = new ByteBufPayload(null);
        }
        if (log.isTraceEnabled()) {
            for (StackTraceElement element : (new Exception()).getStackTrace()) {
                if (!"org.jetlinks.core.Payload".equals(element.getClassName()) &&
                        !"org.jetlinks.core.ByteBufPayload".equals(element.getClassName()) &&
                        !element.getClassName().startsWith("org.jetlinks.core.codec")
                ) {
                    payload.caller = element.toString();
                    break;
                }
            }
        }
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
        if (release && handle != null) {
            body = null;
            caller = null;
            handle.recycle(this);
        }
        return release;
    }

    @Override
    protected void finalize() throws Throwable {
        int refCnt = ReferenceCountUtil.refCnt(body);
        if (refCnt > 0 ) {
            log.trace("payload {} was not release properly, release() was not called before it's garbage-collected. refCnt={}. caller: {}", body, refCnt, caller);
        }
        super.finalize();
    }

    @Override
    public String toString() {
        return "ByteBufPayload{" +
                "body=" + body +
                ", caller='" + caller + '\'' +
                '}';
    }
}
