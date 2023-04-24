package org.jetlinks.core;

import lombok.RequiredArgsConstructor;
import org.jetlinks.core.utils.SerializeUtils;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.function.Supplier;

@RequiredArgsConstructor(staticName = "of")
public class Lazy<T> implements Supplier<T>, Externalizable {

    private transient final Supplier<? extends T> supplier;

    private T value;
    private volatile boolean resolved;


    public Lazy() {
        this.supplier = null;
    }

    @Override
    public T get() {

        if (resolved || supplier == null) {
            return value;
        }

        this.value = supplier.get();
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
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        value = (T) SerializeUtils.readObject(in);
        resolved = true;
    }
}
