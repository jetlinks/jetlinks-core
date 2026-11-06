package org.jetlinks.core.metadata;

import org.jetlinks.core.annotation.GraphSubject;
import org.jetlinks.core.annotation.Attr;
import org.jetlinks.core.annotation.Expands;
import org.jetlinks.core.annotation.SemanticRole;
import org.jetlinks.core.annotation.SemanticTypes;
import org.jetlinks.core.annotation.command.FunctionSemantics;
import org.jetlinks.core.annotation.ui.Presentation;
import org.jetlinks.core.command.AbstractCommand;
import org.jetlinks.core.command.CommandMetadataResolver;
import org.jetlinks.core.utils.MetadataUtils;
import org.junit.Test;
import org.springframework.core.ResolvableType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

/** 业务 DTO 经已有注解解析路径输出标准语义，无额外业务解析器。 */
public class PresentationAnnotationsTest {
    @Test
    public void should_describe_subject_identity_and_primary_value_using_composed_annotations() {
        org.jetlinks.core.metadata.types.ObjectType output =
            (org.jetlinks.core.metadata.types.ObjectType) MetadataUtils.parseType(ResolvableType.forClass(Summary.class));
        PropertyMetadata id = output.getProperty("objectId").orElseThrow();
        assertTrue(MetadataSemantics.propertyRole(id).isEmpty());
        assertEquals(Boolean.TRUE, id.getExpands().get("graphSubject"));
        PropertyMetadata count = output.getProperty("count").orElseThrow();
        assertEquals(Boolean.TRUE, ((Map<?, ?>) count.getExpands().get("presentation")).get("primary"));
        assertNull(count.getExpands().get("graphSubject"));
    }

    @Test
    public void should_encode_typed_roles_as_standard_strings_and_validate_time_binding() {
        var output = (org.jetlinks.core.metadata.types.ObjectType) MetadataUtils.parseType(ResolvableType.forClass(Observation.class));
        assertArrayEquals(new String[]{"observation", "result"}, (String[]) output.getExpands().get("semanticTypes"));
        assertEquals(PropertySemanticRole.result, MetadataSemantics.propertyRole(output.getProperty("value").orElseThrow()).orElseThrow());
        assertEquals("sampledAt", ((Map<?, ?>) output.getProperty("value").orElseThrow().getExpands().get("presentation")).get("observedAt"));
        assertEquals(PropertySemanticRole.identity, MetadataSemantics.propertyRole(output.getProperty("recordId").orElseThrow()).orElseThrow());
        assertFalse(output.getProperty("recordId").orElseThrow().getExpand("graphSubject").isPresent());
        assertEquals("observedAt", output.getProperty("sampledAt").orElseThrow().getExpands().get("semanticRole"));
    }

    @Test
    public void should_classify_commands_without_defaulting_unclassified_commands_to_query() {
        assertEquals(FunctionKind.query, MetadataSemantics.functionKind(CommandMetadataResolver.resolve(Query.class)).orElseThrow());
        assertTrue(MetadataSemantics.functionKind(CommandMetadataResolver.resolve(Unclassified.class)).isEmpty());
    }

    @Test
    public void should_reject_conflicts_without_using_annotation_order() {
        assertInvalid(ConflictingRole.class);
        assertInvalid(ConflictingRoleReversed.class);
    }

    @Test
    public void should_merge_equivalent_semantic_sets_across_json_arrays_and_collections() {
        var original = new String[]{"observation", "domain:reading", "result"};
        Map<String, Object> expands = new HashMap<>(Map.of("semanticTypes", original));
        MetadataUtils.mergeExpands(expands, Map.of("semanticTypes", List.of("result", "observation", "domain:reading", "result")));
        var json = com.alibaba.fastjson.JSON.parseObject(com.alibaba.fastjson.JSON.toJSONString(expands));
        MetadataUtils.mergeExpands(expands, json);
        assertSame(original, expands.get("semanticTypes"));
        assertArrayEquals(new String[]{"observation", "domain:reading", "result"}, original);
    }

    @Test
    public void should_still_reject_different_semantic_sets_and_scalar_roles() {
        for (String key : List.of("semanticTypes", "semanticRole", "functionKind")) {
            Map<String, Object> expands = new HashMap<>(Map.of(key,
                "semanticTypes".equals(key) ? List.of("domain:first") : "first"));
            Object different = "semanticTypes".equals(key) ? new String[]{"domain:second"} : "second";
            reactor.test.StepVerifier.create(reactor.core.publisher.Mono.fromRunnable(
                () -> MetadataUtils.mergeExpands(expands, Map.of(key, different))))
                .expectError(org.hswebframework.web.exception.I18nSupportException.class).verify();
        }
    }

    @Test
    public void should_reject_ambiguous_subjects_invalid_times_and_unexported_markers() {
        assertInvalid(TwoSubjects.class);
        assertInvalid(InvalidTime.class);
        assertInvalid(Unexported.class);
    }

    @Test
    public void should_preserve_unknown_roles_and_multiple_primary_fields() {
        var output = (org.jetlinks.core.metadata.types.ObjectType) MetadataUtils.parseType(ResolvableType.forClass(OpenVocabulary.class));
        assertEquals("domain:reading", output.getProperty("first").orElseThrow().getExpands().get("semanticRole"));
        assertEquals(2, output.getProperties().size());
        MetadataSemantics.validateOutput(output);
    }

    @Test
    public void should_merge_split_presentation_declarations_on_getter_and_field() {
        var output = (org.jetlinks.core.metadata.types.ObjectType) MetadataUtils.parseType(ResolvableType.forClass(SplitPresentation.class));
        assertEquals(Map.of("primary", true, "observedAt", "sampledAt"),
                     output.getProperty("value").orElseThrow().getExpands().get("presentation"));
    }

    @Test
    public void should_reject_presentation_conflicts_across_getter_and_field_and_map_merge() {
        assertInvalid(ConflictingPresentation.class);
        Map<String, Object> original = Map.of("primary", true, "observedAt", "sampledAt");
        Map<String, Object> target = new HashMap<>(Map.of("presentation", original));
        reactor.test.StepVerifier.create(reactor.core.publisher.Mono.fromRunnable(
            () -> MetadataUtils.mergeExpands(target, Map.of("presentation", Map.of("observedAt", "otherTime")))))
            .expectError(org.hswebframework.web.exception.I18nSupportException.class).verify();
        assertSame(original, target.get("presentation"));
    }

    public static class SplitPresentation {
        @Schema @Expands(key = "presentation", value = @Attr(key = "observedAt", value = "sampledAt"))
        private double value;
        @Schema private long sampledAt;
        @Presentation(primary = true)
        public double getValue() { return value; }
        public void setValue(double value) { this.value = value; }
    }

    public static class ConflictingPresentation {
        @Schema @Presentation(primary = false) private double value;
        @Presentation(primary = true)
        public double getValue() { return value; }
        public void setValue(double value) { this.value = value; }
    }

    public static class Summary {
        @GraphSubject
        @Schema(title = "对象 ID")
        private String objectId;
        @Presentation(primary = true)
        @Schema(title = "数量")
        private long count;

        public String getObjectId() { return objectId; }
        public long getCount() { return count; }
    }

    private static void assertInvalid(Class<?> type) {
        reactor.test.StepVerifier.create(reactor.core.publisher.Mono.fromSupplier(
            () -> MetadataUtils.parseType(ResolvableType.forClass(type))))
            .expectError(org.hswebframework.web.exception.I18nSupportException.class).verify();
    }

    @SemanticTypes({SemanticType.observation, SemanticType.result})
    public static class Observation {
        @Schema(title = "记录") @SemanticRole(PropertySemanticRole.identity)
        private String recordId;
        @Schema(title = "被描述对象") @GraphSubject
        private String subject;
        @Schema(title = "值") @SemanticRole(PropertySemanticRole.result) @Presentation(primary = true, observedAt = "sampledAt")
        private double value;
        @Schema(title = "采样时间") @SemanticRole(PropertySemanticRole.observedAt)
        private long sampledAt;
    }

    @FunctionSemantics(FunctionKind.query)
    public static class Query extends AbstractCommand<String, Query> {}
    public static class Unclassified extends AbstractCommand<String, Unclassified> {}

    public static class ConflictingRole {
        @Schema @SemanticRole(PropertySemanticRole.result) @Expands(@Attr(key = "semanticRole", value = "status"))
        private String value;
    }
    public static class ConflictingRoleReversed {
        @Schema @Expands(@Attr(key = "semanticRole", value = "status")) @SemanticRole(PropertySemanticRole.result)
        private String value;
    }
    public static class TwoSubjects {
        @Schema @GraphSubject private String first;
        @Schema @GraphSubject private String second;
    }
    public static class InvalidTime {
        @Schema @Presentation(primary = true, observedAt = "name") private double value;
        @Schema private String name;
    }
    public static class Unexported {
        @GraphSubject private String subject;
    }
    public static class OpenVocabulary {
        @Schema @Presentation(primary = true) @Expands(@Attr(key = "semanticRole", value = "domain:reading"))
        private double first;
        @Schema @Presentation(primary = true) private double second;
    }
}
