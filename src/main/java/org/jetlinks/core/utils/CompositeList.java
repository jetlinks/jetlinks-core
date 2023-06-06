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
        private List<E> current;

        CompositeListIterator(int index) {
            this.index = index;
        }

        private void init() {
            this.current = this.index < first.size()
                    ? first()
                    : second();
        }

        @Override
        public boolean hasNext() {
            return current == null ? index < size() : index < current.size();
        }

        @Override
        public E next() {
            if (current == null) {
                init();
            }
            if (current == first) {
                if (index >= first.size()) {
                    current = second();
                    index -= first.size();
                }
            }
            return current.get(index++);
        }

        @Override
        public boolean hasPrevious() {
            if (current == second) {
                return true;
            }
            if (current == first) {
                return index >= 0;
            }
            return index > 0;
        }

        @Override
        public E previous() {
            if (current == null) {
                init();
            }
            if (current == second) {
                if (index < 0) {
                    current = first();
                    index += first.size();
                }
                if (index == current.size()) {
                    index--;
                }
            }
            return current.get(index--);
        }

        @Override
        public int nextIndex() {
            if (current == second) {
                return index + first.size();
            }
            return index;
        }

        @Override
        public int previousIndex() {
            if (current == second) {
                if (index == 0) {
                    return first.size();
                }
                if (index == second.size()) {
                    return index + first.size() - 1;
                }
                return index + first.size();
            }
            return index;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(E e) {
            throw new UnsupportedOperationException();
        }
    }
}
