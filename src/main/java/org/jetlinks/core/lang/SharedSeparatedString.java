package org.jetlinks.core.lang;

import lombok.EqualsAndHashCode;
import org.jetlinks.core.utils.RecyclerUtils;
import org.jetlinks.core.utils.TopicUtils;

/**
 * 共享的分隔字符串.当有大量可能存在部分重复的字符串缓存时,可以使用此类来减少内存占用.
 * <p>
 * 将一个字符串按分隔符分割为字符串数组,然后将字符串数组中的元素缓存共享,以减少内存占用.
 *
 * @author zhouhao
 * @see RecyclerUtils#intern(Object)
 * @see SharedSeparatedString#of(char, String)
 * @since 1.2.3
 */
@EqualsAndHashCode(of = "separator",
    callSuper = true,
    cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY)
public class SharedSeparatedString extends SeparatedString {

    final char separator;

    SharedSeparatedString(char separator, String[] separated) {
        super(separated);
        this.separator = separator;
    }

    public static SharedSeparatedString of(char separator, String[] string) {
        return RecyclerUtils.intern(new SharedSeparatedString(separator, string));
    }

    @Override
    protected char separator() {
        return separator;
    }
}
