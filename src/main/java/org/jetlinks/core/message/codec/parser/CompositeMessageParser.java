package org.jetlinks.core.message.codec.parser;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 组合式粘拆包解析器, 基于 {@link AbstractMessageParser} 实现, 支持按不同规则解析同一条报文流,
 * 以适配如「注册帧」「心跳帧」「业务帧」等拥有不同粘拆包规则的场景。
 * <p>
 * 典型用法:
 * <pre>{@code
 * CompositeMessageParser parser = CompositeMessageParser.of(
 *     // 注册帧规则
 *     CompositeMessageParser.rule(
 *         buf -> buf.readableBytes() >= 2 && buf.getUnsignedShort(buf.readerIndex()) == 0x1001,
 *         buf -> {
 *             if (buf.readableBytes() < 10) { // 注册帧固定长度 10
 *                 return null;
 *             }
 *             return buf.readRetainedSlice(10);
 *         }
 *     ),
 *     // 心跳帧规则
 *     CompositeMessageParser.rule(
 *         buf -> buf.readableBytes() >= 2 && buf.getUnsignedShort(buf.readerIndex()) == 0x1002,
 *         buf -> {
 *             if (buf.readableBytes() < 6) { // 心跳帧固定长度 6
 *                 return null;
 *             }
 *             return buf.readRetainedSlice(6);
 *         }
 *     ),
 *     // 业务帧规则(长度在报文体中携带)
 *     CompositeMessageParser.rule(
 *         buf -> buf.readableBytes() >= 4 && buf.getUnsignedShort(buf.readerIndex()) == 0x2001,
 *         buf -> {
 *             if (buf.readableBytes() < 6) {
 *                 return null;
 *             }
 *             int startIdx = buf.readerIndex();
 *             // 假设第 2~3 字节为长度字段(不含头本身)
 *             int len = buf.getUnsignedShort(startIdx + 2);
 *             int frameLength = 4 + len; // 2 字节命令 + 2 字节长度 + len 字节数据
 *             if (buf.readableBytes() < frameLength) {
 *                 return null;
 *             }
 *             return buf.readRetainedSlice(frameLength);
 *         }
 *     )
 * );
 * }</pre>
 *
 * @author zhouhao
 * @since 1.3.2
 */
public class CompositeMessageParser extends AbstractMessageParser {

    /**
     * 解析规则列表, 将按顺序依次尝试匹配.
     */
    private final List<MessageFrameRule> rules;

    /**
     * 当检测到无效数据被丢弃时的回调监听器.
     * 比如可以在这里打印日志、统计丢弃字节数等。
     * <p>
     * 传入的 {@link ByteBuf} 为只读切片，只在当前调用栈内有效，
     * 监听器不得修改其 readerIndex/writeIndex，也不应跨线程保存引用。
     */
    private final Consumer<ByteBuf> discardListener;

    public CompositeMessageParser(List<MessageFrameRule> rules) {
        this(rules, null, 0L, 0);
    }

    public CompositeMessageParser(List<MessageFrameRule> rules, Consumer<ByteBuf> discardListener) {
        this(rules, discardListener, 0L, 0);
    }

    /**
     * @param rules            规则列表
     * @param discardListener  丢弃数据监听器, 可为 null
     * @param maxIdleMs        空闲超时(毫秒), 0 表示不启用
     * @param maxUnparsedBytes 未解析数据长度上限(字节), 0 表示不启用
     */
    public CompositeMessageParser(List<MessageFrameRule> rules, Consumer<ByteBuf> discardListener,
                                  long maxIdleMs, int maxUnparsedBytes) {
        this(rules, discardListener, AbstractMessageParser.DEFAULT_MAX_CUMULATION_BYTES, maxIdleMs, maxUnparsedBytes);
    }

    /**
     * @param rules               规则列表
     * @param discardListener     丢弃数据监听器, 可为 null
     * @param maxCumulationBytes  累积缓冲区上限(字节)
     * @param maxIdleMs           空闲超时(毫秒), 0 表示不启用
     * @param maxUnparsedBytes    未解析数据长度上限(字节), 0 表示不启用
     */
    public CompositeMessageParser(List<MessageFrameRule> rules, Consumer<ByteBuf> discardListener,
                                  int maxCumulationBytes, long maxIdleMs, int maxUnparsedBytes) {
        super(maxCumulationBytes, maxIdleMs, maxUnparsedBytes);
        Objects.requireNonNull(rules, "rules");
        this.rules = Collections.unmodifiableList(new ArrayList<>(rules));
        this.discardListener = discardListener;
    }

    public CompositeMessageParser(MessageFrameRule... rules) {
        this(Arrays.asList(rules), null);
    }

    public CompositeMessageParser(Consumer<ByteBuf> discardListener, MessageFrameRule... rules) {
        this(Arrays.asList(rules), discardListener);
    }

    /**
     * 创建一个组合解析器实例.
     *
     * @param rules 规则列表
     * @return 解析器
     */
    public static CompositeMessageParser of(List<MessageFrameRule> rules) {
        return new CompositeMessageParser(rules);
    }

    public static CompositeMessageParser of(List<MessageFrameRule> rules, Consumer<ByteBuf> discardListener) {
        return new CompositeMessageParser(rules, discardListener);
    }

    /**
     * 创建带兜底策略的组合解析器.
     *
     * @param rules            规则列表
     * @param discardListener  丢弃数据监听器, 可为 null
     * @param maxIdleMs        空闲超时(毫秒), 0 表示不启用
     * @param maxUnparsedBytes 未解析数据长度上限(字节), 0 表示不启用
     * @return 解析器
     */
    public static CompositeMessageParser of(List<MessageFrameRule> rules, Consumer<ByteBuf> discardListener,
                                           long maxIdleMs, int maxUnparsedBytes) {
        return new CompositeMessageParser(rules, discardListener, maxIdleMs, maxUnparsedBytes);
    }

    /**
     * 创建带完整兜底配置的组合解析器.
     *
     * @param rules               规则列表
     * @param discardListener     丢弃数据监听器, 可为 null
     * @param maxCumulationBytes  累积缓冲区上限(字节)
     * @param maxIdleMs           空闲超时(毫秒), 0 表示不启用
     * @param maxUnparsedBytes    未解析数据长度上限(字节), 0 表示不启用
     * @return 解析器
     */
    public static CompositeMessageParser of(List<MessageFrameRule> rules, Consumer<ByteBuf> discardListener,
                                           int maxCumulationBytes, long maxIdleMs, int maxUnparsedBytes) {
        return new CompositeMessageParser(rules, discardListener, maxCumulationBytes, maxIdleMs, maxUnparsedBytes);
    }

    /**
     * 创建一个组合解析器实例.
     *
     * @param rules 规则列表
     * @return 解析器
     */
    public static CompositeMessageParser of(MessageFrameRule... rules) {
        return new CompositeMessageParser(rules);
    }

    public static CompositeMessageParser of(Consumer<ByteBuf> discardListener, MessageFrameRule... rules) {
        return new CompositeMessageParser(discardListener, rules);
    }

    @Override
    protected void handle(ByteBuf buf, List<ByteBuf> container) {
        if (!buf.isReadable() || rules.isEmpty()) {
            return;
        }

        while (buf.isReadable()) {
            boolean parsed = false;
            int originalReaderIndex = buf.readerIndex();
            int minStartIndex = -1;

            for (MessageFrameRule rule : rules) {
                buf.readerIndex(originalReaderIndex);
                MessageFrameRule.ParseResult result = rule.parse(buf);

                if (result.frame != null) {
                    container.add(result.frame);
                    parsed = true;
                    break;
                }

                if (result.startIndex >= 0) {
                    if (minStartIndex == -1 || result.startIndex < minStartIndex) {
                        minStartIndex = result.startIndex;
                    }
                }
            }

            if (parsed) {
                continue;
            }

            if (minStartIndex == -1) {
                // 没有任何规则匹配，且没有规则提示需要更多数据（notMatch）
                // 这种情况下，如果不消耗字节会死循环。
                // 理论上由上层决定是否丢弃，但为了保持继续，这里跳过 1 字节。
                // 在丢弃前先通知监听器。
                if (discardListener != null) {
                    ByteBuf discarded = buf.slice(originalReaderIndex, 1).asReadOnly();
                    discardListener.accept(discarded);
                }
                buf.readerIndex(originalReaderIndex + 1);
                continue;
            }

            // 有规则匹配到了头部但数据不足
            // 如果头部在当前位置之后，可以跳过之前的无效数据
            if (minStartIndex > originalReaderIndex) {
                if (discardListener != null) {
                    int discardLen = minStartIndex - originalReaderIndex;
                    if (discardLen > 0) {
                        ByteBuf discarded = buf.slice(originalReaderIndex, discardLen).asReadOnly();
                        discardListener.accept(discarded);
                    }
                }
                buf.readerIndex(minStartIndex);
                continue;
            }

            // 头部就在当前位置或之前，回退并等待更多数据
            buf.readerIndex(originalReaderIndex);
            break;
        }
    }

}

