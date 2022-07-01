package org.jetlinks.core;


import java.util.concurrent.atomic.AtomicLongFieldUpdater;

public abstract class VisitCount {

    private static final AtomicLongFieldUpdater<VisitCount> LAST_ACCESS_TIME =
            AtomicLongFieldUpdater.newUpdater(VisitCount.class, "lastVisitTime");

    private volatile long lastVisitTime = System.currentTimeMillis();
    //  private volatile long numberOfVisits;

    protected void visit() {
        LAST_ACCESS_TIME.set(this, System.currentTimeMillis());
        // NUMBER_OF_VISITS.incrementAndGet(this);
    }

    public boolean tooLongNoVisit(long timeout) {
        return System.currentTimeMillis() - lastVisitTime >= timeout;
    }
}
