package org.jetlinks.core.message.codec.parser.rule;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.message.codec.parser.MessageFrameRule;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * JSON 粘拆包规则（对象/数组）.
 * <p>
 * 约定:
 * <ul>
 *     <li>仅将以 <code>{</code> 或 <code>[</code> 开头的 JSON 视为一帧;</li>
 *     <li>支持嵌套对象/数组;</li>
 *     <li>支持字符串内转义字符, 不会将字符串中的括号误判为结构符号;</li>
 *     <li>遇到非法结构时会尝试在后续字节中重同步到下一个可能的 JSON 起始符.</li>
 * </ul>
 */
public class JSONMessageFrameRule implements MessageFrameRule {

    private static final int PARSE_NEED_MORE = -1;
    private static final int PARSE_MALFORMED = -2;
    public static final JSONMessageFrameRule INSTANCE = new JSONMessageFrameRule();

    private final Predicate<ByteBuf> matcher;

    public JSONMessageFrameRule() {
        this(buf -> true);
    }

    public JSONMessageFrameRule(Predicate<ByteBuf> matcher) {
        this.matcher = Objects.requireNonNull(matcher, "matcher");
    }

    @Override
    public ParseResult parse(ByteBuf buf) {
        if (!matcher.test(buf)) {
            return ParseResult.notMatch();
        }
        int start = buf.readerIndex();
        int end = start + buf.readableBytes();
        if (start >= end) {
            return ParseResult.needMore(start);
        }

        int candidate = findNextStart(buf, start, end);
        while (candidate >= 0) {
            int frameEnd = tryParseFrameEnd(buf, candidate, end);
            if (frameEnd >= 0) {
                int frameLength = frameEnd - candidate + 1;
                buf.readerIndex(candidate);
                ByteBuf frame = buf.readRetainedSlice(frameLength);
                return ParseResult.success(candidate, frame);
            }
            if (frameEnd == PARSE_NEED_MORE) {
                return ParseResult.needMore(candidate);
            }
            candidate = findNextStart(buf, candidate + 1, end);
        }
        return ParseResult.notMatch();
    }

    private int findNextStart(ByteBuf buf, int from, int endExclusive) {
        for (int i = from; i < endExclusive; i++) {
            byte b = buf.getByte(i);
            if (b == '{' || b == '[') {
                return i;
            }
        }
        return -1;
    }

    private int tryParseFrameEnd(ByteBuf buf, int start, int endExclusive) {
        int maxDepth = endExclusive - start;
        if (maxDepth <= 0) {
            return PARSE_NEED_MORE;
        }
        byte[] expectedClosers = new byte[maxDepth];
        int top = 0;
        byte first = buf.getByte(start);
        expectedClosers[top++] = first == '{' ? (byte) '}' : (byte) ']';

        boolean inString = false;
        boolean escaped = false;
        for (int i = start + 1; i < endExclusive; i++) {
            byte b = buf.getByte(i);

            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (b == '\\') {
                    escaped = true;
                } else if (b == '"') {
                    inString = false;
                }
                continue;
            }

            if (b == '"') {
                inString = true;
                continue;
            }
            if (b == '{') {
                expectedClosers[top++] = '}';
                continue;
            }
            if (b == '[') {
                expectedClosers[top++] = ']';
                continue;
            }
            if (b == '}' || b == ']') {
                if (top == 0) {
                    return PARSE_MALFORMED;
                }
                byte expected = expectedClosers[--top];
                if (expected != b) {
                    return PARSE_MALFORMED;
                }
                if (top == 0) {
                    return i;
                }
            }
        }
        return PARSE_NEED_MORE;
    }
}
