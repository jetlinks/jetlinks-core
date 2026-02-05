package org.jetlinks.core.json;

import org.jetlinks.core.utils.json.ObjectMappers;
import org.junit.Assert;
import org.junit.Test;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ObjectMappersTest {

    @Test
    public void testJsonlNoLeadingSpace() {
        List<Map<String, Object>> data = Arrays.asList(
            Map.of("id", 1, "name", "a"),
            Map.of("id", 2, "name", "b"),
            Map.of("id", 3, "name", "c")
        );

        byte[] bytes = ObjectMappers
            .toJsonlStream(Flux.fromIterable(data))
            .reduce(new StringBuilder(), (sb, part) -> sb.append(new String(part, StandardCharsets.UTF_8)))
            .map(sb -> sb.toString().getBytes(StandardCharsets.UTF_8))
            .block();

        Assert.assertNotNull(bytes);

        String text = new String(bytes, StandardCharsets.UTF_8);
        String[] lines = text.split("\\n", -1);

        // 最后一行为空(末尾换行),去掉
        int end = lines.length;
        if (end > 0 && lines[end - 1].isEmpty()) {
            end--;
        }
        Assert.assertTrue(end >= 3);

        for (int i = 0; i < end; i++) {
            Assert.assertFalse("line " + i + " starts with space: [" + lines[i] + "]", lines[i].startsWith(" "));
        }

        // 校验每行都是合法 JSON
        for (int i = 0; i < end; i++) {
            ObjectMappers.parseJson(lines[i], Map.class);
        }
    }
}
