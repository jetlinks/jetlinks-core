package org.jetlinks.core.support;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public class JetLinksDeviceMetadata implements DeviceMetadata {

    private JSONObject jsonObject;

    private List<PropertyMetadata> properties;

    private List<FunctionMetadata> functions;

    private List<EventMetadata> events;

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


    public JetLinksDeviceMetadata(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public JetLinksDeviceMetadata(DeviceMetadata another) {
        this.properties = another.getProperties()
                .stream()
                .map(JetLinksPropertyMetadata::new)
                .collect(Collectors.toList());
        this.functions = another.getFunctions()
                .stream()
                .map(JetLinksDeviceFunctionMetadata::new)
                .collect(Collectors.toList());
        this.events = another.getEvents()
                .stream()
                .map(JetLinksEventMetadata::new)
                .collect(Collectors.toList());

    }

    @Override
    public List<PropertyMetadata> getProperties() {
        if (properties == null && jsonObject != null) {
            properties = Optional.ofNullable(jsonObject.getJSONArray("properties"))
                    .map(Collection::stream)
                    .map(stream -> stream
                            .map(JSONObject.class::cast)
                            .map(JetLinksPropertyMetadata::new)
                            .map(PropertyMetadata.class::cast)
                            .collect(Collectors.toList()))
                    .orElse(Collections.emptyList());
        }
        return properties;
    }

    @Override
    public List<FunctionMetadata> getFunctions() {
        if (functions == null && jsonObject != null) {
            functions = Optional.ofNullable(jsonObject.getJSONArray("functions"))
                    .map(Collection::stream)
                    .map(stream -> stream
                            .map(JSONObject.class::cast)
                            .map(JetLinksDeviceFunctionMetadata::new)
                            .map(FunctionMetadata.class::cast)
                            .collect(Collectors.toList()))
                    .orElse(Collections.emptyList());
        }
        return functions;
    }

    @Override
    public List<EventMetadata> getEvents() {
        if (events == null && jsonObject != null) {
            events = Optional.ofNullable(jsonObject.getJSONArray("events"))
                    .map(Collection::stream)
                    .map(stream -> stream
                            .map(JSONObject.class::cast)
                            .map(JetLinksEventMetadata::new)
                            .map(EventMetadata.class::cast)
                            .collect(Collectors.toList()))
                    .orElse(Collections.emptyList());
        }
        return events;
    }

    @Override
    public Optional<EventMetadata> getEvent(String id) {
        return getEvents()
                .stream()
                .filter(property -> property.getId().equals(id))
                .findFirst();
    }

    @Override
    public Optional<PropertyMetadata> getProperty(String id) {
        return getProperties()
                .stream()
                .filter(property -> property.getId().equals(id))
                .findFirst();
    }

    @Override
    public Optional<FunctionMetadata> getFunction(String id) {
        return getFunctions()
                .stream()
                .filter(function -> function.getId().equals(id))
                .findFirst();
    }

    @Override
    public JSONObject toJson() {
        JSONObject json=new JSONObject();
        json.put("id", id);
        json.put("name", name);
        json.put("description", description);
        json.put("properties",getProperties().stream().map(Jsonable::toJson).collect(Collectors.toList()));
        json.put("functions",getFunctions().stream().map(Jsonable::toJson).collect(Collectors.toList()));
        json.put("events",getEvents().stream().map(Jsonable::toJson).collect(Collectors.toList()));
        json.put("expands", expands);
        return json;
    }

    @Override
    public void fromJson(JSONObject json) {
        this.jsonObject = json;
        this.properties = null;
        this.events = null;
        this.functions = null;
        this.id = json.getString("id");
        this.name = json.getString("name");
        this.description = json.getString("description");
        this.expands=json.getJSONObject("expands");

    }
}
