package org.jetlinks.core.cache;

public interface FileQueueBuilderFactory {

    <T> FileQueue.Builder<T> create();

}
