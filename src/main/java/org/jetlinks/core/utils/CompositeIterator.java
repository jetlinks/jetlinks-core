package org.jetlinks.core.utils;

import java.util.Iterator;

class CompositeIterator<E> implements Iterator<E> {

    private final Iterator<E> first;
    private final Iterator<E> second;
    private Iterator<E> current;

    public CompositeIterator(Iterator<E> first, Iterator<E> second) {
        this.first = first;
        this.second = second;
        this.current = this.first;
    }


    @Override
    public boolean hasNext() {
        boolean hasNext = current.hasNext();
        if (!hasNext) {
            if (current == first) {
                current = second;
                hasNext = current.hasNext();
            }
        }
        return hasNext;
    }

    @Override
    public E next() {
        return current.next();
    }
}
