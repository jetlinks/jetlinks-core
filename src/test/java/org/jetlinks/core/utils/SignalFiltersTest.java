package org.jetlinks.core.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class SignalFiltersTest {
    @Test
    public void confirmsRealStepsButDoesNotDelayNormalChanges() {
        var filter = new SignalFilters.StepConfirmation<Double>(3, 1000, Double::compare,
            (a, b) -> Math.abs(a - b) <= 2);
        assertTrue(filter.accept(10D, 0));
        assertTrue(filter.accept(11D, 1));
        assertFalse(filter.accept(80D, 2));
        assertTrue(filter.accept(12D, 3));
        assertFalse(filter.accept(40D, 4));
        assertFalse(filter.accept(41D, 5));
        assertTrue(filter.accept(40D, 6));
        assertTrue(filter.accept(42D, 7));
        assertFalse(filter.accept(100D, 8));
        assertFalse(filter.accept(110D, 9));
        assertFalse(filter.accept(100D, 10));
        assertFalse(filter.accept(100D, 9));
        assertTrue(filter.accept(200D, 1010));
    }

    @Test
    public void integratesIrregularSourceTimeWithoutFillingGapsOrPoisoningState() {
        var integral = new SignalFilters.Integral(60000, 5000, true);
        assertEquals(0D, integral.apply(60, 0), 0);
        assertEquals(1.5D, integral.apply(120, 1000), 0);
        assertEquals(5.5D, integral.apply(120, 3000), 0);
        assertEquals(5.5D, integral.apply(999, 3000), 0);
        assertEquals(5.5D, integral.apply(999, 2000), 0);
        assertEquals(7D, integral.apply(60, 4000), 0);
        assertEquals(7D, integral.apply(600, 10000), 0);
        assertEquals(17D, integral.apply(600, 11000), 0);
        assertThrows(IllegalArgumentException.class, () -> integral.apply(Double.NaN, 12000));
        var huge = new SignalFilters.Integral(1000, 0, false);
        huge.apply(Double.MAX_VALUE, 0);
        assertThrows(ArithmeticException.class, () -> huge.apply(0, 2000));
        assertEquals(Double.MAX_VALUE / 2, huge.apply(0, 500), 0);
    }

    @Test
    public void integralCompensationPreservesSmallIncrementsAndUsesInputUnit() {
        var integral = new SignalFilters.Integral(1000, 0, false);
        integral.apply(1e16, 0);
        integral.apply(1, 1000);
        for (int i = 2; i <= 1001; i++) integral.apply(1, i * 1000L);
        assertEquals(1e16 + 1000, integral.apply(1, 1001000), 0);
    }
    @Test
    public void hampelRejectsSpikesButTracksPersistentChanges() {
        var filter = new SignalFilters.Hampel(3, 3, 0);
        assertTrue(filter.accept(10));
        assertTrue(filter.accept(10));
        assertFalse(filter.accept(80));
        assertEquals(10, filter.replacement(), 0);
        assertTrue(filter.accept(12));
        assertTrue(filter.accept(12));
        assertFalse(filter.accept(20));
        assertTrue(filter.accept(20));
        assertFalse(filter.accept(Double.NaN));
        filter.reset();
        assertTrue(filter.accept(-100));
    }

    @Test
    public void hampelHandlesZeroMadToleranceAndFiniteExtremes() {
        var filter = new SignalFilters.Hampel(3, 3, .2);
        filter.accept(10); filter.accept(10);
        assertTrue(filter.accept(10.1));
        filter.reset();
        filter.accept(-Double.MAX_VALUE); filter.accept(-Double.MAX_VALUE);
        assertFalse(filter.accept(Double.MAX_VALUE));
        assertEquals(-Double.MAX_VALUE, filter.replacement(), 0);
        assertThrows(IllegalArgumentException.class, () -> new SignalFilters.Hampel(32, 3, 0));
        assertThrows(IllegalArgumentException.class, () -> new SignalFilters.Hampel(4, 3, 0));
        assertThrows(IllegalArgumentException.class, () -> new SignalFilters.Hampel(3, Double.NaN, 0));
    }

    @Test
    public void trimmedMeanWarmsUpWithoutPaddingAndRejectsBothTails() {
        var filter = new SignalFilters.TrimmedMean(5, 1);
        for (int i = 0; i < 4; i++) assertEquals(10, filter.apply(10), 1e-12);
        assertEquals(10, filter.apply(100), 1e-12);
        assertEquals(10, filter.apply(-100), 1e-12);
        assertEquals(32D / 3, filter.apply(12), 1e-12);
        filter.reset();
        for (int i = 0; i < 10; i++) assertTrue(Double.isFinite(filter.apply(i % 2 == 0 ? Double.MAX_VALUE : -Double.MAX_VALUE)));
        assertThrows(IllegalArgumentException.class, () -> new SignalFilters.TrimmedMean(3, Integer.MAX_VALUE));
    }

    @Test
    public void slewUsesSampleTimeAsymmetricBoundsAndRestartsAfterGap() {
        var filter = new SignalFilters.SlewRate(2, 1, 10000);
        assertEquals(10, filter.apply(10, 1000), 0);
        assertEquals(12, filter.apply(30, 2000), 0);
        assertEquals(12, filter.apply(30, 2000), 0);
        assertEquals(12, filter.apply(30, 1999), 0);
        assertEquals(11, filter.apply(0, 3000), 0);
        assertEquals(99, filter.apply(99, 13000), 0);
        assertTrue(Double.isNaN(filter.apply(Double.NaN, 14000)));
        assertEquals(100, filter.apply(999, 13500), 0);
        assertThrows(IllegalArgumentException.class, () -> new SignalFilters.SlewRate(-1, 1, 0));
    }

    @Test
    public void movingAverageWarmsUpRecoversAfterLargeValuesAndResets() {
        var filter = new SignalFilters.MovingAverage(3);
        assertEquals(10, filter.apply(10), 1e-12);
        assertEquals(15, filter.apply(20), 1e-12);
        assertEquals(20, filter.apply(30), 1e-12);
        assertEquals(30, filter.apply(40), 1e-12);
        assertTrue(Double.isNaN(filter.apply(Double.NaN)));
        assertEquals(40, filter.apply(50), 1e-12);
        filter.reset();
        assertEquals(-10, filter.apply(-10), 1e-12);
        var extremes = new SignalFilters.MovingAverage(2);
        assertTrue(Double.isFinite(extremes.apply(Double.MAX_VALUE)));
        assertTrue(Double.isFinite(extremes.apply(-Double.MAX_VALUE)));
        extremes.apply(2);
        assertEquals(3, extremes.apply(4), 1e-12);
        assertThrows(IllegalArgumentException.class, () -> new SignalFilters.MovingAverage(65));
    }

    @Test
    public void movingAverageDoesNotAccumulateDriftAcrossMillionsOfUpdates() {
        var filter = new SignalFilters.MovingAverage(32);
        for (int i = 0; i < 1000000; i++) filter.apply(20 + (i % 32) / 10D);
        assertEquals(21.55, filter.apply(20), 1e-10);
    }

    @Test
    public void confirmationPreservesNumericStatesAndRestartsOnGaps() {
        var filter = new SignalFilters.Confirmation<Integer>(10, 2, 100, Integer::equals);
        assertTrue(filter.accept(0, 0));
        assertFalse(filter.accept(2, 1));
        assertFalse(filter.accept(2, 5));
        assertTrue(filter.accept(2, 11));
        assertFalse(filter.accept(1, 12));
        assertTrue(filter.accept(2, 15));
        assertFalse(filter.accept(1, 20));
        assertFalse(filter.accept(1, 120));
        assertFalse(filter.accept(1, 119));
        assertTrue(filter.accept(1, 130));
        var counts = new SignalFilters.Confirmation<String>(0, 3, 0, String::equals);
        assertTrue(counts.accept("stop", 0));
        assertFalse(counts.accept("run", 1));
        assertFalse(counts.accept("run", 2));
        assertTrue(counts.accept("run", 3));
    }

    @Test
    public void stabilityChecksTheWholeConfirmationSegmentInsteadOfOnlyAdjacentChanges() {
        var filter = new SignalFilters.Stability<Double>(100, 1, 1000, Double::compare,
            (max, min) -> max - min <= 0.25);
        assertFalse(filter.accept(10D, 0));
        assertFalse(filter.accept(10.1, 50));
        assertTrue(filter.accept(10.2, 100));
        assertFalse(filter.accept(10.3, 150));
        assertFalse(filter.accept(10.3, 140));
        assertTrue(filter.accept(10.4, 250));
        assertFalse(filter.accept(10.4, 1250));
        assertTrue(filter.accept(10.4, 1350));
    }

    @Test
    public void exponentialUsesElapsedTimeAndDoesNotLearnInvalidOrOldValues() {
        var filter = new SignalFilters.Exponential(1000, 5000);
        assertEquals(20, filter.apply(20, 0), 0);
        assertEquals(20.9516258, filter.apply(30, 100), 0.000001);
        assertEquals(26.3212056, filter.apply(30, 1000), 0.000001);
        assertTrue(Double.isNaN(filter.apply(Double.NaN, 2000)));
        assertEquals(26.3212056, filter.apply(100, 500), 0.000001);
        assertEquals(40, filter.apply(40, 6000), 0);
    }

    @Test
    public void exponentialAvoidsOverflowBetweenFiniteInputs() {
        var filter = new SignalFilters.Exponential(1, 0);
        filter.apply(Double.MAX_VALUE, 0);
        assertTrue(Double.isFinite(filter.apply(-Double.MAX_VALUE, 1)));
    }

    @Test
    public void medianIsCausalAndHasNoZeroPadding() {
        var filter = new SignalFilters.Median(3);
        assertEquals(10, filter.apply(10), 0);
        assertEquals(10.5, filter.apply(11), 0);
        assertEquals(11, filter.apply(1000), 0);
        assertEquals(12, filter.apply(12), 0);
        assertEquals(13, filter.apply(13), 0);
        assertTrue(Double.isNaN(filter.apply(Double.NaN)));
        assertEquals(13, filter.apply(14), 0);
    }

    @Test
    public void medianResetStartsANewWindowWithoutOldSamples() {
        var filter = new SignalFilters.Median(5);
        for (int i = 0; i < 5; i++) filter.apply(100 + i);
        filter.reset();
        assertEquals(10, filter.apply(10), 0);
        assertEquals(15, filter.apply(20), 0);
        assertEquals(20, filter.apply(30), 0);
        filter.reset();
        filter.reset();
        assertEquals(-10, filter.apply(-10), 0);
    }

    @Test
    public void debounceRequiresContinuousObservedStability() {
        var filter = new SignalFilters.Debounce(100, 200);
        assertTrue(filter.accept(false, 0));
        assertFalse(filter.accept(true, 1));
        assertTrue(filter.accept(false, 50));
        assertFalse(filter.accept(true, 60));
        assertFalse(filter.accept(true, 159));
        assertTrue(filter.accept(true, 160));
        assertFalse(filter.accept(false, 170));
        assertFalse(filter.accept(false, 369));
        assertTrue(filter.accept(false, 370));
    }

    @Test
    public void hysteresisKeepsStateBetweenThresholds() {
        var filter = new SignalFilters.Hysteresis(10, 20);
        assertFalse(filter.apply(15));
        assertTrue(filter.apply(20));
        assertTrue(filter.apply(15));
        assertFalse(filter.apply(10));
        assertFalse(filter.apply(15));
    }
}
