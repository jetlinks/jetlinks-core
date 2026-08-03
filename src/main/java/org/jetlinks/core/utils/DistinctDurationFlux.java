package org.jetlinks.core.utils;

import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxOperator;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * 按固定时间窗口去重。
 *
 * @param <T> 泛型
 * @see FluxUtils#distinct(Function, Duration)
 */
public class DistinctDurationFlux<T> extends FluxOperator<T, T> {

    private static final Object NULL_KEY = new Object();

    private final Flux<? extends T> delegate;

    protected DistinctDurationFlux(Flux<? extends T> source,
                                   Function<T, ?> keySelector,
                                   Duration duration) {
        this(source, keySelector, duration, System::nanoTime);
    }

    DistinctDurationFlux(Flux<? extends T> source,
                         Function<T, ?> keySelector,
                         Duration duration,
                         LongSupplier ticker) {
        super(source);
        Objects.requireNonNull(keySelector, "keySelector");
        Objects.requireNonNull(ticker, "ticker");

        DurationDistinct<T> distinct = new DurationDistinct<>(
            keySelector,
            toNanos(duration),
            ticker);
        this.delegate = source.distinct(
            distinct,
            distinct,
            distinct,
            distinct);
    }

    public static <T> Flux<T> create(Flux<? extends T> source,
                                     Function<T, ?> keySelector,
                                     Duration duration) {
        return new DistinctDurationFlux<>(source, keySelector, duration);
    }

    static <T> Flux<T> create(Flux<? extends T> source,
                              Function<T, ?> keySelector,
                              Duration duration,
                              LongSupplier ticker) {
        return new DistinctDurationFlux<>(source, keySelector, duration, ticker);
    }

    private static long toNanos(Duration duration) {
        Objects.requireNonNull(duration, "duration");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("duration must be greater than zero");
        }
        try {
            return duration.toNanos();
        } catch (ArithmeticException ignore) {
            // A positive Duration beyond the nanoTime range behaves as a practically infinite window.
            return Long.MAX_VALUE;
        }
    }

    @Override
    public void subscribe(@Nonnull CoreSubscriber<? super T> actual) {
        delegate.subscribe(actual);
    }

    /**
     * One immutable configuration object is shared by all subscriptions to avoid retaining several
     * capturing lambdas in every Reactor distinct subscriber.
     */
    static final class DurationDistinct<T>
        implements Function<T, Object>,
        Supplier<DurationStore>,
        BiPredicate<DurationStore, Object>,
        Consumer<DurationStore> {

        private final Function<T, ?> keySelector;
        private final long durationNanos;
        private final LongSupplier ticker;

        private DurationDistinct(Function<T, ?> keySelector,
                                 long durationNanos,
                                 LongSupplier ticker) {
            this.keySelector = keySelector;
            this.durationNanos = durationNanos;
            this.ticker = ticker;
        }

        @Override
        public Object apply(T value) {
            Object key = keySelector.apply(value);
            return key == null ? NULL_KEY : key;
        }

        @Override
        public DurationStore get() {
            return new DurationStore();
        }

        @Override
        public boolean test(DurationStore store, Object key) {
            return key == NULL_KEY || store.add(key, durationNanos, ticker);
        }

        @Override
        public void accept(DurationStore store) {
            store.clear();
        }
    }

    /**
     * Per-subscription state. Reactive Streams serializes signals for a Subscriber, so this store
     * deliberately uses non-concurrent collections. Duplicate hits do not renew the fixed window.
     */
    static final class DurationStore {

        private static final int SMALL_CAPACITY = 8;

        private Object state;

        boolean add(Object key, long durationNanos, LongSupplier ticker) {
            long now = ticker.getAsLong();
            if (state instanceof SmallState) {
                SmallState small = (SmallState) state;
                small.drainExpired(now, durationNanos);
                if (small.size == 0) {
                    state = null;
                } else if (small.size == 1) {
                    state = new SingleState(small.keys[0], small.timestamps[0]);
                } else {
                    if (small.contains(key)) {
                        return false;
                    }
                    if (small.size < SMALL_CAPACITY) {
                        small.add(key, now);
                        return true;
                    }
                    state = new LargeState(small, key, now);
                    return true;
                }
            } else if (state instanceof LargeState) {
                LargeState large = (LargeState) state;
                large.drainExpired(now, durationNanos);
                if (large.size == 0) {
                    state = null;
                } else if (large.size == 1) {
                    Object remainingKey = large.firstKey();
                    state = new SingleState(remainingKey, large.timestamp(remainingKey));
                } else if (large.size <= SMALL_CAPACITY) {
                    SmallState small = new SmallState(large);
                    state = small;
                    if (small.contains(key)) {
                        return false;
                    }
                    if (small.size < SMALL_CAPACITY) {
                        small.add(key, now);
                        return true;
                    }
                    state = new LargeState(small, key, now);
                    return true;
                } else {
                    if (large.contains(key)) {
                        return false;
                    }
                    large.add(key, now);
                    return true;
                }
            }

            if (state == null) {
                state = new SingleState(key, now);
                return true;
            }
            SingleState single = (SingleState) state;
            if (isExpired(now, single.timestamp, durationNanos)) {
                single.key = key;
                single.timestamp = now;
                return true;
            }
            if (key.equals(single.key)) {
                return false;
            }

            state = new SmallState(single.key, single.timestamp, key, now);
            return true;
        }

        private static boolean isExpired(long now, long timestamp, long durationNanos) {
            return now - timestamp >= durationNanos;
        }

        int size() {
            if (state instanceof SmallState) {
                return ((SmallState) state).size;
            }
            if (state instanceof LargeState) {
                return ((LargeState) state).size;
            }
            return state == null ? 0 : 1;
        }

        void clear() {
            if (state instanceof SmallState) {
                ((SmallState) state).clear();
            } else if (state instanceof LargeState) {
                ((LargeState) state).clear();
            }
            state = null;
        }
    }

    static final class SingleState {
        private Object key;
        private long timestamp;

        private SingleState(Object key, long timestamp) {
            this.key = key;
            this.timestamp = timestamp;
        }
    }

    static final class SmallState {

        private final Object[] keys = new Object[DurationStore.SMALL_CAPACITY];
        private final long[] timestamps = new long[DurationStore.SMALL_CAPACITY];
        private int size;

        private SmallState(Object firstKey,
                           long firstTimestamp,
                           Object secondKey,
                           long secondTimestamp) {
            add(firstKey, firstTimestamp);
            add(secondKey, secondTimestamp);
        }

        private SmallState(LargeState large) {
            LargeEntry entry = large.firstExpiry;
            while (entry != null) {
                add(entry.key, entry.timestamp);
                entry = entry.nextExpiry;
            }
        }

        private boolean contains(Object key) {
            for (int i = 0; i < size; i++) {
                if (key.equals(keys[i])) {
                    return true;
                }
            }
            return false;
        }

        private void add(Object key, long timestamp) {
            keys[size] = key;
            timestamps[size] = timestamp;
            size++;
        }

        private void drainExpired(long now, long durationNanos) {
            int writeIndex = 0;
            for (int readIndex = 0; readIndex < size; readIndex++) {
                if (!DurationStore.isExpired(now, timestamps[readIndex], durationNanos)) {
                    if (writeIndex != readIndex) {
                        keys[writeIndex] = keys[readIndex];
                        timestamps[writeIndex] = timestamps[readIndex];
                    }
                    writeIndex++;
                }
            }
            for (int index = writeIndex; index < size; index++) {
                keys[index] = null;
                timestamps[index] = 0;
            }
            size = writeIndex;
        }

        private void clear() {
            for (int i = 0; i < size; i++) {
                keys[i] = null;
                timestamps[i] = 0;
            }
            size = 0;
        }
    }

    static final class LargeState {

        private static final float LOAD_FACTOR = 0.75F;

        private LargeEntry[] table;
        private int resizeThreshold;
        private int size;
        private LargeEntry firstExpiry;
        private LargeEntry lastExpiry;

        private LargeState(SmallState small, Object key, long timestamp) {
            table = new LargeEntry[16];
            resizeThreshold = (int) (table.length * LOAD_FACTOR);
            for (int i = 0; i < small.size; i++) {
                add(small.keys[i], small.timestamps[i]);
            }
            add(key, timestamp);
        }

        private void add(Object key, long timestamp) {
            if (size + 1 > resizeThreshold) {
                resize();
            }
            int bucket = bucket(key, table.length);
            LargeEntry entry = new LargeEntry(key, timestamp, table[bucket]);
            table[bucket] = entry;
            if (lastExpiry == null) {
                firstExpiry = entry;
            } else {
                lastExpiry.nextExpiry = entry;
            }
            lastExpiry = entry;
            size++;
        }

        private boolean contains(Object key) {
            return find(key) != null;
        }

        private long timestamp(Object key) {
            LargeEntry entry = find(key);
            if (entry == null) {
                throw new IllegalStateException("missing duration distinct key");
            }
            return entry.timestamp;
        }

        private Object firstKey() {
            return firstExpiry.key;
        }

        private void drainExpired(long now, long durationNanos) {
            while (firstExpiry != null &&
                DurationStore.isExpired(now, firstExpiry.timestamp, durationNanos)) {
                LargeEntry expired = firstExpiry;
                firstExpiry = expired.nextExpiry;
                expired.nextExpiry = null;
                removeFromTable(expired);
                size--;
            }
            if (firstExpiry == null) {
                lastExpiry = null;
            }
        }

        private void clear() {
            table = null;
            firstExpiry = null;
            lastExpiry = null;
            size = 0;
        }

        private LargeEntry find(Object key) {
            LargeEntry entry = table[bucket(key, table.length)];
            while (entry != null) {
                if (key.equals(entry.key)) {
                    return entry;
                }
                entry = entry.nextInBucket;
            }
            return null;
        }

        private void removeFromTable(LargeEntry removed) {
            int bucket = bucket(removed.key, table.length);
            LargeEntry entry = table[bucket];
            LargeEntry previous = null;
            while (entry != null) {
                if (entry == removed) {
                    if (previous == null) {
                        table[bucket] = entry.nextInBucket;
                    } else {
                        previous.nextInBucket = entry.nextInBucket;
                    }
                    entry.nextInBucket = null;
                    return;
                }
                previous = entry;
                entry = entry.nextInBucket;
            }
        }

        private void resize() {
            LargeEntry[] expanded = new LargeEntry[table.length << 1];
            LargeEntry entry = firstExpiry;
            while (entry != null) {
                int bucket = bucket(entry.key, expanded.length);
                entry.nextInBucket = expanded[bucket];
                expanded[bucket] = entry;
                entry = entry.nextExpiry;
            }
            table = expanded;
            resizeThreshold = (int) (expanded.length * LOAD_FACTOR);
        }

        private static int bucket(Object key, int length) {
            int hash = key.hashCode();
            return (hash ^ (hash >>> 16)) & (length - 1);
        }
    }

    static final class LargeEntry {
        private final Object key;
        private final long timestamp;
        private LargeEntry nextInBucket;
        private LargeEntry nextExpiry;

        private LargeEntry(Object key, long timestamp, LargeEntry nextInBucket) {
            this.key = key;
            this.timestamp = timestamp;
            this.nextInBucket = nextInBucket;
        }
    }
}
