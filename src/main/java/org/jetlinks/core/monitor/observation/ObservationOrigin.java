package org.jetlinks.core.monitor.observation;

/**
 * 派生记录保留的原业务发生身份。用于识别来源，不代表投递成功、业务恢复或访问授权。
 * 实现只暴露原始类型/ID；不得根据当前对象状态补造历史来源。
 *
 * @since 1.3.3
 */
public interface ObservationOrigin {
    /** @return 原业务发生类型；没有可信来源时为空 */
    String getOriginType();

    /** @return 原业务单次发生 ID；不使用通知路由 ID 代替 */
    String getOriginId();
}
