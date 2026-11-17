package org.jetlinks.core.utils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.function.BiPredicate;

/**
 * 因果信号处理算法。每个信号独立创建实例，由调用方串行调用；不创建线程或定时器，窗口始终有界。
 * 时间参数使用同一单调时间源，单位由调用方统一；数值平滑仅用于浮点测量，不用于精确累计量。
 *
 * @since 1.3.3
 */
public final class SignalFilters {
    private SignalFilters() {
    }

    /**
     * 突变确认：正常变化更新基准，超限段整体波动不超过阈值且达到次数后通过当前值。
     * 仅持有基准和候选段极值；调用方提供不可变数值及精确比较，避免大累计量窄化。
     */
    public static final class StepConfirmation<T> {
        private final int samples;
        private final long maxGap;
        private final Comparator<T> comparator;
        private final BiPredicate<T, T> withinRange;
        private T previous;
        private T min;
        private T max;
        private long lastTime;
        private int count;

        public StepConfirmation(int samples, long maxGap, Comparator<T> comparator, BiPredicate<T, T> withinRange) {
            if (samples < 2 || maxGap < 0 || comparator == null || withinRange == null) {
                throw new IllegalArgumentException("invalid step confirmation configuration");
            }
            this.samples = samples;
            this.maxGap = maxGap;
            this.comparator = comparator;
            this.withinRange = withinRange;
        }

        public boolean accept(T input, long time) {
            if (previous != null && time < lastTime) return false;
            boolean gap = previous == null || maxGap > 0 && time - lastTime >= maxGap;
            lastTime = time;
            if (gap || withinRange.test(input, previous)) {
                previous = input;
                min = max = null;
                count = 0;
                return true;
            }
            T low = count == 0 || comparator.compare(input, min) < 0 ? input : min;
            T high = count == 0 || comparator.compare(input, max) > 0 ? input : max;
            if (count == 0 || !withinRange.test(high, low)) {
                min = max = input;
                count = 1;
            } else {
                min = low;
                max = high;
                count++;
            }
            if (count < samples) return false;
            previous = input;
            min = max = null;
            count = 0;
            return true;
        }
    }

    /**
     * 来源时间积分，时间和输入速率的单位周期均为毫秒。固定标量、补偿求和；
     * 首值从零开始，重复/倒序不推进，长断流保留累计值但不补算缺失区间。
     * 状态由调用方拥有，不持久化，不适用于计费或需要重启续算的累计量。
     */
    public static final class Integral {
        private final long unitMillis;
        private final long maxGap;
        private final boolean trapezoid;
        private long lastTime;
        private double previous;
        private double total;
        private double correction;
        private boolean initialized;

        public Integral(long unitMillis, long maxGap, boolean trapezoid) {
            if (unitMillis <= 0 || maxGap < 0) throw new IllegalArgumentException("invalid integral configuration");
            this.unitMillis = unitMillis;
            this.maxGap = maxGap;
            this.trapezoid = trapezoid;
        }

        public double apply(double input, long time) {
            if (!Double.isFinite(input)) throw new IllegalArgumentException("integral requires a finite measurement");
            if (!initialized) {
                initialized = true;
                previous = input;
                lastTime = time;
                return 0;
            }
            long elapsed = time - lastTime;
            if (elapsed <= 0) return total;
            if (maxGap == 0 || elapsed < maxGap) {
                double area = (trapezoid ? previous / 2 + input / 2 : previous) * ((double) elapsed / unitMillis);
                double adjusted = area - correction;
                double next = total + adjusted;
                if (!Double.isFinite(next)) throw new ArithmeticException("integral overflow");
                correction = (next - total) - adjusted;
                total = next;
            }
            previous = input;
            lastTime = time;
            return total;
        }
    }

    /**
     * 因果Hampel检测，最多31个原始样本；满窗前通过，不用未来样本。
     * MAD用正态一致性系数1.4826缩放，绝对容差避免恒值信号的零MAD误报。
     * 被标记的原始值也推进窗口，因此持续真实阶跃会建立新基准，而非永久锁死。
     */
    public static final class Hampel {
        private final SampleWindow window;
        private final double sigma;
        private final double tolerance;
        private double median;

        public Hampel(int size, double sigma, double tolerance) {
            if (size < 3 || size > 31 || (size & 1) == 0 || !Double.isFinite(sigma) || sigma <= 0
                || !Double.isFinite(tolerance) || tolerance < 0) {
                throw new IllegalArgumentException("invalid Hampel configuration");
            }
            window = new SampleWindow(size);
            this.sigma = sigma;
            this.tolerance = tolerance;
        }

        public void reset() { window.reset(); }

        public boolean accept(double input) {
            if (!Double.isFinite(input)) return false;
            window.add(input);
            median = window.median();
            if (window.count < window.samples.length) return true;
            // 用半幅距离避免两个有限极值相减溢出；阈值和偏差采用同一尺度。
            for (int i = 0; i < window.count; i++) {
                window.sorted[i] = Math.abs(window.samples[i] / 2 - median / 2);
            }
            Arrays.sort(window.sorted, 0, window.count);
            double mad = window.median();
            return Math.abs(input / 2 - median / 2) <= Math.max(tolerance / 2, sigma * 1.4826 * mad);
        }

        /** 最近一次检测的中值，可用于显式选择的异常修正，不覆盖原始报文。 */
        public double replacement() { return median; }
    }

    /** 双侧截尾均值，最多31个样本；预热按已有数量同比截尾，不补零。 */
    public static final class TrimmedMean {
        private final SampleWindow window;
        private final int trim;

        public TrimmedMean(int size, int trim) {
            if (size < 3 || size > 31 || trim < 1 || trim > (size - 1) / 2) {
                throw new IllegalArgumentException("invalid trimmed mean configuration");
            }
            window = new SampleWindow(size);
            this.trim = trim;
        }

        public void reset() { window.reset(); }

        public double apply(double input) {
            if (!Double.isFinite(input)) return input;
            window.add(input);
            int cut = (int) ((long) trim * window.count / window.samples.length);
            double mean = window.sorted[cut];
            for (int i = cut + 1; i < window.count - cut; i++) {
                int n = i - cut + 1;
                double difference = window.sorted[i] - mean;
                mean = Double.isFinite(difference) ? mean + difference / n
                    : mean * ((double) (n - 1) / n) + window.sorted[i] / n;
            }
            return mean;
        }
    }

    /**
     * 限制测量值每秒升降幅度，固定标量状态。time和resetAfter用源时间毫秒，
     * 重复或倒序时间不推进状态；断流后重新以当前值初始化，不跨缺失区间修正。
     */
    public static final class SlewRate {
        private final double rise;
        private final double fall;
        private final long resetAfter;
        private long lastTime;
        private double value;
        private boolean initialized;

        public SlewRate(double rise, double fall, long resetAfter) {
            if (!Double.isFinite(rise) || !Double.isFinite(fall) || rise < 0 || fall < 0 || resetAfter < 0) {
                throw new IllegalArgumentException("invalid slew rate configuration");
            }
            this.rise = rise;
            this.fall = fall;
            this.resetAfter = resetAfter;
        }

        public double apply(double input, long time) {
            if (!Double.isFinite(input)) return input;
            if (!initialized) {
                initialized = true;
                lastTime = time;
                return value = input;
            }
            long elapsed = time - lastTime;
            if (elapsed <= 0) return value;
            lastTime = time;
            if (resetAfter > 0 && elapsed >= resetAfter) return value = input;
            double seconds = elapsed / 1000D;
            return value = Math.max(value - fall * seconds, Math.min(value + rise * seconds, input));
        }
    }

    /** 仅稳健窗口算法共用的固定工作区，不装箱、不在逐样本路径分配数组。 */
    private static final class SampleWindow {
        private final double[] samples;
        private final double[] sorted;
        private int count;
        private int index;

        private SampleWindow(int size) { samples = new double[size]; sorted = new double[size]; }
        private void reset() { count = index = 0; }
        private void add(double input) {
            samples[index] = input;
            index = (index + 1) % samples.length;
            count = Math.min(count + 1, samples.length);
            System.arraycopy(samples, 0, sorted, 0, count);
            Arrays.sort(sorted, 0, count);
        }
        private double median() {
            return (count & 1) == 1 ? sorted[count / 2] : sorted[count / 2 - 1] / 2 + sorted[count / 2] / 2;
        }
    }

    /** 一阶低通；首样本和长断流后以当前值初始化，固定数量标量状态。 */
    public static final class Exponential {
        private final double timeConstant;
        private final long resetAfter;
        private long lastTime;
        private double value;
        private boolean initialized;

        public Exponential(double timeConstant, long resetAfter) {
            if (!Double.isFinite(timeConstant) || timeConstant <= 0 || resetAfter < 0) {
                throw new IllegalArgumentException("invalid signal filter duration");
            }
            this.timeConstant = timeConstant;
            this.resetAfter = resetAfter;
        }

        public double apply(double input, long time) {
            if (!Double.isFinite(input)) return input;
            if (!initialized) {
                initialized = true;
                lastTime = time;
                return value = input;
            }
            long elapsed = time - lastTime;
            if (elapsed <= 0) return value;
            lastTime = time;
            if (resetAfter > 0 && elapsed >= resetAfter) return value = input;
            double alpha = -Math.expm1(-elapsed / timeConstant);
            // 分别加权，避免两个有限的极大异号数相减溢出。
            return value = (1 - alpha) * value + alpha * input;
        }
    }

    /** 最近3或5个有效样本的中值；预热使用已有样本，不补零，工作区循环复用。 */
    public static final class Median {
        private final double[] samples;
        private final double[] sorted;
        private int index;
        private int count;

        public Median(int size) {
            if (size != 3 && size != 5) throw new IllegalArgumentException("median window must be 3 or 5");
            samples = new double[size];
            sorted = new double[size];
        }

        /** 断流后重新预热，复用固定工作区；旧样本不参与新窗口。 */
        public void reset() {
            index = count = 0;
        }

        public double apply(double input) {
            if (!Double.isFinite(input)) return input;
            samples[index] = input;
            index = (index + 1) % samples.length;
            count = Math.min(count + 1, samples.length);
            System.arraycopy(samples, 0, sorted, 0, count);
            Arrays.sort(sorted, 0, count);
            // 偶数预热窗口取两中值的均值，避免大数求和溢出。
            return (count & 1) == 1 ? sorted[count / 2] : sorted[count / 2 - 1] / 2 + sorted[count / 2] / 2;
        }
    }

    /**
     * 固定窗口移动平均；环形数组最多64项，预热不补零，摊销O(1)。
     * 缩放后补偿求和，每轮重建一次，避免长期累积舍入误差及大值移出后的消减误差。
     */
    public static final class MovingAverage {
        private final double[] samples;
        private int count;
        private int index;
        private double sum;
        private double correction;

        public MovingAverage(int size) {
            if (size < 2 || size > 64) throw new IllegalArgumentException("average window must be 2 to 64");
            samples = new double[size];
        }

        public void reset() {
            count = index = 0;
            sum = correction = 0;
        }

        public double apply(double input) {
            if (!Double.isFinite(input)) return input;
            if (count < samples.length) count++;
            else add(-samples[index] / samples.length);
            add(input / samples.length);
            samples[index] = input;
            index = (index + 1) % samples.length;
            if (index == 0 || !Double.isFinite(sum + correction)) {
                sum = correction = 0;
                for (int i = 0; i < count; i++) add(samples[i] / samples.length);
            }
            double mean = (sum + correction) / ((double) count / samples.length);
            if (Double.isFinite(mean)) return mean;
            // 极限数值的中间舍入仍可能溢出，有限输入的均值采用分别加权恢复。
            mean = samples[0];
            for (int i = 1; i < count; i++) {
                double difference = samples[i] - mean;
                mean = Double.isFinite(difference) ? mean + difference / (i + 1)
                    : mean * ((double) i / (i + 1)) + samples[i] / (i + 1);
            }
            sum = mean * ((double) count / samples.length);
            correction = 0;
            return mean;
        }

        private void add(double value) {
            double next = sum + value;
            correction += Math.abs(sum) >= Math.abs(value) ? (sum - next) + value : (value - next) + sum;
            sum = next;
        }
    }

    /**
     * 样本驱动的状态确认。首值直接通过，新候选满足时间和次数才通过；无后续样本不会自行输出。
     * 比较和不可变快照由调用者提供，支持数字编码与枚举，不把非零值视为true。
     */
    public static final class Confirmation<T> {
        private final long duration;
        private final int samples;
        private final long maxGap;
        private final BiPredicate<T, T> equivalent;
        private T stable;
        private T candidate;
        private long since;
        private long lastTime;
        private int count;
        private boolean initialized;
        private boolean pending;

        public Confirmation(long duration, int samples, long maxGap, BiPredicate<T, T> equivalent) {
            if (duration < 0 || samples < 1 || maxGap < 0 || equivalent == null) {
                throw new IllegalArgumentException("invalid confirmation configuration");
            }
            this.duration = duration;
            this.samples = samples;
            this.maxGap = maxGap;
            this.equivalent = equivalent;
        }

        public boolean accept(T input, long time) {
            if (!initialized) {
                initialized = true;
                stable = input;
                lastTime = time;
                return true;
            }
            if (time < lastTime) return false;
            boolean gap = maxGap > 0 && time - lastTime >= maxGap;
            lastTime = time;
            if (equivalent.test(input, stable)) {
                pending = false;
                candidate = null;
                return true;
            }
            if (!pending || gap || !equivalent.test(input, candidate)) {
                candidate = input;
                since = time;
                count = 1;
                pending = true;
            } else if (count < samples) count++;
            if (count < samples || time - since < duration) return false;
            stable = input;
            candidate = null;
            pending = false;
            return true;
        }
    }

    /**
     * 连续确认段的稳定检测，调用方定义大小和允许波动。超限从当前样本重新确认，固定状态。
     * 这是保守的连续段确认，不是保存全部样本的精确滑动时间窗口；输出始终使用当前样本。
     */
    public static final class Stability<T> {
        private final long duration;
        private final int samples;
        private final long maxGap;
        private final Comparator<T> comparator;
        private final BiPredicate<T, T> withinRange;
        private T min;
        private T max;
        private long since;
        private long lastTime;
        private int count;

        public Stability(long duration, int samples, long maxGap, Comparator<T> comparator,
                         BiPredicate<T, T> withinRange) {
            if (duration < 0 || samples < 1 || maxGap < 0 || comparator == null || withinRange == null) {
                throw new IllegalArgumentException("invalid stability configuration");
            }
            this.duration = duration;
            this.samples = samples;
            this.maxGap = maxGap;
            this.comparator = comparator;
            this.withinRange = withinRange;
        }

        public boolean accept(T input, long time) {
            if (count != 0 && time < lastTime) return false;
            boolean restart = count == 0 || maxGap > 0 && time - lastTime >= maxGap;
            lastTime = time;
            if (!restart) {
                T low = comparator.compare(input, min) < 0 ? input : min;
                T high = comparator.compare(input, max) > 0 ? input : max;
                restart = !withinRange.test(high, low);
                if (!restart) { min = low; max = high; }
            }
            if (restart) {
                min = max = input;
                since = time;
                count = 1;
            } else if (count < samples) count++;
            return count >= samples && time - since >= duration;
        }
    }

    /** 样本驱动的布尔防抖；只有持续收到相同候选值超过稳定时间才准入，不合成新样本。 */
    public static final class Debounce {
        private final long onDelay;
        private final long offDelay;
        private boolean initialized;
        private boolean stable;
        private boolean candidate;
        private long since;

        public Debounce(long onDelay, long offDelay) {
            if (onDelay < 0 || offDelay < 0) throw new IllegalArgumentException("invalid debounce duration");
            this.onDelay = onDelay;
            this.offDelay = offDelay;
        }

        public boolean accept(boolean input, long time) {
            if (!initialized) {
                initialized = true;
                stable = candidate = input;
                since = time;
                return true;
            }
            if (input == stable) {
                candidate = input;
                since = time;
                return true;
            }
            if (input != candidate) {
                candidate = input;
                since = time;
            }
            long delay = input ? onDelay : offDelay;
            if (time - since < delay) return false;
            stable = input;
            return true;
        }
    }

    /** 双阈值状态转换：达到上限开启，达到下限关闭，中间保留状态；首次处于中间时为关闭。 */
    public static final class Hysteresis {
        private final double lower;
        private final double upper;
        private boolean active;

        public Hysteresis(double lower, double upper) {
            if (!Double.isFinite(lower) || !Double.isFinite(upper) || lower >= upper) {
                throw new IllegalArgumentException("invalid hysteresis bounds");
            }
            this.lower = lower;
            this.upper = upper;
        }

        public boolean apply(double input) {
            if (input >= upper) active = true;
            else if (input <= lower) active = false;
            return active;
        }
    }
}
