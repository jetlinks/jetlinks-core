package org.jetlinks.core.metadata;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.hswebframework.web.exception.I18nSupportException;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.things.relation.Relation;
import org.jetlinks.core.things.relation.RelationSemantic;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class MetadataSemanticsTest {

    @Test
    public void should_preserve_alarm_roles_and_future_vocabulary() {
        SimpleDeviceMetadata model = new SimpleDeviceMetadata();
        model.setId("alarmOccurrence");
        model.expand("semanticTypes", Arrays.asList("event", "result", "urn:example:future"));
        SimplePropertyMetadata time = SimplePropertyMetadata.of("alarmTime", "发生时间", DateTimeType.GLOBAL);
        time.expand(MetadataSemantics.PROPERTY_ROLE, "occurredAt");
        model.addProperty(time);

        assertEquals(2, MetadataSemantics.semanticTypes(model).size());
        assertTrue(MetadataSemantics.semanticTypes(model).contains(SemanticType.event));
        assertEquals(PropertySemanticRole.occurredAt, MetadataSemantics.propertyRole(time).orElseThrow());
        JSONObject json = JSON.parseObject(model.toJson().toJSONString());
        assertTrue(json.getJSONObject("expands").getJSONArray("semanticTypes").contains("urn:example:future"));
        assertFalse(json.containsKey("relations"));
    }

    @Test
    public void should_not_classify_unknown_or_malformed_declarations() {
        SimpleFunctionMetadata function = new SimpleFunctionMetadata();
        function.expand("functionKind", "future-kind");
        assertFalse(MetadataSemantics.functionKind(function).isPresent());
        function.expand("functionKind", true);
        StepVerifier.create(Mono.fromRunnable(() -> MetadataSemantics.functionKind(function)))
            .expectError(I18nSupportException.class).verify();
        SimpleDeviceMetadata model = new SimpleDeviceMetadata();
        model.expand("semanticTypes", "event");
        StepVerifier.create(Mono.fromRunnable(() -> MetadataSemantics.semanticTypes(model)))
            .expectError(I18nSupportException.class).verify();
    }

    @Test
    public void should_roundtrip_relation_semantics_without_loading_related_types() {
        Relation relation = Relation.parse(JSON.parseObject("{\"id\":\"target\",\"reverse\":false,"
            + "\"expands\":{\"semantic\":\"about\",\"future\":{\"v\":1}}}"));
        SimpleDeviceMetadata model = new SimpleDeviceMetadata();
        model.setId("alarm-schema-v2");
        model.setRelations(Collections.singletonMap("device", Collections.singletonList(relation)));
        SimpleDeviceMetadata decoded = new SimpleDeviceMetadata();
        decoded.fromJson(JSON.parseObject(model.toJson().toJSONString()));

        assertEquals("alarm-schema-v2", decoded.getId());
        Relation actual = decoded.getRelations("device").get(0);
        assertEquals(RelationSemantic.about, MetadataSemantics.relationSemantic(actual).orElseThrow());
        assertEquals(relation.toJson(), actual.toJson());
        assertTrue(actual.getExpands().get("future") instanceof Map);
    }

    @Test
    public void should_reject_conflicting_relations_in_combined_models() {
        SimpleDeviceMetadata first = new SimpleDeviceMetadata();
        first.setRelations(Collections.singletonMap("device", Collections.singletonList(
            Relation.parse(JSON.parseObject("{\"id\":\"target\",\"expands\":{\"semantic\":\"about\"}}")))));
        SimpleDeviceMetadata second = new SimpleDeviceMetadata();
        second.setRelations(Collections.singletonMap("device", Collections.singletonList(
            Relation.parse(JSON.parseObject("{\"id\":\"target\",\"expands\":{\"semantic\":\"used\"}}")))));
        StepVerifier.create(Mono.fromRunnable(() -> new CompositeThingMetadata(first, second).getRelations()))
            .expectError(I18nSupportException.class).verify();
        assertEquals(1, new CompositeThingMetadata(first, first).getRelations("device").size());
    }

    @Test
    public void should_keep_instance_semantics_independent_of_other_expands_and_json_encoding() {
        SimpleDeviceMetadata instance = new SimpleDeviceMetadata();
        instance.setId("reading-template");
        instance.expand("semanticTypes", List.of("observation", "urn:example:quality"));
        SimpleDeviceMetadata type = new SimpleDeviceMetadata();
        type.expand("semanticTypes", List.of("resource"));
        CompositeThingMetadata combined = new CompositeThingMetadata(instance, type);

        assertEquals(instance.getExpands().get("semanticTypes"), combined.getExpands().get("semanticTypes"));
        type.expand("supportAssert", false);
        type.expand("future", Map.of("revision", 2));
        assertEquals(instance.getExpands().get("semanticTypes"), combined.getExpands().get("semanticTypes"));
        assertEquals(Map.of("revision", 2), combined.getExpands().get("future"));
        assertEquals(List.of("resource"), type.getExpands().get("semanticTypes"));

        String json = combined.toJson().toJSONString();
        assertEquals(json.indexOf("\"semanticTypes\""), json.lastIndexOf("\"semanticTypes\""));
        assertEquals(combined.getExpands(), JSON.parseObject(json).getJSONObject("expands"));
        assertEquals(Collections.singleton(SemanticType.observation), MetadataSemantics.semanticTypes(combined));
    }
}
