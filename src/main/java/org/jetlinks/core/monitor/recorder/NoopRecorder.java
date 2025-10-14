package org.jetlinks.core.monitor.recorder;

import jakarta.annotation.Nonnull;

class NoopRecorder implements Recorder {

    static NoopRecorder INSTANCE = new NoopRecorder();

    @Override
    public <E> ActionRecorder<E> action(@Nonnull CharSequence action) {
        return ActionRecorder.noop();
    }

}
