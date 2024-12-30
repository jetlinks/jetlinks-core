package org.jetlinks.core.lang;

import lombok.AllArgsConstructor;
import org.jetlinks.core.utils.StringBuilderUtils;

import javax.annotation.Nonnull;
import java.util.Objects;

@AllArgsConstructor
class SeparatedString2 extends AbstractSeparatedCharSequence {

    private final char separator;
    private final CharSequence s1, s2;

    @Override
    public char separator() {
        return separator;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public int size() {
        return 2;
    }

    @Override
    public CharSequence get(int index) {
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SeparatedString2)) {
            return false;
        }
        SeparatedString2 another = ((SeparatedString2) obj);
        if (another.separator() != separator()) {
            return false;
        }
        return Objects.equals(s2, another.s2)
            && Objects.equals(s1, another.s1);
    }
}
