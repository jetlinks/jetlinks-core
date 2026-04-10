package org.jetlinks.core.message.codec.parser.rule;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.message.codec.parser.MessageFrameRule;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * XML 粘拆包规则.
 * <p>
 * 支持:
 * <ul>
 *     <li>普通元素嵌套与自闭合标签;</li>
 *     <li>处理指令（<? ... ?>）;</li>
 *     <li>注释（<!-- ... -->）与 CDATA（<![CDATA[ ... ]]>）;</li>
 *     <li>DOCTYPE（含内部子集 [ ... ]）.</li>
 * </ul>
 */
public class XMLMessageFrameRule implements MessageFrameRule {

    private static final int PARSE_NEED_MORE = -1;
    private static final int PARSE_MALFORMED = -2;

    public static final XMLMessageFrameRule INSTANCE = new XMLMessageFrameRule();

    private final Predicate<ByteBuf> matcher;

    public XMLMessageFrameRule() {
        this(buf -> true);
    }

    public XMLMessageFrameRule(Predicate<ByteBuf> matcher) {
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

        int candidate = findNextChar(buf, start, end, '<');
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
            candidate = findNextChar(buf, candidate + 1, end, '<');
        }
        return ParseResult.notMatch();
    }

    private int tryParseFrameEnd(ByteBuf buf, int start, int endExclusive) {
        int i = start;
        int elementDepth = 0;
        boolean rootSeen = false;

        while (i < endExclusive) {
            int lt = findNextChar(buf, i, endExclusive, '<');
            if (lt < 0) {
                return PARSE_NEED_MORE;
            }
            if (lt + 1 >= endExclusive) {
                return PARSE_NEED_MORE;
            }

            if (startsWith(buf, lt, endExclusive, "<!--")) {
                int close = findSubsequence(buf, lt + 4, endExclusive, "-->");
                if (close < 0) {
                    return PARSE_NEED_MORE;
                }
                i = close + 3;
                continue;
            }

            if (startsWith(buf, lt, endExclusive, "<![CDATA[")) {
                int close = findSubsequence(buf, lt + 9, endExclusive, "]]>");
                if (close < 0) {
                    return PARSE_NEED_MORE;
                }
                i = close + 3;
                continue;
            }

            if (startsWith(buf, lt, endExclusive, "<?")) {
                int close = findSubsequence(buf, lt + 2, endExclusive, "?>");
                if (close < 0) {
                    return PARSE_NEED_MORE;
                }
                i = close + 2;
                continue;
            }

            if (startsWithIgnoreCase(buf, lt, endExclusive, "<!DOCTYPE")) {
                int close = findDoctypeEnd(buf, lt + 9, endExclusive);
                if (close < 0) {
                    return close == PARSE_MALFORMED ? PARSE_MALFORMED : PARSE_NEED_MORE;
                }
                i = close + 1;
                continue;
            }

            if (startsWith(buf, lt, endExclusive, "</")) {
                int gt = findSimpleTagEnd(buf, lt + 2, endExclusive);
                if (gt < 0) {
                    return PARSE_NEED_MORE;
                }
                if (elementDepth <= 0) {
                    return PARSE_MALFORMED;
                }
                elementDepth--;
                i = gt + 1;
                if (rootSeen && elementDepth == 0) {
                    return gt;
                }
                continue;
            }

            if (startsWith(buf, lt, endExclusive, "<!")) {
                int gt = findTagEnd(buf, lt + 2, endExclusive);
                if (gt < 0) {
                    return PARSE_NEED_MORE;
                }
                i = gt + 1;
                continue;
            }

            int gt = findTagEnd(buf, lt + 1, endExclusive);
            if (gt < 0) {
                return PARSE_NEED_MORE;
            }
            boolean selfClosing = isSelfClosingTag(buf, lt, gt);
            rootSeen = true;
            if (!selfClosing) {
                elementDepth++;
            } else if (elementDepth == 0) {
                return gt;
            }
            i = gt + 1;
        }
        return PARSE_NEED_MORE;
    }

    private int findDoctypeEnd(ByteBuf buf, int from, int endExclusive) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        int subsetDepth = 0;
        for (int i = from; i < endExclusive; i++) {
            byte b = buf.getByte(i);
            if (!inDoubleQuote && b == '\'') {
                inSingleQuote = !inSingleQuote;
                continue;
            }
            if (!inSingleQuote && b == '"') {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }
            if (inSingleQuote || inDoubleQuote) {
                continue;
            }
            if (b == '[') {
                subsetDepth++;
            } else if (b == ']') {
                if (subsetDepth > 0) {
                    subsetDepth--;
                } else {
                    return PARSE_MALFORMED;
                }
            } else if (b == '>' && subsetDepth == 0) {
                return i;
            }
        }
        return PARSE_NEED_MORE;
    }

    private int findSimpleTagEnd(ByteBuf buf, int from, int endExclusive) {
        for (int i = from; i < endExclusive; i++) {
            if (buf.getByte(i) == '>') {
                return i;
            }
        }
        return -1;
    }

    private int findTagEnd(ByteBuf buf, int from, int endExclusive) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int i = from; i < endExclusive; i++) {
            byte b = buf.getByte(i);
            if (!inDoubleQuote && b == '\'') {
                inSingleQuote = !inSingleQuote;
                continue;
            }
            if (!inSingleQuote && b == '"') {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }
            if (!inSingleQuote && !inDoubleQuote && b == '>') {
                return i;
            }
        }
        return -1;
    }

    private boolean isSelfClosingTag(ByteBuf buf, int lt, int gt) {
        for (int i = gt - 1; i > lt; i--) {
            byte b = buf.getByte(i);
            if (b == ' ' || b == '\t' || b == '\r' || b == '\n') {
                continue;
            }
            return b == '/';
        }
        return false;
    }

    private int findSubsequence(ByteBuf buf, int from, int endExclusive, String needle) {
        int n = needle.length();
        int end = endExclusive - n + 1;
        for (int i = from; i < end; i++) {
            boolean match = true;
            for (int j = 0; j < n; j++) {
                if (buf.getByte(i + j) != needle.charAt(j)) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return i;
            }
        }
        return -1;
    }

    private boolean startsWith(ByteBuf buf, int offset, int endExclusive, String token) {
        if (offset + token.length() > endExclusive) {
            return false;
        }
        for (int i = 0; i < token.length(); i++) {
            if (buf.getByte(offset + i) != token.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private boolean startsWithIgnoreCase(ByteBuf buf, int offset, int endExclusive, String token) {
        if (offset + token.length() > endExclusive) {
            return false;
        }
        for (int i = 0; i < token.length(); i++) {
            byte actual = buf.getByte(offset + i);
            char expected = token.charAt(i);
            if (Character.toLowerCase((char) actual) != Character.toLowerCase(expected)) {
                return false;
            }
        }
        return true;
    }

    private int findNextChar(ByteBuf buf, int from, int endExclusive, char ch) {
        for (int i = from; i < endExclusive; i++) {
            if (buf.getByte(i) == ch) {
                return i;
            }
        }
        return -1;
    }
}
