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
                    LargeEntry remaining = large.firstEntry();
                    state = new SingleState(remaining.key, remaining.timestamp);
                } else if (large.size < SMALL_CAPACITY) {
                    SmallState small = new SmallState(large);
                    state = small;
                    if (small.contains(key)) {
                        return false;
                    }
                    small.add(key, now);
                    return true;
                } else {
                    boolean added = large.addIfAbsent(key, now);
                    if (!added && large.size == SMALL_CAPACITY) {
                        // A duplicate at the boundary can safely compact to SmallState. A new key
                        // stays in LargeState and avoids rebuilding the 8/9-key boundary each time.
                        state = new SmallState(large);
                    }
                    return added;
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
        private static final int MIN_TABLE_CAPACITY = 16;

        private LargeEntry[] table;
        private int resizeThreshold;
        private int size;
        private LargeEntry firstExpiry;
        private LargeEntry lastExpiry;

        private LargeState(SmallState small, Object key, long timestamp) {
            table = new LargeEntry[MIN_TABLE_CAPACITY];
            resizeThreshold = (int) (table.length * LOAD_FACTOR);
            for (int i = 0; i < small.size; i++) {
                addKnownAbsent(small.keys[i], small.timestamps[i]);
            }
            addKnownAbsent(key, timestamp);
        }

        private boolean addIfAbsent(Object key, long timestamp) {
            shrinkIfSparse();
            int hash = spreadHash(key);
            int bucket = bucket(hash, table.length);
            LargeEntry entry = table[bucket];
            if (entry == null) {
                if (size + 1 > resizeThreshold) {
                    resize();
                    addKnownAbsent(key, timestamp, hash);
                } else {
                    linkEntry(bucket, null, new LargeEntry(key, timestamp));
                }
                return true;
            }

            LargeEntry tail;
            do {
                if (key == entry.key || key.equals(entry.key)) {
                    return false;
                }
                tail = entry;
                entry = entry.nextInBucket;
            } while (entry != null);

            if (size + 1 > resizeThreshold) {
                resize();
                addKnownAbsent(key, timestamp, hash);
                return true;
            }

            linkEntry(bucket, tail, new LargeEntry(key, timestamp));
            return true;
        }

        private void addKnownAbsent(Object key, long timestamp) {
            addKnownAbsent(key, timestamp, spreadHash(key));
        }

        private void addKnownAbsent(Object key, long timestamp, int hash) {
            if (size + 1 > resizeThreshold) {
                resize();
            }
            int bucket = bucket(hash, table.length);
            LargeEntry tail = table[bucket];
            if (tail != null) {
                while (tail.nextInBucket != null) {
                    tail = tail.nextInBucket;
                }
            }
            linkEntry(bucket, tail, new LargeEntry(key, timestamp));
        }

        private void linkEntry(int bucket, LargeEntry bucketTail, LargeEntry entry) {
            if (bucketTail == null) {
                table[bucket] = entry;
            } else {
                bucketTail.nextInBucket = entry;
            }
            if (lastExpiry == null) {
                firstExpiry = entry;
            } else {
                lastExpiry.nextExpiry = entry;
            }
            lastExpiry = entry;
            size++;
        }

        private LargeEntry firstEntry() {
            return firstExpiry;
        }

        private void drainExpired(long now, long durationNanos) {
            LargeEntry first = firstExpiry;
            if (!DurationStore.isExpired(now, first.timestamp, durationNanos)) {
                return;
            }

            drainExpiredEntries(now, durationNanos, first);
        }

        private void drainExpiredEntries(long now, long durationNanos, LargeEntry first) {
            // DurationStore retains LargeState between calls only when at least nine entries remain.
            LargeEntry second = first.nextExpiry;
            if (DurationStore.isExpired(now, second.timestamp, durationNanos)) {
                drainExpiredBatch(now, durationNanos);
                return;
            }

            firstExpiry = second;
            first.nextExpiry = null;
            removeFromTable(first);
            size--;
        }

        private void drainExpiredBatch(long now, long durationNanos) {
            if (DurationStore.isExpired(now, lastExpiry.timestamp, durationNanos)) {
                // Only probe the newest entry after detecting a multi-entry expiry batch. This keeps
                // one-at-a-time steady churn on the same path while retaining O(1) full-window reset.
                clear();
                return;
            }

            while (firstExpiry != null &&
                DurationStore.isExpired(now, firstExpiry.timestamp, durationNanos)) {
                LargeEntry expired = firstExpiry;
                firstExpiry = expired.nextExpiry;
                expired.nextExpiry = null;
                removeFromTable(expired);
                size--;
            }
        }

        private void shrinkIfSparse() {
            int capacity = table.length;
            if (size > (capacity >>> 2)) {
                return;
            }
            int targetCapacity = capacity;
            // Shrink below 25% load. Halving leaves retained entries between 25% and 50% full.
            while (targetCapacity > MIN_TABLE_CAPACITY && size <= (targetCapacity >>> 2)) {
                targetCapacity >>>= 1;
            }
            if (targetCapacity != capacity) {
                rebuild(targetCapacity);
            }
        }

        private void rebuild(int capacity) {
            LargeEntry[] rebuilt = new LargeEntry[capacity];
            LargeEntry[] tails = new LargeEntry[capacity];
            LargeEntry entry = firstExpiry;
            while (entry != null) {
                LargeEntry nextExpiry = entry.nextExpiry;
                entry.nextInBucket = null;
                int bucket = bucket(spreadHash(entry.key), capacity);
                LargeEntry tail = tails[bucket];
                if (tail == null) {
                    rebuilt[bucket] = entry;
                } else {
                    tail.nextInBucket = entry;
                }
                tails[bucket] = entry;
                entry = nextExpiry;
            }
            table = rebuilt;
            resizeThreshold = (int) (capacity * LOAD_FACTOR);
        }

        private void clear() {
            table = null;
            firstExpiry = null;
            lastExpiry = null;
            size = 0;
        }

        private void removeFromTable(LargeEntry removed) {
            int bucket = bucket(spreadHash(removed.key), table.length);
            LargeEntry entry = table[bucket];
            // Bucket links follow insertion order, so expiry order normally removes the head.
            if (entry == removed) {
                table[bucket] = entry.nextInBucket;
                entry.nextInBucket = null;
                return;
            }

            // Keep a defensive slow path for an unexpected ordering violation.
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
            LargeEntry[] previous = table;
            LargeEntry[] expanded = new LargeEntry[previous.length << 1];
            // Low/high splitting preserves insertion order inside every bucket, keeping the
            // bucket head aligned with the global expiry queue after a resize.
            for (int oldBucket = 0; oldBucket < previous.length; oldBucket++) {
                LargeEntry lowHead = null;
                LargeEntry lowTail = null;
                LargeEntry highHead = null;
                LargeEntry highTail = null;
                LargeEntry entry = previous[oldBucket];
                while (entry != null) {
                    LargeEntry next = entry.nextInBucket;
                    entry.nextInBucket = null;
                    if ((spreadHash(entry.key) & previous.length) == 0) {
                        if (lowTail == null) {
                            lowHead = entry;
                        } else {
                            lowTail.nextInBucket = entry;
                        }
                        lowTail = entry;
                    } else {
                        if (highTail == null) {
                            highHead = entry;
                        } else {
                            highTail.nextInBucket = entry;
                        }
                        highTail = entry;
                    }
                    entry = next;
                }
                expanded[oldBucket] = lowHead;
                expanded[oldBucket + previous.length] = highHead;
            }
            table = expanded;
            resizeThreshold = (int) (expanded.length * LOAD_FACTOR);
        }

        private static int spreadHash(Object key) {
            int hash = key.hashCode();
            return hash ^ (hash >>> 16);
        }

        private static int bucket(int hash, int length) {
            return hash & (length - 1);
        }
    }

    static final class LargeEntry {
        private final Object key;
        private final long timestamp;
        private LargeEntry nextInBucket;
        private LargeEntry nextExpiry;

        private LargeEntry(Object key, long timestamp) {
            this.key = key;
            this.timestamp = timestamp;
        }
    }
}
