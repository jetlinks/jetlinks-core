package org.jetlinks.core.message.codec.parser;

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

/**
 * {@link MessageParser} 构建器, 基于 DSL 方式组合并复用各类 {@link MessageFrameRule.FrameRule}.
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
 * @author zhouhao
 * @since 1.3.2
 */
public final class MessageParserBuilder {

    private final List<MessageFrameRule.FrameRule> rules = new ArrayList<>();

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
    public MessageParserBuilder addRule(MessageFrameRule.FrameRule rule) {
        Objects.requireNonNull(rule, "rule");
        this.rules.add(rule);
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
        return CompositeMessageParser.of(rules);
    }
}

