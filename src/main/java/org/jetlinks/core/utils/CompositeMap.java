package org.jetlinks.core.utils;

import com.google.common.collect.AbstractIterator;

import java.io.Serializable;
import java.util.*;

public class CompositeMap<K, V> implements Map<K, V>, Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<K, V> first;
    private final Map<K, V> second;

    public CompositeMap(Map<K, V> first, Map<K, V> second) {
        this.first = first.size() >= second.size() ? first : second;
        this.second = second.size() > first.size() ? first : second;
    }

    @Override
    public int size() {
        int duplicate = 0;

        for (K e : second.keySet()) {
            if (first.containsKey(e)) {
                duplicate++;
            }
        }

        return first.size() + second.size() - duplicate;
    }

    @Override
    public boolean isEmpty() {
        return first.isEmpty() && second.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return first.containsKey(key) || second.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return first.containsValue(value) || second.containsValue(value);
    }

    @Override
    public V get(Object key) {
        V firstValue = first.get(key);
        if (firstValue == null) {
            return second.get(key);
        }
        return firstValue;
    }

    @Override
    public V put(K key, V value) {
        throw new IllegalStateException();
    }

    @Override
    public V remove(Object key) {
        throw new IllegalStateException();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new IllegalStateException();
    }

    @Override
    public void clear() {

    }

    @Override
    public Set<K> keySet() {
        return new CompositeSet<>(first.keySet(), second.keySet());
    }

    @Override
    public Collection<V> values() {
        return new ValueView();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return new CompositeSet<>(first.entrySet(), second.entrySet());
    }

    @Override
    public String toString() {
        return "[" + first + "," + second + "]";
    }

    class ValueView extends AbstractCollection<V> {

        @Override
        public Iterator<V> iterator() {
            return new ValueIteratorView();
        }

        @Override
        public int size() {
            return CompositeMap.this.size();
        }
    }

    class ValueIteratorView extends AbstractIterator<V> {
        private final Iterator<Entry<K, V>> firstIt = first.entrySet().iterator();
        private final Iterator<Entry<K, V>> secondIt = second.entrySet().iterator();

        @Override
        protected V computeNext() {

            if (secondIt.hasNext()) {
                return secondIt.next().getValue();
            }

            while (firstIt.hasNext()) {
                Entry<K, V> e = firstIt.next();
                if (!second.containsKey(e.getKey())) {
                    return e.getValue();
                }
            }
            return endOfData();
        }
    }
}
