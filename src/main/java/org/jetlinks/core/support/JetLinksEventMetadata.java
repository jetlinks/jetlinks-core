package org.jetlinks.core.support;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.EventMetadata;
import org.jetlinks.core.metadata.Jsonable;
import org.jetlinks.core.metadata.PropertyMetadata;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public class JetLinksEventMetadata implements EventMetadata {

    private JSONObject jsonObject;

    private List<PropertyMetadata> parameters;

    private transient EventMetadata another;

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
    private Map<String, Object> expands;


    public JetLinksEventMetadata(JSONObject jsonObject) {
       fromJson(jsonObject);
    }

    public JetLinksEventMetadata(EventMetadata another) {
        this.another = another;
    }

    @Override
    public List<PropertyMetadata> getParameters() {
        if (parameters == null && jsonObject != null) {
            parameters = Optional.ofNullable(jsonObject.getJSONArray("parameters"))
                    .map(Collection::stream)
                    .map(stream -> stream
                            .map(JSONObject.class::cast)
                            .map(JetLinksPropertyMetadata::new)
                            .map(PropertyMetadata.class::cast)
                            .collect(Collectors.toList()))
                    .orElse(Collections.emptyList());
        }
        if (parameters == null && another != null) {
            parameters = another.getParameters()
                    .stream()
                    .map(JetLinksPropertyMetadata::new)
                    .collect(Collectors.toList());
        }
        return parameters;
    }

    @Override
    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("name", name);
        jsonObject.put("description", description);
        jsonObject.put("parameters", getParameters().stream().map(Jsonable::toJson).collect(Collectors.toList()));
        jsonObject.put("expands",expands);
        return jsonObject;
    }

    @Override
    public void fromJson(JSONObject json) {
        this.jsonObject = json;
        this.parameters = null;
        this.id = json.getString("id");
        this.name = json.getString("name");
        this.description = json.getString("description");
        this.expands=json.getJSONObject("expands");

    }
}
