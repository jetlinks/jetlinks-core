package org.jetlinks.core.event;

import reactor.core.Disposable;

public interface Cancelable extends Disposable {

    /**
     * 当设置了{@link Subscription.Feature#persistent}时,
     * 使用此方法取消订阅,否则数据将一直保留在事件总线中.
     */
    default void cancel() {
        dispose();
    }

}
