package org.jetlinks.core.lang;

import lombok.EqualsAndHashCode;
import org.jetlinks.core.utils.RecyclerUtils;
import org.jetlinks.core.utils.StringBuilderUtils;
import org.jetlinks.core.utils.TopicUtils;

import javax.annotation.Nonnull;

@EqualsAndHashCode(of = "separated")
public abstract class SeparatedString implements CharSequence, Comparable<SeparatedString> {
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

    public static CharSequence of(char separator, String string) {
        String[] arr = TopicUtils.split(string, separator);
        for (int i = 0; i < arr.length; i++) {
            arr[i] = RecyclerUtils.intern(arr[i]);
        }
        if (arr.length == 1) {
            return arr[0];
        }
        if (arr.length == 2) {
            return RecyclerUtils.intern(new SeparatedString2(separator, arr[0], arr[1]));
        }
        if (arr.length == 3) {
            return RecyclerUtils.intern(new SeparatedString3(separator, arr[0], arr[1], arr[2]));
        }
        return SharedSeparatedString.of(separator, arr);
    }

    @Override
    public int compareTo(SeparatedString o) {

        if (index() != o.index()) {
            return Integer.compare(index(), o.index());
        }

        if (separatedLength() != o.separatedLength()) {
            return Integer.compare(separatedLength(), o.separatedLength());
        }

        if (separator() != o.separator()) {
            return Character.compare(separator(), o.separator());
        }
        if (separated.length != o.separated.length) {
            return Integer.compare(separated.length, o.separated.length);
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
