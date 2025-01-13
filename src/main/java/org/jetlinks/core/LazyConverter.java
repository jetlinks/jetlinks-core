package org.jetlinks.core;

import lombok.RequiredArgsConstructor;
import org.jetlinks.core.utils.SerializeUtils;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.function.Function;
import java.util.function.Supplier;

@RequiredArgsConstructor(staticName = "of")
public class LazyConverter<V, T> implements Supplier<T>, Externalizable {

    private transient final V source;
    private transient final Function<V, T> converter;

    private T value;
    private volatile boolean resolved;


    public LazyConverter() {
        this.source = null;
        this.converter = null;
    }

    @Override
    public T get() {
        if (resolved || source == null) {
            return value;
        }

        this.value = converter.apply(source);
        this.resolved = true;

        return value;
    }


    @Override
    public String toString() {
        return String.valueOf(get());
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        SerializeUtils.writeObject(get(), out);
    }

    @Override
    @SuppressWarnings("all")
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        value = (T) SerializeUtils.readObject(in);
        resolved = true;
    }
}
