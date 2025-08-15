package org.jetlinks.core.utils.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.PooledDataBuffer;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;

/**
 * JSON 字段提取器，用于从 JSON 流中提取指定字段的值
 * 使用实用的混合方案：适度收集数据后进行解析，平衡内存效率和实现复杂度
 *
 * @author zhouhao
 * @since 2.2
 */
public class JsonFieldExtractor {

    private final String fieldPath;
    private final List<String> pathSegments;
    private final ObjectMapper mapper;

    public JsonFieldExtractor(String fieldPath, ObjectMapper mapper) {
        this.fieldPath = fieldPath;
        this.pathSegments = Arrays.asList(fieldPath.split("\\."));
        this.mapper = mapper;
    }

    /**
     * 从数据流中提取字段值
     * 使用混合策略：收集数据并进行高效解析，跳过不相关的部分
     *
     * @param stream    数据流
     * @param valueType 值类型
     * @return 字段值流
     */
    public <T> Flux<T> extractField(Flux<DataBuffer> stream, Class<T> valueType) {
        return DataBufferUtils
            .join(stream)
            .<T>flatMapMany(bytes -> Flux
                .create(sink -> {
                    try (JsonParser parser = mapper
                        .getFactory()
                        .createParser(bytes.asInputStream(true))) {
                        parseJsonStreamForField(parser, pathSegments, 0, sink, valueType);
                        sink.complete();
                    } catch (Exception e) {
                        sink.error(e);
                    }
                }))
            .doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release);
    }

    /**
     * 高效的递归解析，专注于目标字段路径，跳过不相关的数据
     * 这种方法比异步状态机简单可靠，同时在解析时跳过大量无关数据
     */
    @SneakyThrows
    private <T> void parseJsonStreamForField(JsonParser parser, List<String> pathSegments,
                                             int currentDepth, reactor.core.publisher.FluxSink<T> sink,
                                             Class<T> valueType) {
        JsonToken token;
        while ((token = parser.nextToken()) != null && !sink.isCancelled()) {
            if (token == JsonToken.FIELD_NAME) {
                String fieldName = parser.getCurrentName();

                if (currentDepth < pathSegments.size() &&
                    fieldName.equals(pathSegments.get(currentDepth))) {

                    // 移动到字段值
                    JsonToken valueToken = parser.nextToken();

                    if (currentDepth == pathSegments.size() - 1) {
                        // 到达目标字段
                        if (valueToken == JsonToken.START_ARRAY) {
                            // 处理数组值 - 这里是关键优化点
                            extractArrayValuesEfficiently(parser, sink, valueType);
                        } else if (valueToken != JsonToken.VALUE_NULL) {
                            // 处理单个值
                            try {
                                T value = mapper.readValue(parser, valueType);
                                if (value != null) {
                                    sink.next(value);
                                }
                            } catch (Throwable e) {
                                sink.error(e);
                            }
                        }
                    } else if (valueToken == JsonToken.START_OBJECT) {
                        // 继续深入嵌套对象
                        parseJsonStreamForField(parser, pathSegments, currentDepth + 1, sink, valueType);
                    } else if (valueToken == JsonToken.START_ARRAY) {
                        // 处理嵌套数组中的对象
                        while (parser.nextToken() != JsonToken.END_ARRAY && !sink.isCancelled()) {
                            if (parser.getCurrentToken() == JsonToken.START_OBJECT) {
                                parseJsonStreamForField(parser, pathSegments, currentDepth + 1, sink, valueType);
                            }
                        }
                    }
                } else {
                    // 【关键优化】跳过不相关的字段 - 避免解析大量无用数据
                    parser.nextToken();
                    parser.skipChildren();
                }
            }
        }
    }

    /**
     * 高效提取数组中的值，支持多种数据类型
     * 这是主要的性能优化点：直接从流中提取值，避免构建中间对象
     */
    @SneakyThrows
    private <T> void extractArrayValuesEfficiently(JsonParser parser, reactor.core.publisher.FluxSink<T> sink, Class<T> valueType) {
        JsonToken token;
        while ((token = parser.nextToken()) != null && token != JsonToken.END_ARRAY && !sink.isCancelled()) {
            if (token != JsonToken.VALUE_NULL) {
                try {
                    T value = mapper.readValue(parser, valueType);
                    if (value != null) {
                        sink.next(value);
                    }
                } catch (Exception e) {
                    // 跳过无法解析的值
                }
            }
        }
    }
}