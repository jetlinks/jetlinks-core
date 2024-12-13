package org.jetlinks.core.lang;

import org.jetlinks.core.utils.RecyclerUtils;
import org.jetlinks.core.utils.TopicUtils;

public abstract class SeparatedString extends AbstractSeparatedCharSequence {
    protected final String[] separated;

    SeparatedString(String[] separated) {
        this.separated = separated;
    }

    public static CharSequence of(char separator, String string, boolean intern) {
        String[] arr = TopicUtils.split(string, separator);
        if (intern) {
            for (int i = 0; i < arr.length; i++) {
                arr[i] = RecyclerUtils.intern(arr[i]);
            }
        }
        if (arr.length == 1) {
            return arr[0];
        }
        if (separator == '/') {
            return intern ? RecyclerUtils.intern(SharedPathString.of(arr)) : SharedPathString.of(arr);
        }
        if (arr.length == 2) {
            return intern ? RecyclerUtils.intern(new SeparatedString2(separator, arr[0], arr[1])) :
                new SeparatedString2(separator, arr[0], arr[1]);
        }
        if (arr.length == 3) {
            return intern ? RecyclerUtils.intern(new SeparatedString3(separator, arr[0], arr[1], arr[2])) :
                new SeparatedString3(separator, arr[0], arr[1], arr[2]);
        }
        return SeparatedStringN.of(separator, arr);
    }

    public static CharSequence of(char separator, String string) {
        return of(separator, string, true);
    }

    protected abstract char separator();

    protected int size0() {
        return separated.length;
    }

    protected String get0(int index) {
        return separated[index];
    }

    public String[] separated() {
        return separated.clone();
    }

    public String[] unsafeSeparated() {
        return separated;
    }


}
