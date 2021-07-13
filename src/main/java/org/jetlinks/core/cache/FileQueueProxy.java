package org.jetlinks.core.cache;

import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Iterator;

@AllArgsConstructor
public class FileQueueProxy<T> implements FileQueue<T> {

    protected final FileQueue<T> target;

    @Override
    public void close() {
        target.close();
    }

    @Override
    public void flush() {
        target.flush();
    }

    @Override
    public int size() {
        return target.size();
    }

    @Override
    public boolean isEmpty() {
        return target.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return target.contains(o);
    }

    @Nonnull
    @Override
    public Iterator<T> iterator() {
        return target.iterator();
    }

    @Override
    @Nonnull
    public Object[] toArray() {
        return target.toArray();
    }

    @Override
    @Nonnull
    public <T1> T1[] toArray(@Nonnull T1[] a) {
        return target.toArray(a);
    }

    @Override
    public boolean add(T t) {
        return target.add(t);
    }

    @Override
    public boolean remove(Object o) {
        return target.remove(o);
    }

    @Override
    public boolean containsAll(@Nonnull Collection<?> c) {
        return target.containsAll(c);
    }

    @Override
    public boolean addAll(@Nonnull Collection<? extends T> c) {
        return target.addAll(c);
    }

    @Override
    public boolean removeAll(@Nonnull Collection<?> c) {
        return target.removeAll(c);
    }

    @Override
    public boolean retainAll(@Nonnull Collection<?> c) {
        return target.retainAll(c);
    }

    @Override
    public void clear() {
        target.clear();
    }

    @Override
    public boolean offer(T t) {
        return target.offer(t);
    }

    @Override
    public T remove() {
        return target.remove();
    }

    @Override
    public T poll() {
        return target.poll();
    }

    @Override
    public T element() {
        return target.element();
    }

    @Override
    public T peek() {
        return target.peek();
    }
}
