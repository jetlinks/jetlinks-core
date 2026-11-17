package org.jetlinks.core.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class SignalFiltersTest {
    @Test
    public void medianKeepsSubnormalValuesDuringWarmup() {
        var filter = new SignalFilters.Median(3);
        assertEquals(Double.MIN_VALUE, filter.apply(Double.MIN_VALUE), 0);
        assertEquals(Double.MIN_VALUE, filter.apply(Double.MIN_VALUE), 0);
        assertEquals(Double.MIN_VALUE, filter.apply(2 * Double.MIN_VALUE), 0);
        filter.reset();
        assertEquals(-Double.MIN_VALUE, filter.apply(-Double.MIN_VALUE), 0);
        assertEquals(-Double.MIN_VALUE, filter.apply(-Double.MIN_VALUE), 0);
    }

    @Test
    public void inPlaceMedianDoesNotReorderTheAgeOfRawSamples() {
        var filter = new SignalFilters.Median(5);
        double[] input = {100, -10, 80, 0, 1, 2, 3, 4, 5, 6};
        double[] expected = {100, 45, 80, 40, 1, 1, 2, 2, 3, 4};
        for (int i = 0; i < input.length; i++) assertEquals("sample " + i, expected[i], filter.apply(input[i]), 0);
    }

    @Test
    public void causalMediansMatchAnIndependentSortedWindowAfterManyWrapsAndResets() {
        var random = new java.util.Random(903551);
        for (int size : new int[]{3, 5}) {
            var filter = new SignalFilters.Median(size);
            var history = new java.util.ArrayDeque<Double>();
            for (int i = 0; i < 5000; i++) {
                if (i % 191 == 0) { filter.reset(); history.clear(); }
                double value = random.nextInt(2048) / 8D;
                if (history.size() == size) history.removeFirst();
                history.addLast(value);
                double[] ordered = history.stream().mapToDouble(Double::doubleValue).sorted().toArray();
                int middle = ordered.length / 2;
                double expected = (ordered.length & 1) == 0
                    ? (ordered[middle - 1] + ordered[middle]) / 2 : ordered[middle];
                assertEquals(expected, filter.apply(value), 0);
            }
        }
    }

    @Test
    public void hampelMatchesIndependentCausalMedianAndMadAcrossAllWindowSizes() {
        var random = new java.util.Random(13452);
        for (int size : new int[]{3, 7, 15, 31}) {
            var filter = new SignalFilters.Hampel(size, 3, .1);
            var history = new java.util.ArrayDeque<Double>();
            for (int i = 0; i < 1000; i++) {
                if (i % 157 == 0) { filter.reset(); history.clear(); }
                double value = i % 29 == 0 ? 1000 : random.nextInt(9) / 4D;
                if (history.size() == size) history.removeFirst();
                history.addLast(value);
                double[] ordered = history.stream().mapToDouble(Double::doubleValue).sorted().toArray();
                int middle = ordered.length / 2;
                double median = (ordered.length & 1) == 0
                    ? (ordered[middle - 1] + ordered[middle]) / 2 : ordered[middle];
                double[] deviations = history.stream().mapToDouble(sample -> Math.abs(sample - median)).sorted().toArray();
                boolean expected = history.size() < size
                    || Math.abs(value - median) <= Math.max(.1, 3 * 1.4826 * deviations[middle]);
                assertEquals(expected, filter.accept(value));
                assertEquals(median, filter.replacement(), 0);
            }
        }
    }

    @Test
    public void libraryMeansUseOnlyTheActiveWindowAfterResetAndIgnoreInvalidInput() {
        var mean = new SignalFilters.MovingAverage(64);
        var trimmed = new SignalFilters.TrimmedMean(31, 3);
        for (int i = 0; i < 64; i++) {
            double average = mean.apply(Double.MAX_VALUE);
            double trimmedAverage = trimmed.apply(Double.MAX_VALUE);
            assertTrue(Double.isFinite(average));
            assertTrue(Double.isFinite(trimmedAverage));
            assertEquals(Double.MAX_VALUE, average, Math.ulp(Double.MAX_VALUE) * 2);
            assertEquals(Double.MAX_VALUE, trimmedAverage, Math.ulp(Double.MAX_VALUE) * 2);
        }
        mean.reset();
        trimmed.reset();
        assertTrue(Double.isNaN(mean.apply(Double.NaN)));
        assertTrue(Double.isInfinite(trimmed.apply(Double.POSITIVE_INFINITY)));
        assertEquals(1e-300, mean.apply(1e-300), 1e-314);
        assertEquals(1e-300, trimmed.apply(1e-300), 1e-314);
        assertEquals(1e-300, mean.apply(1e-300), 1e-314);
        assertEquals(1e-300, trimmed.apply(1e-300), 1e-314);
    }

    @Test
    public void windowMeansAgreeWithAnIndependentDecimalReferenceOverChangingMagnitudes() {
        var random = new java.util.Random(81473);
        for (int size : new int[]{2, 3, 31, 64}) {
            var filter = new SignalFilters.MovingAverage(size);
            var window = new java.util.ArrayDeque<java.math.BigDecimal>();
            for (int i = 0; i < 2000; i++) {
                if (i % 173 == 0) { filter.reset(); window.clear(); }
                double value = (random.nextDouble() - .5) * (i % 7 == 0 ? 1e12 : 100);
                if (window.size() == size) window.removeFirst();
                window.addLast(new java.math.BigDecimal(value));
                var sum = java.math.BigDecimal.ZERO;
                for (var sample : window) sum = sum.add(sample);
                double expected = sum.divide(java.math.BigDecimal.valueOf(window.size()), java.math.MathContext.DECIMAL128).doubleValue();
                double scale = 0;
                for (var sample : window) scale = Math.max(scale, sample.abs().doubleValue());
                assertEquals(expected, filter.apply(value), Math.max(1e-10, Math.ulp(scale) * 16));
            }
        }
    }

    @Test
    public void trimmedMeansMatchAnIndependentDecimalReferenceAcrossWindowShapes() {
        var random = new java.util.Random(672114);
        for (int size : new int[]{3, 4, 5, 16, 31}) {
            for (int trim : new int[]{1, (size - 1) / 2}) {
                var filter = new SignalFilters.TrimmedMean(size, trim);
                var history = new java.util.ArrayDeque<Double>();
                for (int i = 0; i < 900; i++) {
                    if (i % 137 == 0) { filter.reset(); history.clear(); }
                    if (i % 17 == 0) assertTrue(Double.isNaN(filter.apply(Double.NaN)));
                    double value = (random.nextDouble() - .5) * (i % 7 == 0 ? 1e9 : 100);
                    if (history.size() == size) history.removeFirst();
                    history.addLast(value);
                    double[] ordered = history.stream().mapToDouble(Double::doubleValue).sorted().toArray();
                    int cut = trim * ordered.length / size;
                    var sum = java.math.BigDecimal.ZERO;
                    for (int j = cut; j < ordered.length - cut; j++) {
                        sum = sum.add(new java.math.BigDecimal(ordered[j]));
                    }
                    double expected = sum.divide(java.math.BigDecimal.valueOf(ordered.length - 2L * cut),
                        java.math.MathContext.DECIMAL128).doubleValue();
                    assertEquals(expected, filter.apply(value), Math.max(1e-9, Math.ulp(expected) * 4));
                }
            }
        }
    }

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
    public void booleanConfirmationRequiresContinuousObservedStability() {
        var filter = new SignalFilters.Confirmation<Boolean>(100, 1, 0, Boolean::equals);
        assertTrue(filter.accept(false, 0));
        assertFalse(filter.accept(true, 1));
        assertTrue(filter.accept(false, 50));
        assertFalse(filter.accept(true, 60));
        assertFalse(filter.accept(true, 159));
        assertTrue(filter.accept(true, 160));
        assertFalse(filter.accept(false, 170));
        assertTrue(filter.accept(true, 190));
        assertFalse(filter.accept(false, 200));
        assertFalse(filter.accept(false, 299));
        assertTrue(filter.accept(false, 300));
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
