package org.jetlinks.core.utils;

import com.google.common.collect.AbstractIterator;

import java.util.Iterator;
import java.util.Set;


public class CompositeSet<E> extends CompositeCollection<E> implements Set<E> {

    public CompositeSet(Set<E> first, Set<E> second) {
        super(first.size() >= second.size() ? first : second,
              second.size() > first.size() ? first : second);
    }

    @Override
    public int size() {
        int duplicate = 0;

        for (E e :second ) {
            if (first.contains(e)) {
                duplicate++;
            }
        }

        return super.size() - duplicate;
    }

    @Override
    public Iterator<E> iterator() {
        return new IteratorView();
    }

    class IteratorView extends AbstractIterator<E>{
        private final Iterator<E> firstIt = first.iterator();
        private final Iterator<E> secondIt = second.iterator();

        @Override
        protected E computeNext() {

            if (secondIt.hasNext()) {
               return secondIt.next();
            }

            while (firstIt.hasNext()) {
                E e = firstIt.next();
                if (!second.contains(e)) {
                    return e;
                }
            }
            return endOfData();
        }
    }
}
