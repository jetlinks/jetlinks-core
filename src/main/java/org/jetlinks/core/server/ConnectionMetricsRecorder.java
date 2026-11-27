package org.jetlinks.core.server;

import org.jetlinks.core.monitor.metrics.DurationSummary;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.function.Predicate;

/**
 * 通信资源持有的固定内存累计器，连接重建时可继续复用，不保存报文、历史或定时任务。
 * 收发字节由实际传输回调记录；协议消息由完成解码的位置记录，二者不混用。
 * getter 为弱一致内存读取，未实现的分布及细分读写错误继续使用契约的空值。
 *
 * @since 1.3.3
 * @see ConnectionMetrics
 */
public class ConnectionMetricsRecorder implements ConnectionMetrics {
    private static final AtomicLongFieldUpdater<ConnectionMetricsRecorder>
        READ = updater("readBytes"), WRITE = updater("writtenBytes"), MESSAGES = updater("messages"),
        DROPPED = updater("dropped"), PENDING = updater("pending"), ERRORS = updater("errors"),
        ATTEMPTS = updater("attempts"), CONNECT_FAILURES = updater("connectFailures"),
        CONNECTIONS = updater("connections"), ACTIVE = updater("active"),
        REQUEST_ERRORS = updater("requestErrors"), TIMEOUTS = updater("timeouts");

    private volatile long readBytes, writtenBytes, messages, dropped, pending, errors;
    private volatile long attempts, connectFailures, connections, active, requestErrors, timeouts;
    private volatile long connectTime, lastCommTime;
    private final DurationSummary acknowledgements = new DurationSummary();

    private static AtomicLongFieldUpdater<ConnectionMetricsRecorder> updater(String name) {
        return AtomicLongFieldUpdater.newUpdater(ConnectionMetricsRecorder.class, name);
    }

    public void received(long bytes) {
        READ.addAndGet(this, bytes);
        lastCommTime = System.currentTimeMillis();
    }

    public void sent(long bytes) {
        WRITE.addAndGet(this, bytes);
        lastCommTime = System.currentTimeMillis();
    }

    public void messageReceived() { MESSAGES.incrementAndGet(this); }
    public void messageDropped() { DROPPED.incrementAndGet(this); }
    public void transportError() { ERRORS.incrementAndGet(this); }
    public void connecting() { ATTEMPTS.incrementAndGet(this); }
    public void connectFailed() { CONNECT_FAILURES.incrementAndGet(this); }

    public void connected() {
        connectTime = System.currentTimeMillis();
        CONNECTIONS.incrementAndGet(this);
        ACTIVE.incrementAndGet(this);
    }

    /** 每次成功建立连接后仅调用一次，调用方绑定该连接的终止信号。 */
    public void disconnected() { ACTIVE.decrementAndGet(this); }

    /**
     * 统计有响应语义的请求，不为取消计失败；每次订阅独立测时并在终止时释放在途计数。
     * 耗时包含 source 内的本地等待，不代表物理网络 RTT。超时分类由协议适配层提供。
     *
     * @param source 实际通信请求，不应再次包装同一个请求
     * @param isTimeout 无副作用的协议超时判定
     * @param <T> 响应类型
     * @return 保留原响应、错误及取消语义的请求
     */
    public <T> Mono<T> recordRequest(Mono<T> source, Predicate<Throwable> isTimeout) {
        return Mono.defer(() -> {
            long started = System.nanoTime();
            PENDING.incrementAndGet(this);
            return source.doOnNext(value -> acknowledgements.record(System.nanoTime() - started))
                .doOnError(error -> {
                    REQUEST_ERRORS.incrementAndGet(this);
                    if (isTimeout.test(error)) TIMEOUTS.incrementAndGet(this);
                })
                .doFinally(ignore -> PENDING.decrementAndGet(this));
        });
    }

    @Override public long getReadBytes() { return readBytes; }
    @Override public long getWrittenBytes() { return writtenBytes; }
    @Override public long getConnectTime() { return connectTime; }
    @Override public long getLastCommTime() { return lastCommTime; }
    @Override public long getTotalMessages() { return messages; }
    @Override public long getPendingMessages() { return pending; }
    @Override public long getDroppedMessages() { return dropped; }
    @Override public Long getTransportErrors() { return errors; }
    @Override public Long getActiveConnections() { return active; }
    @Override public Long getConnectAttempts() { return attempts; }
    @Override public Long getConnectFailures() { return connectFailures; }
    public Long getReconnects() { return Math.max(0, connections - 1); }
    @Override public Long getRequestErrors() { return requestErrors; }
    @Override public Long getAckTimeouts() { return timeouts; }
    @Override public Double getAckAverageDuration() { return acknowledgements.getCount() == 0 ? null : acknowledgements.getMeanDurationSeconds(); }
    @Override public Double getAckMaxDuration() { return acknowledgements.getCount() == 0 ? null : acknowledgements.getMaxDurationNanos() / 1e9; }
    @Override public Double getAckLastDuration() { return acknowledgements.getCount() == 0 ? null : acknowledgements.getLastDurationNanos() / 1e9; }

    /** 在快照边界提供原始样本数及累计耗时，供多个连接按样本数加权汇总。 */
    @Override
    public ConnectionMetricsInfo toInfo() {
        ConnectionMetricsInfo info = ConnectionMetrics.super.toInfo();
        DurationSummary.Snapshot duration = acknowledgements.snapshot();
        info.setAckCount(duration.getCount());
        info.setAckTotalDuration(duration.getTotalDurationSeconds());
        info.setAckAverageDuration(duration.getCount() == 0 ? null : duration.getMeanDurationSeconds());
        info.setAckMaxDuration(duration.getCount() == 0 ? null : duration.getMaxDurationNanos() / 1e9);
        info.setAckLastDuration(duration.getCount() == 0 ? null : duration.getLastDurationNanos() / 1e9);
        return info;
    }
}
