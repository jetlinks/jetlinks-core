package org.jetlinks.core.metadata;

import org.jetlinks.core.annotation.GraphSubject;
import org.jetlinks.core.annotation.ui.Presentation;
import org.jetlinks.core.utils.MetadataUtils;
import org.junit.Test;
import org.springframework.core.ResolvableType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

import static org.junit.Assert.*;

/** 业务 DTO 经已有注解解析路径输出标准语义，无额外业务解析器。 */
public class PresentationAnnotationsTest {
    @Test
    public void should_describe_subject_identity_and_primary_value_using_composed_annotations() {
        org.jetlinks.core.metadata.types.ObjectType output =
            (org.jetlinks.core.metadata.types.ObjectType) MetadataUtils.parseType(ResolvableType.forClass(Summary.class));
        PropertyMetadata id = output.getProperty("objectId").orElseThrow();
        assertEquals(PropertySemanticRole.identity, MetadataSemantics.propertyRole(id).orElseThrow());
        assertEquals(Boolean.TRUE, id.getExpands().get("graphSubject"));
        PropertyMetadata count = output.getProperty("count").orElseThrow();
        assertEquals(Boolean.TRUE, ((Map<?, ?>) count.getExpands().get("presentation")).get("primary"));
        assertNull(count.getExpands().get("graphSubject"));
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
}
