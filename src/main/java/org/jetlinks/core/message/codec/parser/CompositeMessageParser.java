package org.jetlinks.core.message.codec.parser;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
    private final List<MessageFrameRule.FrameRule> rules;

    public CompositeMessageParser(List<MessageFrameRule.FrameRule> rules) {
        Objects.requireNonNull(rules, "rules");
        this.rules = Collections.unmodifiableList(new ArrayList<>(rules));
    }

    public CompositeMessageParser(MessageFrameRule.FrameRule... rules) {
        this(Arrays.asList(rules));
    }

    /**
     * 创建一个组合解析器实例.
     *
     * @param rules 规则列表
     * @return 解析器
     */
    public static CompositeMessageParser of(List<MessageFrameRule.FrameRule> rules) {
        return new CompositeMessageParser(rules);
    }

    /**
     * 创建一个组合解析器实例.
     *
     * @param rules 规则列表
     * @return 解析器
     */
    public static CompositeMessageParser of(MessageFrameRule.FrameRule... rules) {
        return new CompositeMessageParser(rules);
    }

    @Override
    protected void handle(ByteBuf buf, List<ByteBuf> container) {
        if (!buf.isReadable() || rules.isEmpty()) {
            return;
        }

        boolean parsed = false;

        // 每次调用尝试解析一帧, 若解析出一帧则由外层循环继续调用.
        int readerIndex = buf.readerIndex();
        for (MessageFrameRule.FrameRule rule : rules) {
            if (!buf.isReadable()) {
                break;
            }
            // matcher 与 parser 内部都不应修改 readerIndex, 通过 mark/reset 保证安全
            buf.markReaderIndex();
            try {
                if (!rule.match(buf)) {
                    buf.resetReaderIndex();
                    continue;
                }
                ByteBuf frame = rule.parse(buf);
                if (frame == null) {
                    // 数据不足, 回滚等待下次
                    buf.resetReaderIndex();
                    return;
                }
                parsed = true;
                container.add(frame);
                break;
            } catch (Throwable e) {
                // 规则内部异常时, 回滚到进入规则前的状态, 尝试下一个规则
                buf.resetReaderIndex();
            }
        }

        if (!parsed) {
            // 没有任何规则能匹配当前数据, 回退 readerIndex 等待更多数据或上层处理.
            buf.readerIndex(readerIndex);
        }
    }

}

