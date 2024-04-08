package org.jetlinks.core;

/**
 * 包装器接口,实现该接口,表示该对象可能被包装。
 * <p>可通过{@link #isWrapperFor(Class)}判断是否被包装。</p>
 * <p>在需要进行类型转换时,使用{@link #unwrap(Class)}进行转换。</p>
 * <pre>{@code
 *
 *   if(session.isWrapperFor(ChildDeviceSession.class)){
 *       session.unwrap(ChildDeviceSession.class).getParentDevice();
 *   }
 *
 * }</pre>
 * <p>
 * 使用场景: 当提供的某个接口存在多种变种且可能会被代理、拦截时,可通过实现此接口对外提供兼容的类型转换方式.
 *
 * @author zhouhao
 * @since 1.2.0
 */
public interface Wrapper {

    /**
     * 当前对象是否为指定的类型或者被包装为指定的类型
     *
     * @param type 类型
     * @return 是否为指定的类型
     */
    default boolean isWrapperFor(Class<?> type) {
        return type.isInstance(this);
    }

    /**
     * 尝试将当前对象转换为指定的类型,如果无法转换,将抛出{@link ClassCastException}
     *
     * @param type 类型
     * @param <T>  类型
     * @return 转换后的对象
     */
    default <T> T unwrap(Class<T> type) {
        return type.cast(this);
    }

}
