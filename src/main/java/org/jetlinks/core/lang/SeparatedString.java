package org.jetlinks.core.lang;

import org.jetlinks.core.utils.RecyclerUtils;
import org.jetlinks.core.utils.TopicUtils;

public abstract class SeparatedString extends AbstractSeparatedCharSequence {
    protected final String[] separated;

    SeparatedString(String[] separated) {
        this.separated = separated;
    }

    public static SeparatedCharSequence create(char separator, String... arr) {
        if (separator == '/') {
            return SharedPathString.of(arr);
        }
        if (arr.length == 2) {
            return new SeparatedString2(separator, arr[0], arr[1]);
        }
        if (arr.length == 3) {
            return new SeparatedString3(separator, arr[0], arr[1], arr[2]);
        }
        return new SeparatedStringN(separator, arr);
    }

    public static CharSequence of(char separator, String[] arr) {
        if (arr.length == 1) {
            return arr[0];
        }
        return create(separator, arr);
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
            return intern ? SharedPathString.of(arr).intern() : SharedPathString.of(arr);
        }
        if (arr.length == 2) {
            return intern ? new SeparatedString2(separator, arr[0], arr[1]).intern() :
                new SeparatedString2(separator, arr[0], arr[1]);
        }
        if (arr.length == 3) {
            return intern ? new SeparatedString3(separator, arr[0], arr[1], arr[2]).intern() :
                new SeparatedString3(separator, arr[0], arr[1], arr[2]);
        }
        SeparatedStringN n = new SeparatedStringN(separator, arr);
        return intern ? n.intern() : n;
    }

    public static CharSequence of(char separator, String string) {
        return of(separator, string, true);
    }

    public abstract char separator();

    public int size() {
        return separated.length;
    }

    public String get(int index) {
        return separated[index];
    }

    public String[] separated() {
        return separated.clone();
    }

    public String[] unsafeSeparated() {
        return separated;
    }

    public SeparatedString internInner() {
        for (int i = 0; i < separated.length; i++) {
            separated[i] = RecyclerUtils.intern(separated[i]);
        }
        return this;
    }

    @Override
    public SeparatedString intern() {
        return RecyclerUtils.intern(this);
    }
}
