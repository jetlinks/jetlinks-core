package org.jetlinks.core.metadata;

import com.alibaba.fastjson.JSONObject;
import io.vavr.Function3;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SimpleDeviceMetadata implements DeviceMetadata {
    private volatile Map<String, PropertyMetadata> properties = new LinkedHashMap<>();

    private volatile Map<String, FunctionMetadata> functions = new LinkedHashMap<>();

    private volatile Map<String, EventMetadata> events = new LinkedHashMap<>();

    private volatile Map<String, PropertyMetadata> tags = new LinkedHashMap<>();

    @Getter
    @Setter
    private String id;

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private String description;

    @Setter
    private Map<String, Object> expands;

    public void addProperty(PropertyMetadata metadata) {
        this.properties.put(metadata.getId(), metadata);
    }

    public void addFunction(FunctionMetadata metadata) {
        this.functions.put(metadata.getId(), metadata);
    }

    public void addEvent(EventMetadata metadata) {
        this.events.put(metadata.getId(), metadata);
    }

    public void addTag(PropertyMetadata metadata) {
        this.tags.put(metadata.getId(), metadata);
    }

    @Override
    public List<PropertyMetadata> getProperties() {
        return new ArrayList<>(properties.values());
    }

    @Override
    public List<FunctionMetadata> getFunctions() {

        return new ArrayList<>(functions.values());
    }

    @Override
    public List<PropertyMetadata> getTags() {
        return new ArrayList<>(tags.values());
    }

    @Override
    public List<EventMetadata> getEvents() {
        return new ArrayList<>(events.values());
    }

    @Override
    public EventMetadata getEventOrNull(String id) {
        if (events == null) {
            return null;
        }
        return events.get(id);
    }

    @Override
    public PropertyMetadata getPropertyOrNull(String id) {
        if (properties == null) {
            return null;
        }
        return properties.get(id);
    }

    @Override
    public FunctionMetadata getFunctionOrNull(String id) {
        if (functions == null) {
            return null;
        }
        return functions.get(id);
    }

    @Override
    public PropertyMetadata getTagOrNull(String id) {
        if (tags == null) {
            return null;
        }
        return tags.get(id);
    }

    public Map<String, Object> getExpands() {
        return this.expands;
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name);
        json.put("description", description);
        json.put("properties", getProperties().stream().map(Jsonable::toJson).collect(Collectors.toList()));
        json.put("functions", getFunctions().stream().map(Jsonable::toJson).collect(Collectors.toList()));
        json.put("events", getEvents().stream().map(Jsonable::toJson).collect(Collectors.toList()));
        json.put("expands", expands);
        return json;
    }

    @Override
    public void fromJson(JSONObject json) {

        this.properties = null;
        this.events = null;
        this.functions = null;
        this.id = json.getString("id");
        this.name = json.getString("name");
        this.description = json.getString("description");
        this.expands = json.getJSONObject("expands");

    }

    private <V extends Metadata> void doMerge(Map<String, V> map,
                                              V value,
                                              Function3<V, V, MergeOption[], V> mergeFunction,
                                              MergeOption... options) {

        map.compute(value.getId(), (k, old) -> {
            if (old == null) {
                return value;
            }
            //忽略已存在的物模型
            if (MergeOption.has(MergeOption.ignoreExists, options)) {
                return old;
            }
            return mergeFunction.apply(old, value, options);
        });

    }

    @Override
    public DeviceMetadata merge(DeviceMetadata metadata, MergeOption... options) {
        SimpleDeviceMetadata deviceMetadata = new SimpleDeviceMetadata();
        deviceMetadata.setId(metadata.getId());
        deviceMetadata.setName(metadata.getName());
        deviceMetadata.setDescription(metadata.getDescription());
        deviceMetadata.setExpands(metadata.getExpands());


        if (MergeOption.has(MergeOption.overwriteProperty, options)) {
            deviceMetadata.properties.clear();
        }

        for (PropertyMetadata property : metadata.getProperties()) {
            doMerge(deviceMetadata.properties, property, PropertyMetadata::merge, options);
        }
        //属性过滤
        if (MergeOption.PropertyFilter.has(options)) {
            Map<String, PropertyMetadata> temp = new LinkedHashMap<>(deviceMetadata.properties);
            deviceMetadata.properties.clear();
            for (Map.Entry<String, PropertyMetadata> entry : temp.entrySet()) {
                if (MergeOption.PropertyFilter.doFilter(entry.getValue(), options)) {
                    deviceMetadata.properties.put(entry.getKey(), entry.getValue());
                }
            }
        }

        for (FunctionMetadata func : metadata.getFunctions()) {
            doMerge(deviceMetadata.functions, func, FunctionMetadata::merge, options);
        }

        for (EventMetadata event : metadata.getEvents()) {
            doMerge(deviceMetadata.events, event, EventMetadata::merge, options);

        }

        for (PropertyMetadata tag : metadata.getTags()) {
            doMerge(deviceMetadata.tags, tag, PropertyMetadata::merge, options);
        }

        return deviceMetadata;
    }
}
