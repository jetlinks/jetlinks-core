package org.jetlinks.core.metadata;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.core.metadata.types.DataTypes;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 可独立构造及进行 JSON 往返的功能定义，输入输出使用 core 注册的数据类型。
 * 实例由调用方持有和修改，不保存执行器或调用上下文。
 *
 * @see FunctionMetadata
 */
@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class SimpleFunctionMetadata implements FunctionMetadata {

    private String id;
    private String name;
    private String description;
    private Map<String, Object> expands;
    private List<PropertyMetadata> inputs;
    private DataType output;
    private boolean async;

    public static SimpleFunctionMetadata of(String id, String name, List<PropertyMetadata> inputs, DataType output) {
        return SimpleFunctionMetadata.of(id, name, null, null, inputs, output, false);
    }

    public static SimpleFunctionMetadata of(FunctionMetadata metadata) {
        return SimpleFunctionMetadata.of(metadata.getId(),
                                         metadata.getName(),
                                         metadata.getDescription(),
                                         metadata.getExpands() == null ? null : new HashMap<>(metadata.getExpands()),
                                        new ArrayList<>(metadata.getInputs()),
                                         metadata.getOutput(),
                                         metadata.isAsync());
    }

    public SimpleFunctionMetadata copy() {
        return of(id,
                  name,
                  description,
                  expands == null ? null : new HashMap<>(expands),
                  new ArrayList<>(inputs),
                  output,
                  async);
    }

    public <T> SimpleFunctionMetadata expand(ConfigKey<T> key, T value) {
        return expand(key.getKey(), value);
    }

    public synchronized SimpleFunctionMetadata expand(String key, Object value) {
        if (expands == null) {
            expands = new HashMap<>();
        } else if (!(expands instanceof HashMap)) {
            expands = Maps.newHashMap(expands);
        }
        expands.put(key, value);
        return this;
    }

    @Override
    public @Nonnull List<PropertyMetadata> getInputs() {
        if (inputs == null) {
            return Collections.emptyList();
        }
        return inputs;
    }

    @Override
    public void fromJson(JSONObject json) {
        json = Jsonable.requireObject(json);
        List<PropertyMetadata> parsedInputs = new ArrayList<>();
        JSONArray definitions = json.getJSONArray("inputs");
        if (definitions != null) {
            for (Object definition : definitions) {
                SimplePropertyMetadata input = new SimplePropertyMetadata();
                input.fromJson(Jsonable.requireObject(definition));
                parsedInputs.add(input);
            }
        }
        JSONObject outputJson = json.getJSONObject("output");
        DataType parsedOutput = outputJson == null ? null : DataTypes.fromJsonNow(outputJson);
        id = json.getString("id");
        name = json.getString("name");
        description = json.getString("description");
        expands = json.getJSONObject("expands");
        async = json.getBooleanValue("async");
        inputs = parsedInputs;
        output = parsedOutput;
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name);
        json.put("description", description);
        json.put("expands", expands);
        json.put("async", async);
        json.put("inputs", getInputs().stream().map(Jsonable::toJson).collect(Collectors.toList()));
        json.put("output", output == null ? null : output.toJson());
        return json;
    }

    @Override
    public FunctionMetadata merge(FunctionMetadata another, MergeOption... option) {
        return another;
    }

    @Override
    public String toString() {
        return (output == null ? "void" : output) + " " + id + "(" + (CollectionUtils.isEmpty(inputs) ? "" : inputs) + ")";
    }
}
