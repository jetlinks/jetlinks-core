package org.jetlinks.core.things;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public abstract class AbstractThingsDataManager implements ThingsDataManager {
    private final Map<String, ThingPropertyRef> localCache = newCache();

    static <K, V> Map<K, V> newCache() {
        return Caffeine
                .newBuilder()
                //10分钟没有访问则过期
                .expireAfterAccess(Duration.ofMinutes(10))
                .removalListener((key, value, removalCause) -> {
                    if (value instanceof Disposable) {
                        ((Disposable) value).dispose();
                    }
                })
                .<K, V>build()
                .asMap();
    }

    @Override
    public final Mono<ThingProperty> getLastProperty(String thingId,
                                                     String property,
                                                     long baseTime) {
        return localCache
                .computeIfAbsent(thingId, ThingPropertyRef::new)
                .getLastProperty(property, baseTime);
    }

    @Override
    public final Mono<ThingProperty> getFirstProperty(String thingId,
                                                      String property) {
        return localCache
                .computeIfAbsent(thingId, ThingPropertyRef::new)
                .getFirstProperty(property);
    }

    @Override
    public Mono<Long> getLastPropertyTime(String thingId, long baseTime) {
        return localCache
                .computeIfAbsent(thingId, ThingPropertyRef::new)
                .getLastPropertyTime(baseTime);
    }

    protected abstract Mono<ThingProperty> loadAnyLastProperty(String thingId, long baseTime);

    protected abstract Mono<ThingProperty> loadLastProperty(String thingId, String property, long baseTime);

    protected abstract Mono<ThingProperty> loadFirstProperty(String thingId, String property);

    protected abstract Flux<ThingProperty> subscribeProperty(String thingId);

    private static final Object NULL = new Object();

    static class PropertyRef implements ThingProperty {

        @Getter
        private final String property;

        @Getter
        private volatile Object value;

        @Getter
        private volatile String state;

        @Getter
        private volatile long timestamp;

        //上一个
        private transient PropertyRef pre;
        //第一个
        private transient PropertyRef first;

        public PropertyRef(String property) {
            this.property = property;
        }

        PropertyRef setValue(Object value, long ts, String state) {
            //只处理比较新的数据
            if (this.value == null || this.value == NULL || ts >= this.timestamp) {
                if (pre == null) {
                    pre = new PropertyRef(property);
                }
                pre.value = this.value;
                pre.timestamp = this.timestamp;
                pre.value = this.state;
                this.value = value;
                this.timestamp = ts;
                this.state = state;
            }
            return this;
        }

        void setNull() {
            if (value == null) {
                value = NULL;
            }
        }

        PropertyRef setFirst(Object value, long ts) {
            if (first == null) {
                first = new PropertyRef(property);
            }
            if (first.value == null || first.value == NULL || ts <= first.timestamp) {
                first.value = value;
                first.timestamp = ts;
            }
            return first;
        }

        PropertyRef setFirstNull() {
            if (first == null) {
                first = new PropertyRef(property);
                first.value = NULL;
            }
            return first;
        }

        ThingProperty copy() {
            return ThingProperty.of(property, value, timestamp);
        }
    }

    class ThingPropertyRef implements Disposable {
        Disposable disposable;
        Map<String, PropertyRef> refs = new ConcurrentHashMap<>();
        String thingId;
        private long lastPropertyTime;
        private long propertyTime;

        public ThingPropertyRef(String thingId) {
            this.thingId = thingId;
            disposable = subscribeProperty(thingId)
                    .subscribe(this::upgrade);
        }

        private void upgrade(ThingProperty property) {

            PropertyRef ref = refs.get(property.getProperty());
            //只更新有人读取过的属性,节省内存
            if (null != ref) {
                ref.setValue(property.getValue(), property.getTimestamp(), property.getState());
            }
            updatePropertyTime(property.getTimestamp());
        }

        private long updatePropertyTime(long timestamp) {
            if (propertyTime <= timestamp) {
                this.lastPropertyTime = propertyTime;
                this.propertyTime = timestamp;
            }
            return propertyTime;
        }

        public Mono<ThingProperty> getFirstProperty(String property) {
            PropertyRef ref = refs.computeIfAbsent(property, ignore -> new PropertyRef(property));
            if (ref.first != null && ref.first.getValue() != null) {
                if (ref.first.getValue() == NULL) {
                    return Mono.empty();
                }
                return Mono.just(ref.first);
            }
            return AbstractThingsDataManager.this
                    .loadFirstProperty(thingId, property)
                    .<ThingProperty>map(prop -> ref.setFirst(prop.getValue(), prop.getTimestamp()))
                    .switchIfEmpty(Mono.fromRunnable(ref::setFirstNull))
                    ;
        }

        public Mono<Long> getLastPropertyTime(long baseTime) {
            if (propertyTime == -1) {
                return Mono.empty();
            }
            if (propertyTime > 0 && propertyTime < baseTime) {
                return Mono.just(propertyTime);
            }
            if (lastPropertyTime > 0 && lastPropertyTime < baseTime) {
                return Mono.just(lastPropertyTime);
            }
            //查询最新属性
            return loadAnyLastProperty(thingId, baseTime)
                    .map(val -> {
                        if (propertyTime <= 0) {
                            updatePropertyTime(val.getTimestamp());
                        }
                        return val.getTimestamp();
                    })
                    .switchIfEmpty(Mono.fromRunnable(() -> {
                        if (this.propertyTime == 0) {
                            propertyTime = -1;
                        }
                    }));
        }

        public Mono<ThingProperty> getLastProperty(String key, long baseTime) {
            PropertyRef ref = refs.computeIfAbsent(key, PropertyRef::new);
            Object val = ref.getValue();
            if (val == NULL) {
                return Mono.empty();
            }
            Function<Mono<ThingProperty>, Mono<ThingProperty>> resultHandler;
            if (val != null) {
                //本地缓存
                if (ref.timestamp < baseTime) {
                    return Mono.just(ref.copy());
                }
                if (ref.pre != null && ref.pre.timestamp < baseTime && ref.pre.value != NULL) {
                    return Mono.just(ref.pre.copy());
                }
                //获取当前数据之前的数据
                resultHandler = prop -> prop
                        .map(propertyValue -> new PropertyRef(key)
                                .setValue(
                                        propertyValue.getValue(),
                                        propertyValue.getTimestamp(),
                                        propertyValue.getState()
                                ));
            } else {
                //当前没有任何数据,则进行查询
                resultHandler = prop -> prop
                        .<ThingProperty>map(propertyValue -> ref.setValue(
                                propertyValue.getValue(),
                                propertyValue.getTimestamp(),
                                propertyValue.getState())
                        )
                        .switchIfEmpty(Mono.fromRunnable(ref::setNull));
            }

            return loadLastProperty(thingId, key, baseTime)
                    .as(resultHandler);
        }

        @Override
        public void dispose() {
            disposable.dispose();
        }
    }
}
