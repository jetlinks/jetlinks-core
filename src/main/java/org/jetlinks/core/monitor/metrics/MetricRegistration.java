package org.jetlinks.core.monitor.metrics;

import reactor.core.Disposable;

/**
 * 一次对象观测注册的生命周期句柄；调用方随运行实例关闭它。
 * 支持实现必须绑定原注册及其代次，不能在关闭时重新查找并删除替换后的实例。
 *
 * @since 1.3.3
 */
public interface MetricRegistration extends Disposable {

    /** @return 注册是否受实现支持；false 表示 unsupported，不能解释为已采样的零值 */
    boolean isSupported();

    /** @return 无目标引用、已关闭的共享 unsupported 句柄 */
    static MetricRegistration unsupported() {
        return Unsupported.INSTANCE;
    }

    /** 不持有 target、契约、scope 或读取器的缺省句柄。 */
    final class Unsupported implements MetricRegistration {
        private static final Unsupported INSTANCE = new Unsupported();

        private Unsupported() {
        }

        @Override
        public boolean isSupported() {
            return false;
        }

        @Override
        public boolean isDisposed() {
            return true;
        }

        @Override
        public void dispose() {
        }
    }
}
