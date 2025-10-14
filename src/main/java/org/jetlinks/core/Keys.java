package org.jetlinks.core;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetlinks.core.utils.CompositeList;

import java.util.List;

class Keys {



    static class ListKey<T> extends BaseKey<List<T>>{

        public ListKey(String key) {
            super(key);
        }

        @Override
        public List<T> apply(List<T> ts, List<T> t2) {
            return new CompositeList<>(ts,t2);
        }
    }

    @AllArgsConstructor
    @Getter
    @EqualsAndHashCode(of = "key")
    static class BaseKey<T> implements Key<T>{
        private final String key;

        @Override
        public T apply(T t, T t2) {
            return t2;
        }
    }

}
