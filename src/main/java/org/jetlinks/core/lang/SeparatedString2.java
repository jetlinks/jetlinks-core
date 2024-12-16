package org.jetlinks.core.lang;

import lombok.AllArgsConstructor;
import org.jetlinks.core.utils.StringBuilderUtils;

import javax.annotation.Nonnull;

@AllArgsConstructor
class SeparatedString2 extends AbstractSeparatedCharSequence {

    private final char separator;
    private final CharSequence s1, s2;

    @Override
    protected char separator() {
        return separator;
    }

    @Override
    protected int size0() {
        return 2;
    }

    @Override
    protected CharSequence get0(int index) {
        if (index == 0) {
            return s1;
        }
        if (index == 1) {
            return s2;
        }
        throw new StringIndexOutOfBoundsException(index);
    }

    @Override
    public int length() {
        return s1.length() + s2.length() + 1;
    }

    @Override
    public char charAt(int index) {
        int s1Length = s1.length();
        if (index < s1Length) {
            return s1.charAt(index);
        }
        if (index == s1Length) {
            return separator;
        }
        int s2Length = s2.length();
        int idx = index - s1Length - 1;
        if (idx < s2Length) {
            return s2.charAt(idx);
        }
        throw new StringIndexOutOfBoundsException(index);
    }

    @Override
    @Nonnull
    public CharSequence subSequence(int start, int end) {
        return toString().subSequence(start, end);
    }

    @Override
    @Nonnull
    public String toString() {
        return StringBuilderUtils.buildString(
            s1,
            separator,
            s2,
            (a, b, c, bd) -> {
                bd.append(a).append(b).append(c);
            }
        );
    }
}
