package org.jetlinks.core.lang;

import lombok.EqualsAndHashCode;
import org.jetlinks.core.utils.StringBuilderUtils;

import javax.annotation.Nonnull;

@EqualsAndHashCode(of = "separated")
abstract class SeparatedString implements CharSequence, Comparable<SeparatedString> {
    protected final String[] separated;

    protected abstract char separator();

    protected int index() {
        return 0;
    }

    protected int separatedLength() {
        return separated.length;
    }

    SeparatedString(String[] separated) {
        this.separated = separated;
    }

    public String[] separated() {
        return separated.clone();
    }

    public String[] unsafeSeparated() {
        return separated;
    }

    @Override
    public int compareTo(SeparatedString o) {

        if (index() != o.index()) {
            return index() - o.index();
        }

        if (separatedLength() != o.separatedLength()) {
            return separatedLength() - o.separatedLength();
        }

        if (separator() != o.separator()) {
            return separator() - o.separator();
        }
        if (separated.length != o.separated.length) {
            return separated.length - o.separated.length;
        }
        for (int i = 0; i < separated.length; i++) {
            int c = separated[i].compareTo(o.separated[i]);
            if (c != 0) {
                return c;
            }
        }
        return 0;
    }

    @Override
    public int length() {
        int len = 0;
        for (int i = index(); i < separatedLength(); i++) {
            len += separated[i].length();
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
                int index = self.index();
                for (int i = index; i < self.separatedLength(); i++) {
                    if (i > index) {
                        sb.append(sp);
                    }
                    sb.append(self.separated[i]);
                }
            });
    }
}
