package org.jetlinks.core.lang;

import org.jetlinks.core.utils.RecyclerUtils;
import org.jetlinks.core.utils.TopicUtils;

/**
 * 共享的路径字符串,与{@link SeparatedStringN}相同的用途,但是固定使用'/'作为分隔符.
 *
 * @author zhouhao
 * @see RecyclerUtils#intern(Object)
 * @see SeparatedStringN
 * @see SharedPathString#of(String)
 * @since 1.2.3
 */
public class SharedPathString extends SeparatedString {

    private static final SharedPathString EMPTY = new SharedPathString(new String[0]);

    @Override
    protected final char separator() {
        return '/';
    }

    SharedPathString(String[] topic) {
        super(topic);
    }

    SharedPathString(String topic) {
        this(TopicUtils.split(topic, true, true));
    }

    public SharedPathString intern() {
        return RecyclerUtils.intern(this);
    }

    public static SharedPathString empty() {
        return EMPTY;
    }

    public static SharedPathString of(String path, boolean intern) {
        return intern
            ? new SharedPathString(path).intern()
            :of(path);
    }

    public static SharedPathString of(CharSequence path) {
        if (path instanceof SharedPathString) {
            return (SharedPathString) path;
        }
        if (path instanceof SeparatedString) {
            return of(((SeparatedString) path).unsafeSeparated());
        }
        if (path instanceof SeparatedCharSequence) {
            return of(((SeparatedCharSequence) path).asStringArray());
        }
        return of(String.valueOf(path));
    }

    public static SharedPathString of(String path) {
        String[] arr = TopicUtils.split(path);
        for (int i = 0; i < arr.length; i++) {
            arr[i] = RecyclerUtils.intern(arr[i]);
        }
        return of(arr);
    }

    public static SharedPathString of(String[] path) {
        return new SharedPathString(path);
    }

}
