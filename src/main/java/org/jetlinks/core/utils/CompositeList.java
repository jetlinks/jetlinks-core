package org.jetlinks.core.utils;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

public class CompositeList<E> extends CompositeCollection<E> implements List<E> {

    public CompositeList(List<E> first, List<E> second) {
        super(first, second);
    }

    List<E> first() {
        return ((List<E>) first);
    }

    List<E> second() {
        return ((List<E>) second);
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public E get(int index) {
        List<E> first = first();
        int firstSize = first.size();
        if (index < firstSize) {
            return first.get(index);
        }
        return second().get(index - firstSize);
    }

    @Override
    public E set(int index, E element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(int index, E element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public E remove(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int indexOf(Object o) {
        int fistIndex = first().indexOf(o);
        if (fistIndex > 0) {
            return fistIndex;
        }
        int secondIndex = second().indexOf(o);
        if (secondIndex > 0) {
            return secondIndex + first.size();
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        int fistIndex = first().lastIndexOf(o);
        if (fistIndex > 0) {
            return fistIndex + second.size();
        }
        int secondIndex = second().lastIndexOf(o);
        if (secondIndex > 0) {
            return secondIndex + first.size();
        }
        return -1;
    }

    @Override
    public ListIterator<E> listIterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        int size = size();
        if (index >= size) {
            throw new IndexOutOfBoundsException("index:" + index + ",size:" + size);
        }
        return new CompositeListIterator(index);
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        if (fromIndex < first().size()) {
            if (toIndex < first().size()) {
                return first().subList(fromIndex, toIndex);
            }
            return new CompositeList<>(first().subList(fromIndex, first().size()),
                                       second().subList(0, toIndex - first().size()));
        }
        return second().subList(fromIndex - first().size(), toIndex - first().size());
    }


    class CompositeListIterator implements ListIterator<E> {

        private int index;

        CompositeListIterator(int index) {
            this.index = index;
        }

        @Override
        public boolean hasNext() {
            return size() > index;
        }

        @Override
        public E next() {
            return CompositeList.this.get(index++);
        }

        @Override
        public boolean hasPrevious() {
            return index > 0;
        }

        @Override
        public E previous() {
            return get(--index);
        }

        @Override
        public int nextIndex() {
            return index;
        }

        @Override
        public int previousIndex() {
            return index - 1;
        }

        @Override
        public void remove() {
            CompositeList.this.remove(index);
        }

        @Override
        public void set(E e) {
            CompositeList.this.set(index, e);
        }

        @Override
        public void add(E e) {
            CompositeList.this.add(index, e);
        }
    }
}
