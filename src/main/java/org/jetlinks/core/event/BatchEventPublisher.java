package org.jetlinks.core.event;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Exceptions;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Coordinates one batch Publisher until every topic branch has either accepted
 * a subscriber or completed without one.
 *
 * <p>This is deliberately package-private and is only the compatibility
 * implementation behind {@link EventBus#publish(java.util.Collection, Publisher)}.
 * Native EventBus implementations may use a cheaper candidate snapshot. The
 * coordinator is single-use per batch, but the returned batch Mono remains
 * repeatable because each subscription creates a new coordinator.</p>
 */
final class BatchEventPublisher<T> {

    private final Publisher<T> source;
    private final byte[] branchStates;
    private final List<Registration<T>> registrations;
    private final Object monitor = new Object();

    private int readyBranches;
    private int pendingOnSubscribeCallbacks;
    private boolean activated;
    private boolean cancelled;
    private volatile reactor.core.Disposable connection;

    BatchEventPublisher(Publisher<T> source, int branchCount) {
        this.source = Objects.requireNonNull(source, "source cannot be null");
        this.branchStates = new byte[branchCount];
        this.registrations = new ArrayList<>();
    }

    Publisher<T> branch(int index) {
        if (index < 0 || index >= branchStates.length) {
            throw new IndexOutOfBoundsException("index: " + index);
        }
        return new BranchPublisher<>(this, index);
    }

    void markNoSubscriber(int index) {
        boolean activate;
        synchronized (monitor) {
            if (branchStates[index] != 0) {
                return;
            }
            branchStates[index] = 2;
            activate = canActivate(++readyBranches);
        }
        if (activate) {
            activate();
        }
    }

    void cancelBranch(int index) {
        synchronized (monitor) {
            // ACTIVE branches were counted when onSubscribe was accepted. A later
            // cancellation only removes the branch from the eventual fan-out and
            // must not increment readyBranches a second time.
            if (branchStates[index] != 1) {
                return;
            }
            branchStates[index] = 2;
        }
    }

    void completeBranch(int index) {
        synchronized (monitor) {
            if (branchStates[index] != 1) {
                return;
            }
            branchStates[index] = 2;
        }
    }

    void subscribe(int index, Subscriber<? super T> actual) {
        Objects.requireNonNull(actual, "actual subscriber cannot be null");

        Registration<T> registration = new Registration<>(actual, this, index);
        boolean activate;
        boolean duplicate;
        try {
            synchronized (monitor) {
                duplicate = branchStates[index] != 0;
                if (duplicate) {
                    activate = false;
                } else {
                    branchStates[index] = 1;
                    registrations.add(registration);
                    pendingOnSubscribeCallbacks++;
                    // The registration defers upstream demand until all branches are ready.
                    // This keeps onSubscribe synchronous without allowing the first topic
                    // to start a synchronous source before the remaining topics are joined.
                    activate = ++readyBranches == branchStates.length;
                }
            }
        } catch (Throwable error) {
            Exceptions.throwIfFatal(error);
            throw Exceptions.propagate(error);
        }
        if (duplicate) {
            Operators.error(
                actual,
                new IllegalStateException("batch event branch subscribed more than once")
            );
            return;
        }
        Throwable failure = null;
        try {
            actual.onSubscribe(registration);
        } catch (Throwable error) {
            Exceptions.throwIfFatal(error);
            failure = error;
            registration.cancel();
        } finally {
            synchronized (monitor) {
                pendingOnSubscribeCallbacks--;
                activate = canActivate(readyBranches);
            }
        }
        if (activate) {
            activate();
        }
        if (failure != null) {
            // The branch is already cancelled above. Report the callback failure
            // without throwing into the remaining topic subscriptions, otherwise
            // a candidate Mono can remain pending.
            Operators.error(actual, failure);
            return;
        }
    }

    void cancel() {
        List<Registration<T>> current;
        reactor.core.Disposable currentConnection;
        synchronized (monitor) {
            if (cancelled) {
                return;
            }
            cancelled = true;
            current = new ArrayList<>(registrations);
            currentConnection = connection;
        }
        for (Registration<T> registration : current) {
            registration.cancel();
        }
        if (currentConnection != null) {
            currentConnection.dispose();
        }
    }

    private void activate() {
        List<Registration<T>> current;
        synchronized (monitor) {
            if (!canActivate(readyBranches)) {
                return;
            }
            activated = true;
            if (registrations.isEmpty()) {
                return;
            }
            current = new ArrayList<>(registrations);
        }

        current.removeIf(Registration::isCancelled);
        if (current.isEmpty()) {
            return;
        }

        ConnectableFlux<T> multicast = Flux.from(source).publish();
        try {
            for (Registration<T> registration : current) {
                multicast.subscribe(registration.forwardingSubscriber());
            }
            reactor.core.Disposable connected = multicast.connect();
            synchronized (monitor) {
                connection = connected;
                if (cancelled) {
                    connected.dispose();
                }
            }
        } catch (Throwable error) {
            Exceptions.throwIfFatal(error);
            for (Registration<T> registration : current) {
                registration.fail(error);
            }
        }
    }

    private boolean canActivate(int ready) {
        return !cancelled && !activated && ready == branchStates.length && pendingOnSubscribeCallbacks == 0;
    }

    private static final class BranchPublisher<T> implements Publisher<T> {

        private final BatchEventPublisher<T> parent;
        private final int index;

        private BranchPublisher(BatchEventPublisher<T> parent, int index) {
            this.parent = parent;
            this.index = index;
        }

        @Override
        public void subscribe(Subscriber<? super T> actual) {
            parent.subscribe(index, actual);
        }
    }

    private static final class Registration<T> implements Subscription {

        private final Subscriber<? super T> actual;
        private final BatchEventPublisher<T> parent;
        private final int index;
        private final Object monitor = new Object();

        private Subscription upstream;
        private long requested;
        private boolean cancelled;

        private Registration(Subscriber<? super T> actual,
                            BatchEventPublisher<T> parent,
                            int index) {
            this.actual = actual;
            this.parent = parent;
            this.index = index;
        }

        @Override
        public void request(long n) {
            if (n <= 0) {
                cancel();
                actual.onError(new IllegalArgumentException("§3.9: non-positive request"));
                return;
            }
            Subscription current;
            synchronized (monitor) {
                if (cancelled) {
                    return;
                }
                if (upstream == null) {
                    requested = addCap(requested, n);
                    return;
                }
                current = upstream;
            }
            current.request(n);
        }

        @Override
        public void cancel() {
            Subscription current;
            synchronized (monitor) {
                if (cancelled) {
                    return;
                }
                cancelled = true;
                current = upstream;
            }
            if (current != null) {
                current.cancel();
            }
            parent.cancelBranch(index);
        }

        private void setUpstream(Subscription subscription) {
            long demand;
            boolean cancelUpstream;
            synchronized (monitor) {
                cancelUpstream = cancelled;
                if (cancelUpstream) {
                    demand = 0;
                } else {
                    upstream = subscription;
                    demand = requested;
                }
            }
            if (cancelUpstream) {
                subscription.cancel();
            } else if (demand != 0) {
                subscription.request(demand);
            }
        }

        private boolean isCancelled() {
            synchronized (monitor) {
                return cancelled;
            }
        }

        private void onNext(T value) {
            synchronized (monitor) {
                if (cancelled) {
                    return;
                }
            }
            actual.onNext(value);
        }

        private void onError(Throwable error) {
            synchronized (monitor) {
                if (cancelled) {
                    return;
                }
                cancelled = true;
            }
            try {
                actual.onError(error);
            } finally {
                parent.completeBranch(index);
            }
        }

        private void onComplete() {
            synchronized (monitor) {
                if (cancelled) {
                    return;
                }
                cancelled = true;
            }
            try {
                actual.onComplete();
            } finally {
                parent.completeBranch(index);
            }
        }

        private void fail(Throwable error) {
            onError(error);
        }

        private Subscriber<T> forwardingSubscriber() {
            return new ForwardingSubscriber<>(this, actual);
        }

        private static long addCap(long current, long n) {
            long next = current + n;
            return next < 0 ? Long.MAX_VALUE : next;
        }
    }

    private static final class ForwardingSubscriber<T> implements CoreSubscriber<T> {

        private final Registration<T> registration;
        private final Subscriber<? super T> actual;

        private ForwardingSubscriber(Registration<T> registration,
                                     Subscriber<? super T> actual) {
            this.registration = registration;
            this.actual = actual;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            registration.setUpstream(subscription);
        }

        @Override
        public void onNext(T value) {
            registration.onNext(value);
        }

        @Override
        public void onError(Throwable throwable) {
            registration.onError(throwable);
        }

        @Override
        public void onComplete() {
            registration.onComplete();
        }

        @Override
        public Context currentContext() {
            if (actual instanceof CoreSubscriber) {
                return ((CoreSubscriber<?>) actual).currentContext();
            }
            return Context.empty();
        }
    }
}
