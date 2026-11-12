package org.jetlinks.core.metadata;

import org.hswebframework.web.exception.I18nSupportException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 物模型结构解析边界，供模型目录、功能输入及嵌套属性共用。
 * 仅接收 Map/Collection 表示的定义，由调用方解码具体类型；不依赖 JSON 引擎或执行 I/O。
 * 无共享可变状态，返回独立容器，嵌套值沿用输入的值语义。
 *
 * @since 1.3.3
 * @see SimpleDeviceMetadata
 */
public final class MetadataDefinitionParser {

    private MetadataDefinitionParser() {
    }

    /**
     * 读取非空对象定义，不将标量、字符串或 Bean 隐式转换为物模型对象。
     *
     * @param definition 字符串键 Map 表示的对象定义
     * @return 独立的对象容器
     * @throws I18nSupportException 输入不是对象或键不是字符串
     */
    public static Map<String, Object> parseObject(Object definition) {
        if (!(definition instanceof Map)) {
            throw new I18nSupportException.NoStackTrace("error.invalid_metadata_definition");
        }
        Map<String, Object> parsed = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) definition).entrySet()) {
            if (!(entry.getKey() instanceof String)) {
                throw new I18nSupportException.NoStackTrace("error.invalid_metadata_definition");
            }
            parsed.put((String) entry.getKey(), entry.getValue());
        }
        return parsed;
    }

    /**
     * 按原顺序解码对象目录；缺失或 null 目录为空，目录中的 null 不是空定义。
     *
     * @param definitions Collection 表示的目录，可为 null
     * @param decoder 对象定义解码器，不修改来源容器
     * @return 完整解码后的独立列表；任一条目失败时传播错误
     * @throws I18nSupportException 目录不是集合，或存在非对象条目
     */
    public static <T> List<T> parseList(Object definitions, Function<Map<String, Object>, T> decoder) {
        List<T> parsed = new ArrayList<>();
        if (definitions == null) {
            return parsed;
        }
        if (!(definitions instanceof Collection)) {
            throw new I18nSupportException.NoStackTrace("error.invalid_metadata_definition");
        }
        for (Object definition : (Collection<?>) definitions) {
            parsed.add(decoder.apply(parseObject(definition)));
        }
        return parsed;
    }
}
