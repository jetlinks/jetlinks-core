package org.jetlinks.core.monitor.recorder;

import org.jetlinks.core.Key;
import org.reactivestreams.Publisher;
import reactor.util.context.ContextView;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

class NoopActionRecorder<E> implements ActionRecorder<E> {
    public static final NoopActionRecorder<Object> INSTANCE = new NoopActionRecorder<>();

    private NoopActionRecorder() {
    }

    @Override
    public ActionRecorder<E> tag(String tag, Object value) {
        return this;
    }

    @Override
    public <V> ActionRecorder<E> tag(Key<V> key, V value) {
        return this;
    }

    @Override
    public <V> ActionRecorder<E> tag(Key<V> key, Supplier<V> value) {
        return this;
    }

    @Override
    public ActionRecorder<E> tags(Map<String, Object> tags) {
        return this;
    }

    @Override
    public ActionRecorder<E> attributes(Map<String, Object> data) {
        return this;
    }

    @Override
    public ActionRecorder<E> attribute(String tag, Object value) {
        return this;
    }

    @Override
    public <V> ActionRecorder<E> attribute(Key<V> key, V value) {
        return this;
    }

    @Override
    public <V> ActionRecorder<E> attribute(Key<V> key, Supplier<V> value) {
        return this;
    }

    @Override
    public ActionRecorder<E> error(Throwable error) {
        return this;
    }

    @Override
    public ActionRecorder<E> cancel() {
        return this;
    }

    @Override
    public ActionRecorder<E> complete() {
        return this;
    }

    @Override
    public ActionRecorder<E> value(E value) {
        return this;
    }

    @Override
    public ActionRecorder<E> valueConverter(Function<E, Object> converter) {
        return this;
    }

    @Override
    public ActionRecorder<E> start(ContextView context) {
        return this;
    }

    @Override
    public <T> ActionRecorder<T> child(CharSequence action) {
        return ActionRecorder.noop();
    }

    @Override
    public Publisher<E> apply(Publisher<E> publisher) {
        return publisher;
    }
}
