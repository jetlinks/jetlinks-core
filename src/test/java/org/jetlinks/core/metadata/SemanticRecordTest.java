package org.jetlinks.core.metadata;

import org.jetlinks.core.annotation.Observation;
import org.jetlinks.core.annotation.SemanticRole;
import org.jetlinks.core.metadata.types.*;
import org.jetlinks.core.utils.MetadataUtils;
import org.junit.Test;
import org.springframework.core.ResolvableType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class SemanticRecordTest {
    @Test
    public void annotationMappingAndJsonRoundTripKeepIndependentTimesAndQuality() {
        var parsed = (ObjectType) MetadataUtils.parseType(ResolvableType.forClass(Reading.class));
        var type = (ObjectType) DataTypes.fromJsonNow(com.alibaba.fastjson.JSON.parseObject(parsed.toJson().toJSONString()));
        var row = Map.of("first", 0, "second", false, "at", 0L, "otherAt", 200L, "valid", false);
        var first = SemanticRecord.value(type, row, "first");
        var second = SemanticRecord.value(type, row, "second");
        assertEquals(0, first.value());
        assertEquals(false, second.value());
        assertEquals(Long.valueOf(0), SemanticRecord.observedAt(first));
        assertEquals(Long.valueOf(200), SemanticRecord.observedAt(second));
        assertEquals(false, SemanticRecord.quality(first));
        assertNull(SemanticRecord.roleValue(type, row, PropertySemanticRole.result));
        assertEquals(new ObservationMapping("at", "valid"), MetadataSemantics.observation(first.field()));
        assertEquals(Map.of("observedAt", "at", "quality", "valid"), first.field().getExpand("presentation").orElseThrow());
        assertEquals(5, row.size());
    }

    @Test
    public void nestedPathsAreRelativeAndNeverFlattenCollections() {
        var inner = new ObjectType();
        inner.setProperties(List.of(
            SimplePropertyMetadata.of("value", "", DoubleType.GLOBAL).expand("presentation", Map.of("observedAt", "at")),
            SimplePropertyMetadata.of("at", "", LongType.GLOBAL)));
        var type = new ObjectType();
        type.setProperties(List.of(SimplePropertyMetadata.of("sensor", "", inner),
            SimplePropertyMetadata.of("rows", "", new ArrayType().elementType(inner))));
        var row = Map.of("sensor", Map.of("value", 0, "at", 100L), "rows", List.of(Map.of("value", 2)));
        assertEquals(Long.valueOf(100), SemanticRecord.observedAt(SemanticRecord.value(type, row, "sensor.value")));
        assertEquals(List.of("sensor.value", "sensor.at", "rows"), SemanticRecord.fields(type, row).stream().map(SemanticRecord.FieldValue::path).toList());
        assertNull(SemanticRecord.field(type, "rows.value"));
        assertNull(SemanticRecord.value(type, row, "sensor.value.extra"));
        assertNull(SemanticRecord.field(type, "sensor..value"));
        assertNull(SemanticRecord.field(type, "a.b.c.d.e.f.g.h.i"));
    }

    @Test
    public void unknownWordsAndLegacyRolesRemainUnmodified() {
        var field = SimplePropertyMetadata.of("reading", "", DoubleType.GLOBAL).expand("semanticRole", "value");
        assertEquals(PropertySemanticRole.result, MetadataSemantics.propertyRole(field).orElseThrow());
        assertEquals("value", field.getExpand("semanticRole").orElseThrow());
        field.expand("semanticRole", "domain:reading");
        assertTrue(MetadataSemantics.propertyRole(field).isEmpty());
        assertEquals("domain:reading", field.getExpand("semanticRole").orElseThrow());
        var model = new SimpleDeviceMetadata();
        model.expand("semanticTypes", List.of("observation", "domain:reading", "observation"));
        assertEquals(List.of("observation", "domain:reading"), List.copyOf(MetadataSemantics.semanticTypeTerms(model)));
        assertEquals(Set.of(SemanticType.observation), MetadataSemantics.semanticTypes(model));
        assertEquals(3, ((List<?>) model.getExpand("semanticTypes").orElseThrow()).size());
    }

    @Test
    public void absentInvalidOrUnmappedTimesStayUnknown() {
        var type = (ObjectType) MetadataUtils.parseType(ResolvableType.forClass(Reading.class));
        for (var row : List.of(Map.of("first", 1), Map.of("first", 1, "at", -0.5), Map.of("first", 1, "at", "invalid"),
            Map.of("first", 1, "at", Double.NaN))) {
            assertNull(SemanticRecord.observedAt(SemanticRecord.value(type, row, "first")));
        }
        var other = new ObjectType();
        other.setProperties(List.of(SimplePropertyMetadata.of("value", "", DoubleType.GLOBAL),
            SimplePropertyMetadata.of("time", "", LongType.GLOBAL).expand("semanticRole", "observedAt")));
        assertNull(SemanticRecord.observedAt(SemanticRecord.value(other, Map.of("value", 1, "time", 100L), "value")));
    }

    @Test
    public void legacyRowsOnlyUseUnambiguousDeclaredTimeWithoutChangingWireValues() {
        var type = new ObjectType();
        var first = SimplePropertyMetadata.of("value", "", DoubleType.GLOBAL).expand("semanticRole", "value");
        var time = SimplePropertyMetadata.of("time", "", LongType.GLOBAL).expand("semanticRole", "observedAt");
        type.setProperties(List.of(first, time));
        var row = Map.of("value", 0, "time", 0L, "second", 2);
        assertEquals(Long.valueOf(0), SemanticRecord.observedAt(SemanticRecord.value(type, row, "value")));
        type.setProperties(List.of(first, time, SimplePropertyMetadata.of("second", "", DoubleType.GLOBAL)
            .expand("semanticRole", "result")));
        assertNull(SemanticRecord.observedAt(SemanticRecord.value(type, row, "value")));
        type.setProperties(List.of(first, time, SimplePropertyMetadata.of("otherTime", "", LongType.GLOBAL)
            .expand("semanticRole", "observedAt")));
        assertNull(SemanticRecord.observedAt(SemanticRecord.value(type, row, "value")));
    }

    @Test
    public void malformedOrUnresolvedMappingsFailAtModelBoundary() {
        var type = new ObjectType();
        var value = SimplePropertyMetadata.of("value", "", DoubleType.GLOBAL);
        type.setProperties(List.of(value));
        for (var mapping : List.of(Map.of("quality", "missing"), Map.of("observedAt", true), Map.of("observedAt", "value.extra"))) {
            value.expand("presentation", mapping);
            reactor.test.StepVerifier.create(reactor.core.publisher.Mono.fromRunnable(() -> MetadataSemantics.validateOutput(type)))
                .expectError(org.hswebframework.web.exception.I18nSupportException.class).verify();
        }
    }

    @Test
    public void structuredSemanticValuesRetainTheirDeclaredModelWithoutImplicitFlattening() {
        var structure = new ObjectType();
        structure.setProperties(List.of(SimplePropertyMetadata.of("a", "", LongType.GLOBAL)));
        var type = new ObjectType();
        for (String role : List.of("result", "domain:position")) {
            type.setProperties(List.of(MetadataSemantics.withPropertyRole(SimplePropertyMetadata.of("result", "", structure), role)));
            var row = Map.of("result", Map.of("a", 0));
            assertEquals(1, SemanticRecord.fields(type, row).size());
            assertEquals(Map.of("a", 0), SemanticRecord.fields(type, row).get(0).value());
            assertEquals(0, SemanticRecord.value(type, row, "result.a").value());
            assertEquals(role, MetadataSemantics.propertyRoleTerm(type.getProperties().get(0)).orElseThrow());
        }
    }

    @Test
    public void timestampRequiresExactMillisecondsAndAllowsDatesBeforeEpoch() {
        var type = new ObjectType();
        type.setProperties(List.of(SimplePropertyMetadata.of("at", "Sample", LongType.GLOBAL)));
        for (Number invalid : List.of(-0.5, 0.5, Double.NaN, Double.POSITIVE_INFINITY,
            new java.math.BigInteger("9223372036854775808"), new java.math.BigDecimal("1234567890123.001"))) {
            assertNull(SemanticRecord.timestamp(SemanticRecord.value(type, Map.of("at", invalid), "at")));
        }
        for (long valid : List.of(-1L, 0L, Long.MIN_VALUE, Long.MAX_VALUE)) {
            assertEquals(Long.valueOf(valid), SemanticRecord.timestamp(SemanticRecord.value(type, Map.of("at", valid), "at")));
        }
        type.setProperties(List.of(SimplePropertyMetadata.of("at", "Sample",
            new DateTimeType().format("yyyy-MM-dd'T'HH:mm:ssX").timeZone(java.time.ZoneOffset.UTC))));
        assertEquals(Long.valueOf(-1000), SemanticRecord.timestamp(SemanticRecord.value(type,
            Map.of("at", "1969-12-31T23:59:59Z"), "at")));
    }

    @Test
    public void resultMetadataAndLabelsOnlyQualifyTheUniqueResultInTheirRecord() {
        var type = new ObjectType();
        var result = MetadataSemantics.withPropertyRole(SimplePropertyMetadata.of("reading", "Value", DoubleType.GLOBAL), PropertySemanticRole.result);
        var definition = MetadataSemantics.withPropertyRole(SimplePropertyMetadata.of("definition", "Definition", new ObjectType()), PropertySemanticRole.valueMetadata);
        var property = MetadataSemantics.withPropertyRole(SimplePropertyMetadata.of("property", "Property", StringType.GLOBAL), PropertySemanticRole.observedProperty);
        type.setProperties(List.of(result, definition, property));
        var metadata = SimplePropertyMetadata.of("temperature", "Temperature", new DoubleType()
            .unit(org.jetlinks.core.metadata.unit.UnifyUnit.celsiusDegrees));
        var row = Map.of("reading", 21, "definition", metadata.toJson(), "property", "temperature");
        var value = SemanticRecord.value(type, row, "reading");
        assertEquals("temperature", SemanticRecord.resultPropertyId(value));
        assertSame(row.get("definition"), SemanticRecord.resultQualifier(value, PropertySemanticRole.valueMetadata));
        assertEquals("temperature", SemanticRecord.resultPropertyId(SemanticRecord.value(type,
            Map.of("reading", 21, "property", "temperature"), "reading")));
        assertNull(SemanticRecord.resultPropertyId(SemanticRecord.value(type,
            Map.of("reading", 21, "definition", metadata.toJson(), "property", "pressure"), "reading")));
        type.setProperties(List.of(result, definition, property,
            MetadataSemantics.withPropertyRole(SimplePropertyMetadata.of("pressure", "Pressure", DoubleType.GLOBAL), PropertySemanticRole.result)));
        assertNull(SemanticRecord.resultPropertyId(value));
        assertNull(SemanticRecord.resultQualifier(value, PropertySemanticRole.observedProperty));
    }

    @Test
    public void conflictingQualityMappingsAndHiddenSemanticFieldsFailThroughOriginalParser() {
        for (var declaration : List.of(ConflictingQuality.class, HiddenObservation.class)) {
            reactor.test.StepVerifier.create(reactor.core.publisher.Mono.fromSupplier(
                () -> MetadataUtils.parseType(ResolvableType.forClass(declaration))))
                .expectError(org.hswebframework.web.exception.I18nSupportException.class).verify();
        }
    }

    public static class ConflictingQuality {
        @Schema @Observation(quality = "valid") private double value;
        @Schema private boolean valid;
        @Schema private boolean other;
        @Observation(quality = "other") public double getValue() { return value; }
        public void setValue(double value) { this.value = value; }
    }

    public static class HiddenObservation {
        @Observation(observedAt = "time") private double hidden;
    }

    public static class Reading {
        @Schema @SemanticRole(PropertySemanticRole.result) @Observation(observedAt = "at", quality = "valid")
        private double first;
        @Schema @SemanticRole(PropertySemanticRole.result) @Observation(observedAt = "otherAt")
        private boolean second;
        @Schema @SemanticRole(PropertySemanticRole.observedAt) private long at;
        @Schema @SemanticRole(PropertySemanticRole.observedAt) private long otherAt;
        @Schema @SemanticRole(PropertySemanticRole.quality) private boolean valid;
    }
}
