package org.jetlinks.core.utils;

import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

@AllArgsConstructor
public class CompositeCollection<E> implements Collection<E>, Serializable {
    private static final long serialVersionUID = 1L;

    protected final Collection<E> first;
    protected final Collection<E> second;

    @Override
    public int size() {
        return first.size() + second.size();
    }

    @Override
    public boolean isEmpty() {
        return first.isEmpty() && second.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return first.contains(o) || second.contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return new CompositeIterator<>(first.iterator(), second.iterator());
    }

    @Override
    public Object[] toArray() {
        Object[] arr = new Object[size()];
        int idx = 0;

        for (E e : this) {
            arr[idx++] = e;
        }

        return arr;
    }

    @Override
    @SuppressWarnings("all")
    public <T> T[] toArray(T[] a) {
        int size = size();
        if (a.length < size) {
            a = Arrays.copyOf(a, size);
        }
        int idx = 0;

        for (E e : this) {
            a[idx++] = (T) e;
        }

        return a;
    }

    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(@Nonnull Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(@Nonnull Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(@Nonnull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(@Nonnull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "[" + first.toString() + "," + second.toString() + "]";
    }
}
