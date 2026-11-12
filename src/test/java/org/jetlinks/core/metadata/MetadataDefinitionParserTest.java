package org.jetlinks.core.metadata;

import org.hswebframework.web.exception.I18nSupportException;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/** 结构解析不依赖 JSON 引擎，所有使用方遵循同一对象及目录边界。 */
public class MetadataDefinitionParserTest {

    @Test
    public void should_decode_in_order_without_modifying_source_containers() {
        Map<String, Object> first = new LinkedHashMap<>(Map.of("id", "first"));
        List<Map<String, Object>> source = new ArrayList<>(List.of(first, Map.of("id", "second")));
        List<Object> decoded = MetadataDefinitionParser.parseList(source, definition -> definition.remove("id"));
        assertEquals(List.of("first", "second"), decoded);
        assertEquals("first", first.get("id"));
        source.clear();
        assertEquals(2, decoded.size());
    }

    @Test
    public void should_decode_missing_and_null_directories_as_empty() {
        assertTrue(MetadataDefinitionParser.parseList(null, definition -> definition).isEmpty());
        assertTrue(MetadataDefinitionParser.parseList(List.of(), definition -> definition).isEmpty());
        assertEquals(Map.of(), MetadataDefinitionParser.parseObject(Map.of()));
    }

    @Test
    public void should_reject_non_object_entries_and_non_string_keys() {
        for (Object entry : Arrays.asList(null, 7, true, "{}", List.of(), new Object(), Map.of(1, "value"))) {
            assertInvalid(() -> MetadataDefinitionParser.parseObject(entry));
            assertInvalid(() -> MetadataDefinitionParser.parseList(Arrays.asList(entry), definition -> definition));
        }
    }

    @Test
    public void should_reject_non_collection_directories() {
        for (Object directory : List.of(7, true, "[]", Map.of(), new Object[0])) {
            assertInvalid(() -> MetadataDefinitionParser.parseList(directory, definition -> definition));
        }
    }

    private static void assertInvalid(Runnable action) {
        StepVerifier.create(Mono.fromRunnable(action))
                    .expectError(I18nSupportException.class)
                    .verify();
    }
}
