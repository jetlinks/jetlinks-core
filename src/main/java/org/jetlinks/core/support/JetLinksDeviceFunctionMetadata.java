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

    @Getter
    @Setter
    private Map<String, Object> expands;

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
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name);
        json.put("description", description);
        json.put("async", async);
        json.put("inputs", getInputs().stream().map(Jsonable::toJson).collect(Collectors.toList()));
        Optional.ofNullable(getOutput())
                .map(Jsonable::toJson)
                .ifPresent(output -> json.put("output", output));
        json.put("expands", expands);

        return json;
    }

    @Override
    public void fromJson(JSONObject json) {
        this.jsonObject = json;
        this.inputs = null;
        this.output = null;
        this.id = json.getString("id");
        this.name = json.getString("name");
        this.description = json.getString("description");
        this.async = json.getBooleanValue("async");
        this.expands=json.getJSONObject("expands");
    }
}
