package org.jetlinks.core.lang;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.function.Consumer;

/**
 * 经过分割的字符序列,用于处理类似于topic的字符串
 *
 * @author zhouhao
 * @see SharedPathString
 * @see SeparatedString
 * @since 1.2.3
 */
public interface SeparatedCharSequence extends CharSequence,
    Comparable<SeparatedCharSequence>,
    Iterable<CharSequence>,
    Appendable {

    char separator();

    /**
     * 分割后的数量
     *
     * @return 数量
     */
    int size();

    /**
     * 获取指定位置的字符序列
     *
     * @param index 位置
     * @return 字符序列
     */
    CharSequence get(int index);

    /**
     * 替换指定索引位置的字符序列，并返回新的字符序列,不会修改原字符序列
     *
     * @param index 位置
     * @return 字符序列
     */
    SeparatedCharSequence replace(int index, CharSequence newChar);

    /**
     * 追加字符到字符序列尾部,并返回新的字符序列,不会修改原字符序列
     *
     * @param c 字符
     * @return 字符序列
     */
    @Override
    SeparatedCharSequence append(char c);

    /**
     * 追加字符序列到字符序列尾部,并返回新的字符序列,不会修改原字符序列
     *
     * @param csq 字符序列
     * @return 字符序列
     */
    @Override
    SeparatedCharSequence append(CharSequence csq);

    /**
     * 追加字符序列到字符序列尾部,并返回新的字符序列,不会修改原字符序列
     *
     * @param csq 字符序列
     * @return 字符序列
     */
    SeparatedCharSequence append(CharSequence... csq);

    /**
     * 追加字符序列到字符序列尾部,并返回新的字符序列,不会修改原字符序列
     *
     * @param csq   字符序列
     * @param start 开始位置
     * @param end   结束位置
     * @return 字符序列
     */
    @Override
    SeparatedCharSequence append(CharSequence csq, int start, int end);

    /**
     * 获取指定返回的字符序列
     *
     * @param start  起始位置
     * @param length 获取长度
     * @return 字符序列
     */
    SeparatedCharSequence range(int start, int length);

    /**
     * 将字符序列转换为共享字符序列,相同的字符序列将共享引用.
     *
     * @return 共享字符序列
     */
    SeparatedCharSequence intern();

    /**
     * 将内部的字符序列转换为共享字符序列,相同的字符序列将共享引用.
     *
     * @return 共享字符序列
     */
    default SeparatedCharSequence internInner() {
        return this;
    }

    /**
     * 创建字符串序列迭代器,用于迭代分割后的字符序列
     *
     * @return 迭代器
     */
    @Override
    @Nonnull
    default Iterator<CharSequence> iterator() {
        return new Iterator<CharSequence>() {
            private int index = 0;
            private final int size = size();

            @Override
            public boolean hasNext() {
                return index < size;
            }

            @Override
            public CharSequence next() {
                return get(index++);
            }
        };
    }

    /**
     * 遍历分割后的字符序列
     *
     * @param action 消费者
     */
    @Override
    default void forEach(Consumer<? super CharSequence> action) {
        for (int i = 0, size = size(); i < size; i++) {
            action.accept(get(i));
        }
    }

    /**
     * 将字符序列转换为字符串数组
     *
     * @return 字符串数组
     */
    default String[] asStringArray() {
        String[] arr = new String[size()];
        for (int i = 0; i < arr.length; i++) {
            CharSequence c = get(i);
            arr[i] = c == null ? null : get(i).toString();
        }
        return arr;
    }

    @Nonnull
    @Override
    String toString();

}
