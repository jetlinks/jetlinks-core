package org.jetlinks.core.message.codec.parser;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.MessageParser;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于 {@link ByteBuf} 的粘拆包解析模板, 参考 Netty {@code ByteToMessageDecoder} 的实现思路:
 *
 * <ul>
 *     <li>将每次收到的报文数据累积到内部缓冲区中;</li>
 *     <li>循环调用子类实现的 {@link #handle(ByteBuf, List)} 方法从缓冲区中解析完整帧;</li>
 *     <li>当无法再解析出新帧时, 保留剩余未读数据等待下次报文到来;</li>
 *     <li>在 {@link #dispose()} 时释放内部缓冲区.</li>
 * </ul>
 *
 * <p>为避免异常客户端持续发送但报文无法被正确解析导致内存占用过高,
 * 默认限制累积缓冲区的最大容量为 16MB, 超出时抛出 {@link IllegalStateException}.</p>
 *
 * <p><b>兜底策略（可选）</b>：</p>
 * <ul>
 *     <li><b>空闲超时重置</b>：若缓冲区中长时间存在未解析数据（由 {@code maxIdleMs} 控制），
 *     在下次 {@link #handle(EncodedMessage)} 时丢弃当前累积数据，仅用本次新数据重新开始，避免脏数据一直占用内存。</li>
 *     <li><b>未解析长度上限</b>：若未解析数据长度达到 {@code maxUnparsedBytes}，
 *     在当次解析结束后丢弃整段缓冲区，下次从新数据开始，避免单连接堆积过多无法识别的数据。</li>
 * </ul>
 *
 * 子类只需关注如何从传入的 {@link ByteBuf} 中读取一帧或多帧数据, 并将解析出的帧写入容器即可.
 *
 * @author zhouhao
 * @since 1.3.2
 */
public abstract class AbstractMessageParser implements MessageParser {

    /**
     * 默认最大累积缓冲区大小: 16MB.
     */
    public static final int DEFAULT_MAX_CUMULATION_BYTES = 16 * 1024 * 1024;

    /**
     * 最大累积缓冲区大小, 防止异常粘包导致内存占用过高.
     */
    private final int maxCumulationBytes;

    /**
     * 空闲超时(毫秒): 若缓冲区中未解析数据存在超过该时长, 下次 handle 时丢弃累积数据并从新数据重新开始.
     * 0 表示不启用.
     */
    private final long maxIdleMs;

    /**
     * 未解析数据长度上限(字节): 当缓冲区中未解析数据达到该长度时, 当次解析结束后丢弃整段缓冲区.
     * 0 表示不启用.
     */
    private final int maxUnparsedBytes;

    /**
     * 首次出现“有未解析数据”的时刻(毫秒时间戳), 用于空闲超时判断; 重置或全部消费后置为 0.
     */
    private long firstUnconsumedTime;

    /**
     * 累积缓冲区, 用于处理粘包与半包.
     */
    private ByteBuf cumulation;

    private volatile boolean disposed;

    protected AbstractMessageParser() {
        this(DEFAULT_MAX_CUMULATION_BYTES, 0L, 0);
    }

    /**
     * @param maxCumulationBytes 最大累积缓冲区大小 (字节), 超出时抛出异常
     */
    protected AbstractMessageParser(int maxCumulationBytes) {
        this(maxCumulationBytes, 0L, 0);
    }

    /**
     * @param maxCumulationBytes 最大累积缓冲区大小 (字节)
     * @param maxIdleMs          空闲超时(毫秒), 0 表示不启用
     * @param maxUnparsedBytes   未解析数据长度上限(字节), 0 表示不启用
     */
    protected AbstractMessageParser(int maxCumulationBytes, long maxIdleMs, int maxUnparsedBytes) {
        if (maxCumulationBytes <= 0) {
            throw new IllegalArgumentException("maxCumulationBytes must be > 0");
        }
        if (maxIdleMs < 0 || maxUnparsedBytes < 0) {
            throw new IllegalArgumentException("maxIdleMs and maxUnparsedBytes must be >= 0");
        }
        this.maxCumulationBytes = maxCumulationBytes;
        this.maxIdleMs = maxIdleMs;
        this.maxUnparsedBytes = maxUnparsedBytes;
    }

    @Override
    public synchronized List<? extends EncodedMessage> handle(EncodedMessage message) {
        if (disposed) {
            return List.of();
        }
        ByteBuf payload = message.getPayload();
        if (!payload.isReadable()) {
            return List.of();
        }

        // 兜底: 空闲超时 — 若已有未解析数据且超过 maxIdleMs 未消费, 丢弃累积缓冲区
        if (cumulation != null && cumulation.isReadable() && maxIdleMs > 0 && firstUnconsumedTime > 0) {
            long now = System.currentTimeMillis();
            if (now - firstUnconsumedTime >= maxIdleMs) {
                cumulation.release();
                cumulation = null;
                firstUnconsumedTime = 0;
            }
        }

        int incoming = payload.readableBytes();
        int existing = (cumulation == null || !cumulation.isReadable()) ? 0 : cumulation.readableBytes();
        if ((long) existing + (long) incoming > maxCumulationBytes) {
            throw new IllegalStateException("Cumulation buffer exceeds max capacity: " + maxCumulationBytes + " bytes");
        }

        // 累积报文数据到内部缓冲区
        if (cumulation == null || !cumulation.isReadable()) {
            cumulation = payload.alloc().buffer(payload.readableBytes());
            cumulation.writeBytes(payload);
        } else {
            cumulation.ensureWritable(payload.readableBytes());
            cumulation.writeBytes(payload);
        }

        List<ByteBuf> frames = new ArrayList<>();

        // 参考 Netty ByteToMessageDecoder 的循环解析逻辑:
        // 只要每次调用子类 handle 都能输出新的帧, 就继续尝试解析.
        while (cumulation.isReadable()) {
            int outSize = frames.size();
            handle(cumulation, frames);
            if (frames.size() == outSize) {
                // 本次未解析出新帧, 说明需要更多数据
                if (firstUnconsumedTime == 0) {
                    firstUnconsumedTime = System.currentTimeMillis();
                }
                break;
            }
        }

        if (!cumulation.isReadable()) {
            // 数据已完全消费, 释放缓冲区
            cumulation.release();
            cumulation = null;
            firstUnconsumedTime = 0;
        } else {
            // 兜底: 未解析长度上限 — 若剩余未解析数据超过阈值, 丢弃整段缓冲区
            if (maxUnparsedBytes > 0 && cumulation.readableBytes() >= maxUnparsedBytes) {
                cumulation.release();
                cumulation = null;
                firstUnconsumedTime = 0;
            }
        }

        if (frames.isEmpty()) {
            return List.of();
        }

        List<EncodedMessage> messages = new ArrayList<>(frames.size());
        for (ByteBuf frame : frames) {
            messages.add(newMessage(frame));
        }
        return messages;
    }

    /**
     * 子类实现该方法, 从 {@code buf} 中解析完整帧并放入 {@code container}.
     * <p>
     * 典型实现会判断 {@code buf.readableBytes()} 是否足以构成一帧, 若不足则不做任何操作;
     * 若足够, 则从 {@code buf} 读取对应长度的字节构成 {@link ByteBuf} 并加入 {@code container}.
     *
     * @param buf       累积缓冲区
     * @param container 解析出的完整帧
     */
    protected abstract void handle(ByteBuf buf, List<ByteBuf> container);

    /**
     * 将单个帧转换为 {@link EncodedMessage}, 子类可按需重写以扩展消息类型.
     *
     * @param buf 帧数据
     * @return EncodedMessage
     */
    protected EncodedMessage newMessage(ByteBuf buf) {
        return EncodedMessage.simple(buf);
    }

    @Override
    public synchronized void dispose() {
        disposed = true;
        if (cumulation != null) {
            cumulation.release();
            cumulation = null;
        }
    }
}
