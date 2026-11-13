package org.jetlinks.core.metadata;

import com.alibaba.fastjson.JSON;
import org.hswebframework.web.exception.I18nSupportException;
import org.jetlinks.core.annotation.PropertyReference;
import org.jetlinks.core.annotation.SemanticRole;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.core.utils.MetadataUtils;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

import static org.junit.Assert.*;

public class PropertyReferenceTest {
    static class WorkOrder {
        @PropertyReference(targetType = "project", relationId = "belongsTo", labelProperty = "projectName")
        @SemanticRole(PropertySemanticRole.modelId)
        String projectId;

        @PropertyReference(targetType = "person", reverse = true)
        String reviewer;
    }

    @Test
    public void annotationAndDynamicDeclarationsShareEncodingAndPreserveOriginalType() throws Exception {
        var property = SimplePropertyMetadata.of("projectId", "所属项目", StringType.GLOBAL);
        property.setExpands(MetadataUtils.parseExpands(WorkOrder.class.getDeclaredField("projectId")));
        var mapping = new PropertyReferenceMapping("project", "belongsTo", false, "projectName");
        assertEquals(mapping, MetadataSemantics.reference(property).orElseThrow());
        var dynamic = SimplePropertyMetadata.of("projectId", "所属项目", StringType.GLOBAL);
        MetadataSemantics.withReference(dynamic, mapping);
        MetadataSemantics.withPropertyRole(dynamic, PropertySemanticRole.modelId);
        assertEquals(property.getExpands(), dynamic.getExpands());
        var decoded = new SimplePropertyMetadata();
        decoded.fromJson(JSON.parseObject(property.toJson().toJSONString()));
        assertEquals(mapping, MetadataSemantics.reference(decoded).orElseThrow());
        assertEquals(StringType.ID, decoded.getValueType().getId());
        assertEquals(PropertySemanticRole.modelId, MetadataSemantics.propertyRole(decoded).orElseThrow());
        assertEquals("projectId", decoded.getId());
    }

    @Test
    public void optionalRelationDoesNotCreateFactsAndUnknownExtensionsSurvive() throws Exception {
        var property = SimplePropertyMetadata.of("reviewer", "审核人", StringType.GLOBAL);
        property.setExpands(MetadataUtils.parseExpands(WorkOrder.class.getDeclaredField("reviewer")));
        assertEquals(new PropertyReferenceMapping("person", null, true, null), MetadataSemantics.reference(property).orElseThrow());
        property.expand(MetadataSemantics.REFERENCE_KEY, Map.of("targetType", "person", "future", "keep"));
        MetadataSemantics.withReference(property, new PropertyReferenceMapping("person", null));
        assertEquals("keep", ((Map<?, ?>) property.getExpand(MetadataSemantics.REFERENCE_KEY).orElseThrow()).get("future"));
        assertFalse(property.getExpands().containsKey("relations"));
    }

    @Test
    public void conflictingTargetsAreRejectedWithoutOverwritingTheDeclaration() {
        var property = SimplePropertyMetadata.of("owner", "负责人", StringType.GLOBAL);
        MetadataSemantics.withReference(property, new PropertyReferenceMapping("person", "owner"));
        StepVerifier.create(Mono.fromRunnable(() -> MetadataSemantics.withReference(property,
            new PropertyReferenceMapping("org", "owner")))).expectError(I18nSupportException.class).verify();
        assertEquals("person", MetadataSemantics.reference(property).orElseThrow().targetType());
        property.expand(MetadataSemantics.REFERENCE_KEY, Map.of("targetType", "person", "reverse", "false"));
        StepVerifier.create(Mono.fromRunnable(() -> MetadataSemantics.reference(property)))
            .expectError(I18nSupportException.class).verify();
    }

    @Test
    public void mappingsAreSerializableAndNamesNeverImplyReferences() throws Exception {
        var mapping = new PropertyReferenceMapping("project", "belongsTo");
        var bytes = new ByteArrayOutputStream();
        try (var output = new ObjectOutputStream(bytes)) { output.writeObject(mapping); }
        try (var input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            assertEquals(mapping, input.readObject());
        }
        var property = SimplePropertyMetadata.of("productId", "产品id", StringType.GLOBAL);
        MetadataSemantics.withPropertyRole(property, PropertySemanticRole.identifier);
        assertTrue(MetadataSemantics.reference(property).isEmpty());
        assertEquals(PropertySemanticRole.identifier, MetadataSemantics.propertyRole(property).orElseThrow());
    }
}
