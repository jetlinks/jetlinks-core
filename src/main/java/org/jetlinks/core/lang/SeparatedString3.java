package org.jetlinks.core.lang;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.jetlinks.core.utils.StringBuilderUtils;

import javax.annotation.Nonnull;

@AllArgsConstructor
@EqualsAndHashCode(cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY)
class SeparatedString3 implements CharSequence {

    private final char separator;
    private final CharSequence s1, s2, s3;

    @Override
    public int length() {
        return s1.length() + s2.length() + s3.length() + 2;
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
        if (idx == s2Length) {
            return separator;
        }

        int s3Length = s3.length();
        idx = idx - s2Length - 1;
        if (idx < s3Length) {
            return s3.charAt(idx);
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
            separator,
            s1,
            s2,
            s3,
            (s, c1, c2, c3, bd) -> {
                bd.append(c1).append(s).append(c2).append(s).append(c3);
            }
        );
    }
}
