package org.jetlinks.core.rpc.transport;
import io.netty.buffer.ByteBuf;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.IllegalReferenceCountException;
import java.util.Objects;
/** 拥有 data/metadata 各一个引用；区域是借用视图，消费者只释放 frame。 */
public final class RpcFrame extends AbstractReferenceCounted {
    private final ByteBuf data, metadata;
    private final RpcFrame parent;
    public RpcFrame(ByteBuf data, ByteBuf metadata) {
        this.data = Objects.requireNonNull(data, "data");
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.parent = null;
    }
    private RpcFrame(RpcFrame parent) {
        parent.retain();
        try {
            data = parent.data.duplicate();
            metadata = parent.metadata.duplicate();
            this.parent = parent;
        } catch (Throwable error) { parent.release(); throw error; }
    }
    private void accessible() { if (refCnt() <= 0) throw new IllegalReferenceCountException(0); }
    public ByteBuf data() { accessible(); return data; }
    public ByteBuf metadata() { accessible(); return metadata; }
    public RpcFrame retainedDuplicate() {
        return new RpcFrame(this);
    }
    @Override protected void deallocate() {
        if (parent != null) parent.release();
        else try { data.release(); } finally { metadata.release(); }
    }
    @Override public RpcFrame touch(Object hint) { data.touch(hint); metadata.touch(hint); return this; }
}
