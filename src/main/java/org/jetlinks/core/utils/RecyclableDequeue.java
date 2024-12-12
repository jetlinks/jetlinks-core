package org.jetlinks.core.utils;

import io.netty.util.internal.ObjectPool;
import io.netty.util.internal.ObjectPool.Handle;

import java.util.ArrayDeque;

public final class RecyclableDequeue<T> extends ArrayDeque<T> {

    private static final long serialVersionUID = -8605125654176467947L;

    private static final int DEFAULT_INITIAL_CAPACITY = 8;

    private static final ObjectPool<RecyclableDequeue<Object>> RECYCLER = ObjectPool.newPool(RecyclableDequeue::new);

    @SuppressWarnings("all")
    public static <T> RecyclableDequeue<T> newInstance() {
        return (RecyclableDequeue<T>) RECYCLER.get();
    }

    private final Handle<RecyclableDequeue<T>> handle;

    private RecyclableDequeue(Handle<RecyclableDequeue<T>> handle) {
        this(handle, DEFAULT_INITIAL_CAPACITY);
    }

    private RecyclableDequeue(Handle<RecyclableDequeue<T>> handle, int initialCapacity) {
        super(initialCapacity);
        this.handle = handle;
    }

    public boolean recycle() {
        clear();
        if (handle != null) {
            handle.recycle(this);
        }
        return true;
    }
}