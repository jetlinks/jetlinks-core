package org.jetlinks.core.monitor.metrics;

import com.alibaba.fastjson.JSON;
import org.jetlinks.core.annotation.metric.Counter;
import org.jetlinks.core.annotation.metric.Gauge;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.UnitSupported;
import org.jetlinks.core.metadata.types.ArrayType;
import org.jetlinks.core.utils.MetadataUtils;
import org.junit.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.ResolvableType;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class BuiltinMetricsMetadataTest {
    private static final List<String> LANGUAGES = List.of("en", "zh", "zh_Hant", "ja", "ko", "de", "fr", "es");

    @Test
    public void languageBundlesCoverEveryCoreKeyAndPreserveMessageArguments() throws Exception {
        Properties english = messages("en");
        for (String language : LANGUAGES) {
            Properties translated = messages(language);
            assertEquals(language, english.stringPropertyNames(), translated.stringPropertyNames());
            Locale locale = Locale.forLanguageTag(language.replace('_', '-'));
            for (String key : english.stringPropertyNames()) {
                String value = translated.getProperty(key);
                assertFalse(language + ":" + key, value.isBlank());
                assertEquals(language + ":" + key, arguments(english.getProperty(key)), arguments(value));
                assertFalse(language + ":" + key, value.contains("\ufffd"));
                new MessageFormat(value, locale);
            }
        }
    }

    @Test
    public void traditionalChineseLocalesUseTheHantBundleThroughStandardLookup() {
        ResourceBundle.Control control = ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES);
        for (String tag : List.of("zh-Hant", "zh-TW", "zh-HK", "zh-Hant-TW", "zh-Hant-HK")) {
            ResourceBundle bundle = ResourceBundle.getBundle("i18n/jetlinks-core/messages", Locale.forLanguageTag(tag), control);
            assertEquals(tag, Locale.forLanguageTag("zh-Hant"), bundle.getLocale());
            assertEquals("累計量", bundle.getString("org.jetlinks.core.monitor.metrics.RateMetrics.count.title"));
            assertEquals("陣列", bundle.getString("message.metadata.type.array"));
        }
    }

    @Test
    public void primitiveAndBoxedDoublesShareNumericMetadataIncludingArrayElements() {
        assertEquals("double", MetadataUtils.parseType(ResolvableType.forClass(double.class)).getId());
        assertEquals("double", MetadataUtils.parseType(ResolvableType.forClass(Double.class)).getId());
        ArrayType array = (ArrayType) MetadataUtils.parseType(ResolvableType.forClass(double[].class));
        assertEquals("double", array.getElementType().getId());
    }

    @Test
    public void rateDefinitionsDistinguishLifetimeMeanAndEwmaWithoutAssumingBusinessUnits() throws Throwable {
        MetricSet set = MetricSet.of(RateMeter.class);
        assertSame(set, MetricSet.of(RateMeter.class));
        assertEquals(5, set.getMetrics().size());
        assertEquals(5, MetricSet.of(RateMetrics.class).getMetrics().size());
        long[] now = {0};
        RateMeter meter = new RateMeter(() -> now[0]);
        meter.mark(10);
        assertEquals(10L, metric(set, "count").read(meter));
        assertEquals(MetricKind.counter, metric(set, "count").getKind());
        assertEquals("mean", semantics(set, "meanRate").get("aggregation"));
        assertEquals("lifetime", semantics(set, "meanRate").get("scope"));
        assertNull(((UnitSupported) property(set, "count").getValueType()).getUnit());
        assertNull(((UnitSupported) property(set, "meanRate").getValueType()).getUnit());
        List<String> ids = List.of("oneMinuteRate", "fiveMinuteRate", "fifteenMinuteRate");
        for (int i = 0; i < ids.size(); i++) {
            Map<?, ?> description = semantics(set, ids.get(i));
            assertEquals("gauge", description.get("kind"));
            assertEquals("rate", description.get("statistic"));
            assertEquals("ewma", description.get("aggregation"));
            assertEquals("exponential", description.get("scope"));
            assertEquals(List.of(60L, 300L, 900L).get(i), description.get("timeConstantSeconds"));
            assertEquals(5L, description.get("tickSeconds"));
            assertEquals(1L, description.get("perSeconds"));
            assertEquals(0D, (Double) metric(set, ids.get(i)).read(meter), 0D);
        }
        now[0] = 5_000_000_000L;
        assertEquals(2D, (Double) metric(set, "oneMinuteRate").read(meter), 0D);
        assertEquals(2D, (Double) metric(set, "meanRate").read(meter), 0D);
    }

    @Test
    public void durationMetadataAndSnapshotUseTheSameContractWithExplicitUnitsAndNoSampleLinks() throws Throwable {
        MetricSet set = MetricSet.of(DurationMetrics.class);
        DurationSummary summary = new DurationSummary();
        assertEquals(6, MetricSet.of(DurationSummary.class).getMetrics().size());
        assertEquals(6, MetricSet.of(DurationSummary.Snapshot.class).getMetrics().size());
        assertEquals(0L, metric(set, "count").read(summary));
        assertEquals("count", semantics(set, "meanDurationSeconds").get("sampleCountProperty"));
        assertEquals("counter", semantics(set, "totalDurationSeconds").get("kind"));
        assertEquals("duration", semantics(set, "totalDurationSeconds").get("semantic"));
        assertEquals("total", semantics(set, "totalDurationSeconds").get("statistic"));
        assertEquals("seconds", unit(property(set, "totalDurationSeconds").getValueType()));
        assertEquals("seconds", unit(property(set, "meanDurationSeconds").getValueType()));
        assertEquals("nanoseconds", unit(property(set, "maxDurationNanos").getValueType()));
        assertEquals("milliseconds", unit(property(set, "maxAtMillis").getValueType()));
        assertEquals("maxDurationNanos", semantics(set, "maxAtMillis").get("valueProperty"));
        summary.record(2_000_000_000L, 100);
        summary.record(1_000_000_000L, 200);
        DurationSummary.Snapshot snapshot = summary.snapshot();
        assertEquals(2L, metric(set, "count").read(snapshot));
        assertEquals(1.5D, (Double) metric(set, "meanDurationSeconds").read(snapshot), 0D);
        assertEquals(2_000_000_000L, metric(set, "maxDurationNanos").read(snapshot));
        assertEquals(100L, metric(set, "maxAtMillis").read(snapshot));
        summary.record(9_000_000_000L, 300);
        assertEquals(100L, metric(set, "maxAtMillis").read(snapshot));
    }

    @Test
    public void fixedWindowDefinitionLinksAvailabilityAndRuntimeBoundariesAcrossConfigurations() throws Throwable {
        MetricSet set = MetricSet.of(WindowMaxMetrics.class);
        assertEquals(5, MetricSet.of(WindowMax.Snapshot.class).getMetrics().size());
        Map<?, ?> description = semantics(set, "maximum");
        assertEquals("maximum", description.get("statistic"));
        assertEquals("fixedWindow", description.get("scope"));
        assertEquals("available", description.get("availableProperty"));
        assertEquals("startMillis", description.get("windowStartProperty"));
        assertEquals("endMillis", description.get("windowEndProperty"));
        WindowMax hourly = new WindowMax(Duration.ofHours(1));
        WindowMax daily = new WindowMax(Duration.ofDays(1), Duration.ofHours(-8));
        hourly.record(0, 1);
        daily.record(42, 1);
        WindowMax.Snapshot hour = hourly.snapshot(2);
        assertEquals(true, metric(set, "available").read(hour));
        assertEquals(0L, metric(set, "maximum").read(hour));
        assertEquals(3_600_000L, metric(set, "endMillis").read(hour));
        assertEquals(-28_800_000L, metric(set, "startMillis").read(daily.snapshot(2)));
        assertEquals(57_600_000L, metric(set, "endMillis").read(daily.snapshot(2)));
        WindowMax.Snapshot expired = hourly.snapshot(3_600_000L);
        assertEquals(false, metric(set, "available").read(expired));
        assertEquals(0L, metric(set, "maximum").read(expired));
        assertEquals(true, metric(set, "available").read(hour));
    }

    @Test
    public void histogramDefinitionExplainsIndependentBucketsAndSnapshotDoesNotLeakMutableArrays() throws Throwable {
        MetricSet set = MetricSet.of(HistogramMetrics.class);
        assertEquals(2, MetricSet.of(FixedHistogram.Snapshot.class).getMetrics().size());
        Map<?, ?> description = semantics(set, "bucketCounts");
        assertEquals("distribution", description.get("statistic"));
        assertEquals("lifetime", description.get("scope"));
        assertEquals("independent", description.get("bucketMode"));
        assertEquals("inclusiveUpper", description.get("boundary"));
        assertEquals(true, description.get("overflowBucket"));
        assertEquals("upperBounds", description.get("boundsProperty"));
        assertEquals("bucketUpperBound", description.get("quantileEstimation"));
        assertEquals("count", unit(((ArrayType) property(set, "bucketCounts").getValueType()).getElementType()));
        FixedHistogram histogram = new FixedHistogram(10, 20);
        long[] counts = histogram.createCounts();
        histogram.record(counts, 10);
        histogram.record(counts, 11);
        histogram.record(counts, 99);
        FixedHistogram.Snapshot snapshot = histogram.snapshotMetrics(counts);
        assertArrayEquals(new long[]{1, 1, 1}, (long[]) metric(set, "bucketCounts").read(snapshot));
        snapshot.getUpperBounds()[0] = -1;
        snapshot.getBucketCounts()[0] = 99;
        histogram.record(counts, 10);
        assertArrayEquals(new long[]{10, 20}, snapshot.getUpperBounds());
        assertArrayEquals(new long[]{1, 1, 1}, snapshot.getBucketCounts());
        assertArrayEquals(new long[]{2, 1, 1}, histogram.snapshotMetrics(counts).getBucketCounts());
    }

    @Test
    public void businessContractsBindUnitsWithoutLosingInheritedStatisticalMeaning() {
        MetricSet bytes = MetricSet.of(ByteRates.class);
        assertEquals("bytes", unit(property(bytes, "count").getValueType()));
        assertEquals("bytes/s", unit(property(bytes, "oneMinuteRate").getValueType()));
        assertEquals("ewma", semantics(bytes, "oneMinuteRate").get("aggregation"));
        assertEquals(60L, semantics(bytes, "oneMinuteRate").get("timeConstantSeconds"));
        MetricSet latency = MetricSet.of(LatencyHistogram.class);
        assertEquals("nanoseconds", unit(((ArrayType) property(latency, "upperBounds").getValueType()).getElementType()));
        assertEquals("independent", semantics(latency, "bucketCounts").get("bucketMode"));
        try {
            MetricSet.of(InvalidArrayUnit.class);
            fail("text elements cannot have duration units");
        } catch (org.hswebframework.web.exception.I18nSupportException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    @Test
    public void sharedInterfaceMessagesAreAvailableThroughOverriddenImplementationsAndSerializableDefinitions() {
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("i18n/jetlinks-core/messages");
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);
        for (Class<?> contract : List.of(RateMetrics.class, DurationMetrics.class, WindowMaxMetrics.class,
            HistogramMetrics.class, RateMeter.class, DurationSummary.class, DurationSummary.Snapshot.class,
            WindowMax.Snapshot.class, FixedHistogram.Snapshot.class)) {
            MetricSet set = MetricSet.of(contract);
            for (Metric metric : set.getMetrics()) {
                PropertyMetadata property = metric.getMetadata();
                String english = MetadataUtils.resolveI18nMessage(Locale.ENGLISH, property, "name", "missing", messages);
                String chinese = MetadataUtils.resolveI18nMessage(Locale.CHINESE, property, "name", "missing", messages);
                assertNotEquals(contract.getName() + "." + metric.getId(), "missing", english);
                assertNotEquals(english, chinese);
                assertNotEquals("missing", MetadataUtils.resolveI18nMessage(
                    Locale.ENGLISH, property, "description", "missing", messages));
                assertEquals(chinese, property.getName());
                for (String language : LANGUAGES) {
                    Locale locale = Locale.forLanguageTag(language.replace('_', '-'));
                    String name = MetadataUtils.resolveI18nMessage(locale, property, "name", "missing", messages);
                    String description = MetadataUtils.resolveI18nMessage(locale, property, "description", "missing", messages);
                    assertEquals(language + ":" + metric.getId(), messages.getMessage(
                        declaration(contract).getName() + "." + metric.getId() + ".title", null, locale), name);
                    assertNotEquals(language + ":" + metric.getId(), "missing", description);
                }
            }
            // 元数据协议只保留稳定值，外部无需加载算法类或 Java 枚举。
            String json = set.getMetadata().toJson().toJSONString();
            assertFalse(json.contains(MetricStatistic.class.getName()));
            assertNotNull(JSON.parseObject(json).getJSONArray("properties"));
        }
    }

    private static Metric metric(MetricSet set, String id) {
        return set.getMetrics().stream().filter(metric -> id.equals(metric.getId())).findFirst().orElseThrow();
    }

    private static Class<?> declaration(Class<?> type) {
        if (RateMetrics.class.isAssignableFrom(type)) return RateMetrics.class;
        if (DurationMetrics.class.isAssignableFrom(type)) return DurationMetrics.class;
        if (WindowMaxMetrics.class.isAssignableFrom(type)) return WindowMaxMetrics.class;
        return HistogramMetrics.class;
    }

    private static Properties messages(String language) throws Exception {
        String path = "/i18n/jetlinks-core/messages_" + language + ".properties";
        Properties properties = new Properties() {
            @Override public synchronized Object put(Object key, Object value) {
                assertFalse(path + ": duplicate " + key, containsKey(key));
                return super.put(key, value);
            }
        };
        try (InputStream input = BuiltinMetricsMetadataTest.class.getResourceAsStream(path)) {
            assertNotNull(path, input);
            properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
        }
        return properties;
    }

    private static List<String> arguments(String text) {
        Matcher matcher = Pattern.compile("\\{[0-9]+(?:,[^}]+)?}").matcher(text);
        List<String> arguments = new ArrayList<>();
        while (matcher.find()) arguments.add(matcher.group());
        Collections.sort(arguments);
        return arguments;
    }

    private static PropertyMetadata property(MetricSet set, String id) {
        return metric(set, id).getMetadata();
    }

    private static Map<?, ?> semantics(MetricSet set, String id) {
        return (Map<?, ?>) property(set, id).getExpand("metric").orElseThrow();
    }

    private static String unit(DataType type) {
        return ((UnitSupported) type).getUnit().getId();
    }

    public interface ByteRates extends RateMetrics {
        @Override @Counter(unit = "bytes") long getCount();
        @Override @Gauge(unit = "bytes/s") double getOneMinuteRate();
    }

    public interface LatencyHistogram extends HistogramMetrics {
        @Override @Gauge(unit = "nanoseconds") long[] getUpperBounds();
    }

    public interface InvalidArrayUnit {
        @Gauge(unit = "seconds") String[] getValues();
    }
}
