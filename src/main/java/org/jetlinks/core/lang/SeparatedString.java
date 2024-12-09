package org.jetlinks.core.lang;

import lombok.EqualsAndHashCode;
import org.jetlinks.core.utils.StringBuilderUtils;

import javax.annotation.Nonnull;

@EqualsAndHashCode(of = "separated")
abstract class SeparatedString implements CharSequence {
    protected final String[] separated;

    protected abstract char separator();

    SeparatedString(String[] separated) {
        this.separated = separated;
    }

    @Override
    public int length() {
        int len = 0;
        for (String s : separated) {
            len += s.length();
            len++;
        }
        return len - 1;
    }

    @Override
    public char charAt(int index) {
        return toString().charAt(index);
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
            this,
            (self, sb) -> {
                char sp = self.separator();
                for (int i = 0; i < self.separated.length; i++) {
                    if (i > 0) {
                        sb.append(sp);
                    }
                    sb.append(self.separated[i]);
                }
            });
    }
}
