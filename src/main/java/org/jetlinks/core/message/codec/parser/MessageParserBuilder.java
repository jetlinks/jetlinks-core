package org.jetlinks.core.message.codec.parser;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.message.codec.MessageParser;
import org.jetlinks.core.message.codec.parser.rule.DelimiterFrameRule;
import org.jetlinks.core.message.codec.parser.rule.FixedLengthFrameRule;
import org.jetlinks.core.message.codec.parser.rule.LengthFieldFrameRule;
import org.jetlinks.core.message.codec.parser.rule.ModbusRtuFrameRule;
import org.jetlinks.core.message.codec.parser.rule.ModbusTcpFrameRule;
import org.jetlinks.core.message.codec.parser.rule.StartEndFrameRule;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * {@link MessageParser} 构建器, 基于 DSL 方式组合并复用各类 {@link MessageFrameRule}.
 * <p>
 * 典型用法:
 * <pre>{@code
 * MessageParser parser = MessageParserBuilder
 *     .builder()
 *     .startEnd(\"r_\", \"_r\")   // 注册帧: r_..._r
 *     .fixedLength4()            // 心跳: ping
 *     .modbusRtu()               // Modbus RTU
 *     .modbusTcp()               // Modbus TCP
 *     .build();
 * }</pre>
 *
 * <p>内部默认使用 {@link CompositeMessageParser} 实现, 默认累积缓冲区上限为 16MB.</p>
 *
 * <p><b>兜底配置</b>（可选，用于异常或脏数据场景）：</p>
 * <ul>
 *     <li>{@link #maxCumulationBytes(int)} 累积缓冲区上限(字节)，超出抛异常</li>
 *     <li>{@link #maxIdleMs(long)} 空闲超时(毫秒)，超时后丢弃未解析数据</li>
 *     <li>{@link #maxUnparsedBytes(int)} 未解析数据长度上限(字节)，超限后丢弃整段缓冲区</li>
 *     <li>{@link #fallback(long, int)} 一次性设置空闲超时与未解析上限</li>
 * </ul>
 *
 * @author zhouhao
 * @since 1.3.2
 */
public final class MessageParserBuilder {

    private final List<MessageFrameRule> rules = new ArrayList<>();

    /**
     * 当底层 {@link CompositeMessageParser} 丢弃无效数据时的回调监听器.
     * <p>
     * 传入的 {@link ByteBuf} 为只读切片, 仅在当前调用栈内有效,
     * 监听器不得修改其 readerIndex/writeIndex, 也不应跨线程保存引用.
     */
    private Consumer<ByteBuf> discardListener;

    /**
     * 累积缓冲区上限(字节), 0 表示使用默认 16MB. 见 {@link AbstractMessageParser}.
     */
    private int maxCumulationBytes;

    /**
     * 空闲超时(毫秒), 0 表示不启用. 见 {@link AbstractMessageParser}.
     */
    private long maxIdleMs;

    /**
     * 未解析数据长度上限(字节), 0 表示不启用. 见 {@link AbstractMessageParser}.
     */
    private int maxUnparsedBytes;

    private MessageParserBuilder() {
    }

    /**
     * 创建一个新的构建器.
     */
    public static MessageParserBuilder builder() {
        return new MessageParserBuilder();
    }

    /**
     * 直接添加自定义规则.
     *
     * @param rule 规则实现
     * @return this
     */
    public MessageParserBuilder addRule(MessageFrameRule rule) {
        Objects.requireNonNull(rule, "rule");
        this.rules.add(rule);
        return this;
    }

    /**
     * 设置当检测到无效数据被丢弃时的回调监听器.
     * <p>
     * 例如可用于打印日志、统计丢弃的字节数等.
     *
     * @param listener 丢弃数据监听器
     * @return this
     */
    public MessageParserBuilder doOnDiscard(Consumer<ByteBuf> listener) {
        this.discardListener = listener;
        return this;
    }

    /**
     * 设置累积缓冲区上限(字节): 单连接粘包缓冲超过该值时抛出 {@link IllegalStateException}.
     *
     * @param maxCumulationBytes 字节数, 0 表示使用默认 16MB
     * @return this
     */
    public MessageParserBuilder maxCumulationBytes(int maxCumulationBytes) {
        this.maxCumulationBytes = maxCumulationBytes <= 0 ? 0 : maxCumulationBytes;
        return this;
    }

    /**
     * 设置空闲超时(毫秒): 若缓冲区中未解析数据存在超过该时长, 下次 handle 时丢弃累积数据并从新数据重新开始.
     *
     * @param maxIdleMs 毫秒, 0 表示不启用
     * @return this
     */
    public MessageParserBuilder maxIdleMs(long maxIdleMs) {
        this.maxIdleMs = maxIdleMs < 0 ? 0 : maxIdleMs;
        return this;
    }

    /**
     * 设置未解析数据长度上限(字节): 当缓冲区中未解析数据达到该长度时, 当次解析结束后丢弃整段缓冲区.
     *
     * @param maxUnparsedBytes 字节数, 0 表示不启用
     * @return this
     */
    public MessageParserBuilder maxUnparsedBytes(int maxUnparsedBytes) {
        this.maxUnparsedBytes = maxUnparsedBytes < 0 ? 0 : maxUnparsedBytes;
        return this;
    }

    /**
     * 一次性设置兜底策略: 空闲超时与未解析长度上限.
     *
     * @param maxIdleMs        空闲超时(毫秒), 0 表示不启用
     * @param maxUnparsedBytes 未解析数据长度上限(字节), 0 表示不启用
     * @return this
     */
    public MessageParserBuilder fallback(long maxIdleMs, int maxUnparsedBytes) {
        this.maxIdleMs = maxIdleMs < 0 ? 0 : maxIdleMs;
        this.maxUnparsedBytes = maxUnparsedBytes < 0 ? 0 : maxUnparsedBytes;
        return this;
    }

    /**
     * 一次性设置兜底策略: 累积缓冲区上限、空闲超时、未解析长度上限.
     *
     * @param maxCumulationBytes 累积缓冲区上限(字节), 0 表示使用默认 16MB
     * @param maxIdleMs          空闲超时(毫秒), 0 表示不启用
     * @param maxUnparsedBytes   未解析数据长度上限(字节), 0 表示不启用
     * @return this
     */
    public MessageParserBuilder fallback(int maxCumulationBytes, long maxIdleMs, int maxUnparsedBytes) {
        this.maxCumulationBytes = maxCumulationBytes <= 0 ? 0 : maxCumulationBytes;
        this.maxIdleMs = maxIdleMs < 0 ? 0 : maxIdleMs;
        this.maxUnparsedBytes = maxUnparsedBytes < 0 ? 0 : maxUnparsedBytes;
        return this;
    }

    // ----------------------------------------------------------------------
    // 常用规则 DSL
    // ----------------------------------------------------------------------

    /**
     * 固定长度帧.
     *
     * @param length 帧长度
     * @return this
     */
    public MessageParserBuilder fixedLength(int length) {
        return addRule(new FixedLengthFrameRule(length));
    }

    /**
     * 常用 4 字节固定长度规则, 等价于 {@link FixedLengthFrameRule#LENGTH_4}.
     *
     * @return this
     */
    public MessageParserBuilder fixedLength4() {
        return addRule(FixedLengthFrameRule.LENGTH_4);
    }

    /**
     * 自定义起止字节序列的帧规则.
     *
     * @param start 起始标记字符串 (按 ASCII 编码)
     * @param end   结束标记字符串 (按 ASCII 编码)
     * @return this
     */
    public MessageParserBuilder startEnd(String start, String end) {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        return addRule(new StartEndFrameRule(
            start.getBytes(StandardCharsets.US_ASCII),
            end.getBytes(StandardCharsets.US_ASCII)
        ));
    }

    /**
     * 自定义起止字节序列的帧规则.
     *
     * @param start 起始标记字节序列
     * @param end   结束标记字节序列
     * @return this
     */
    public MessageParserBuilder startEnd(byte[] start, byte[] end) {
        return addRule(new StartEndFrameRule(start, end));
    }

    /**
     * 自定义分隔符规则, 帧内包含分隔符.
     *
     * @param delimiter 分隔符字符串 (按 ASCII 编码)
     * @return this
     */
    public MessageParserBuilder delimiter(String delimiter) {
        return delimiter(delimiter, false);
    }

    /**
     * 自定义分隔符规则.
     *
     * @param delimiter        分隔符字符串 (按 ASCII 编码)
     * @param excludeDelimiter 为 true 时解析结果不包含分隔符
     * @return this
     */
    public MessageParserBuilder delimiter(String delimiter, boolean excludeDelimiter) {
        Objects.requireNonNull(delimiter, "delimiter");
        return delimiter(delimiter.getBytes(StandardCharsets.US_ASCII), excludeDelimiter);
    }

    /**
     * 自定义分隔符规则, 帧内包含分隔符.
     *
     * @param delimiter 分隔符字节序列
     * @return this
     */
    public MessageParserBuilder delimiter(byte[] delimiter) {
        return addRule(new DelimiterFrameRule(delimiter));
    }

    /**
     * 自定义分隔符规则.
     *
     * @param delimiter        分隔符字节序列
     * @param excludeDelimiter 为 true 时解析结果不包含分隔符
     * @return this
     */
    public MessageParserBuilder delimiter(byte[] delimiter, boolean excludeDelimiter) {
        return addRule(new DelimiterFrameRule(delimiter, excludeDelimiter));
    }

    /**
     * 以 CRLF 结尾的分隔符规则, 帧内包含分隔符, 复用 {@link DelimiterFrameRule#CRLF}.
     *
     * @return this
     */
    public MessageParserBuilder delimiterCrlf() {
        return addRule(DelimiterFrameRule.CRLF);
    }

    /**
     * 以 CRLF 结尾的分隔符规则, 解析结果不包含分隔符, 复用 {@link DelimiterFrameRule#CRLF_EXCLUDE}.
     *
     * @return this
     */
    public MessageParserBuilder delimiterCrlfExclude() {
        return addRule(DelimiterFrameRule.CRLF_EXCLUDE);
    }

    /**
     * 基于长度字段的通用规则:
     * <ul>
     *     <li>2 字节长度字段, 偏移 2;</li>
     *     <li>头部总长 4 字节 (含长度字段本身);</li>
     *     <li>无尾部.</li>
     * </ul>
     * 复用 {@link LengthFieldFrameRule#HEADER4_LEN2}.
     *
     * @return this
     */
    public MessageParserBuilder lengthFieldHeader4Len2() {
        return addRule(LengthFieldFrameRule.HEADER4_LEN2);
    }

    /**
     * 基于长度字段的通用规则, 头 4 字节 + 长度字段 2 字节 + CRC 尾 2 字节, 复用
     * {@link LengthFieldFrameRule#HEADER4_LEN2_TAIL2}.
     *
     * @return this
     */
    public MessageParserBuilder lengthFieldHeader4Len2Tail2() {
        return addRule(LengthFieldFrameRule.HEADER4_LEN2_TAIL2);
    }

    /**
     * 自定义长度字段规则, 无尾部.
     *
     * @param lengthFieldOffset 长度字段相对帧起始的偏移
     * @param lengthFieldLength 长度字段字节数 (1/2/4)
     * @param headerLength      头部长度 (含长度字段)
     * @return this
     */
    public MessageParserBuilder lengthField(int lengthFieldOffset,
                                            int lengthFieldLength,
                                            int headerLength) {
        return addRule(new LengthFieldFrameRule(lengthFieldOffset, lengthFieldLength, headerLength));
    }

    /**
     * 自定义长度字段规则.
     *
     * @param lengthFieldOffset 长度字段相对帧起始的偏移
     * @param lengthFieldLength 长度字段字节数 (1/2/4)
     * @param headerLength      头部长度 (含长度字段)
     * @param tailLength        尾部长度 (如 CRC 2 字节), 0 表示无尾
     * @return this
     */
    public MessageParserBuilder lengthField(int lengthFieldOffset,
                                            int lengthFieldLength,
                                            int headerLength,
                                            int tailLength) {
        return addRule(new LengthFieldFrameRule(lengthFieldOffset, lengthFieldLength, headerLength, tailLength));
    }

    /**
     * 通用 Modbus RTU 规则, 复用 {@link ModbusRtuFrameRule#INSTANCE}.
     *
     * @return this
     */
    public MessageParserBuilder modbusRtu() {
        return addRule(ModbusRtuFrameRule.INSTANCE);
    }

    /**
     * 通用 Modbus TCP 规则, 复用 {@link ModbusTcpFrameRule#DEFAULT}.
     *
     * @return this
     */
    public MessageParserBuilder modbusTcp() {
        return addRule(ModbusTcpFrameRule.DEFAULT);
    }

    /**
     * 构建最终的 {@link MessageParser} 实例.
     *
     * @return 解析器, 若未添加任何规则则返回一个空规则的 {@link CompositeMessageParser}
     */
    public MessageParser build() {
        int maxCum = maxCumulationBytes > 0 ? maxCumulationBytes : AbstractMessageParser.DEFAULT_MAX_CUMULATION_BYTES;
        return CompositeMessageParser.of(rules, discardListener, maxCum, maxIdleMs, maxUnparsedBytes);
    }
}

