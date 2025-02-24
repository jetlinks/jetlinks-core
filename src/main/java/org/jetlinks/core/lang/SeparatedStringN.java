package org.jetlinks.core.lang;

import org.jetlinks.core.utils.RecyclerUtils;

/**
 * 共享的分隔字符串.当有大量可能存在部分重复的字符串缓存时,可以使用此类来减少内存占用.
 * <p>
 * 将一个字符串按分隔符分割为字符串数组,然后将字符串数组中的元素缓存共享,以减少内存占用.
 *
 * @author zhouhao
 * @see RecyclerUtils#intern(Object)
 * @see SeparatedStringN#of(char, String)
 * @since 1.2.3
 */
class SeparatedStringN extends SeparatedString {

    final char separator;

    SeparatedStringN(char separator, String[] separated) {
        super(separated);
        this.separator = separator;
    }

    public static SeparatedStringN of(char separator, String[] string) {
        return new SeparatedStringN(separator, string);
    }

    @Override
    public char separator() {
        return separator;
    }
}
