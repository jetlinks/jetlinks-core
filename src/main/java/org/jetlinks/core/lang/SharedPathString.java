package org.jetlinks.core.lang;

import org.jetlinks.core.utils.RecyclerUtils;
import org.jetlinks.core.utils.TopicUtils;

/**
 * 共享的路径字符串,与{@link SharedSeparatedString}相同的用途,但是固定使用'/'作为分隔符.
 *
 * @author zhouhao
 * @see RecyclerUtils#intern(Object)
 * @see SharedSeparatedString
 * @see SharedPathString#of(String)
 * @since 1.2.3
 */
public class SharedPathString extends SeparatedString {

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

    public static SharedPathString of(String path) {
        return RecyclerUtils.intern(new SharedPathString(path));
    }

    public static SharedPathString of(String[] path) {
        return RecyclerUtils.intern(new SharedPathString(path));
    }

}
