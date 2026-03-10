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
     * 累积缓冲区, 用于处理粘包与半包.
     */
    private ByteBuf cumulation;

    private volatile boolean disposed;

    protected AbstractMessageParser() {
        this(DEFAULT_MAX_CUMULATION_BYTES);
    }

    /**
     * @param maxCumulationBytes 最大累积缓冲区大小 (字节), 超出时抛出异常
     */
    protected AbstractMessageParser(int maxCumulationBytes) {
        if (maxCumulationBytes <= 0) {
            throw new IllegalArgumentException("maxCumulationBytes must be > 0");
        }
        this.maxCumulationBytes = maxCumulationBytes;
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
                break;
            }
        }

        if (!cumulation.isReadable()) {
            // 数据已完全消费, 释放缓冲区
            cumulation.release();
            cumulation = null;
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
