package org.jetlinks.core.support;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.FunctionMetadata;
import org.jetlinks.core.metadata.Jsonable;
import org.jetlinks.core.metadata.PropertyMetadata;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public class JetLinksDeviceFunctionMetadata implements FunctionMetadata {

    private transient JSONObject jsonObject;

    private transient FunctionMetadata another;

    private List<PropertyMetadata> inputs;

    private PropertyMetadata output;

    @Getter
    @Setter
    private String id;

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private String description;

    @Getter
    @Setter
    private boolean async;

    public JetLinksDeviceFunctionMetadata(JSONObject jsonObject) {
        fromJson(jsonObject);
    }

    public JetLinksDeviceFunctionMetadata(FunctionMetadata another) {
        this.another = another;
    }

    @Override
    public List<PropertyMetadata> getInputs() {
        if (inputs == null && jsonObject != null) {
            inputs = Optional.ofNullable(jsonObject.getJSONArray("inputs"))
                    .map(Collection::stream)
                    .map(stream -> stream
                            .map(JSONObject.class::cast)
                            .map(JetLinksPropertyMetadata::new)
                            .map(PropertyMetadata.class::cast)
                            .collect(Collectors.toList()))
                    .orElse(Collections.emptyList());
        }
        if (inputs == null && another != null) {
            inputs = another.getInputs()
                    .stream()
                    .map(JetLinksPropertyMetadata::new)
                    .collect(Collectors.toList());
        }
        return inputs;
    }

    @Override
    public PropertyMetadata getOutput() {
        if (output == null && jsonObject != null) {
            output = Optional.ofNullable(jsonObject.getJSONObject("output"))
                    .map(JetLinksPropertyMetadata::new)
                    .orElse(null);
        }
        if (output == null && another != null) {
            output = Optional.ofNullable(another.getOutput())
                    .map(JetLinksPropertyMetadata::new)
                    .orElse(null);
        }
        return output;
    }

    @Override
    public String toString() {
        // /*获取系统信息*/ getSysInfo(Type name,)

        return String.join("", new String[]{
                "/* ", getName(), " */",
                getId(),
                "(",
                String.join(",", getInputs().stream().map(PropertyMetadata::toString).toArray(String[]::new))
                , ")"
        });
    }

    @Override
    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("name", name);
        jsonObject.put("description", description);
        jsonObject.put("async", async);
        jsonObject.put("inputs", getInputs().stream().map(Jsonable::toJson).collect(Collectors.toList()));
        Optional.ofNullable(getOutput())
                .map(Jsonable::toJson)
                .ifPresent(output -> jsonObject.put("output", output));

        return jsonObject;
    }

    @Override
    public void fromJson(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
        this.inputs = null;
        this.output = null;
        this.id = jsonObject.getString("id");
        this.name = jsonObject.getString("name");
        this.description = jsonObject.getString("description");
        this.async = jsonObject.getBooleanValue("async");
    }
}
