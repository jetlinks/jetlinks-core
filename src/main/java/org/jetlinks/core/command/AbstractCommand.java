package org.jetlinks.core.command;

import com.alibaba.fastjson.JSONObject;
import lombok.EqualsAndHashCode;
import org.jetlinks.core.metadata.Jsonable;
import org.jetlinks.core.utils.ConverterUtils;
import org.jetlinks.core.utils.SerializeUtils;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@EqualsAndHashCode(of = "properties")
public abstract class AbstractCommand<Response, Self extends AbstractCommand<Response, Self>>
    implements Command<Response>, Jsonable, Externalizable {

    private Map<String, Object> properties;

    @Override
    public final Self with(String key, Object value) {
        writable().put(key, value);
        return castSelf();
    }

    @Override
    public final Self with(Map<String, Object> properties) {
        writable().putAll(properties);
        return castSelf();
    }

    @Override
    public <T> T getOrNull(String key, Type type) {
        return ConverterUtils.convert(readable().get(key), type);
    }

    public Map<String, Object> readable() {
        return properties == null ? Collections.emptyMap() : properties;
    }

    public Map<String, Object> writable() {
        return properties == null ? properties = new HashMap<>() : properties;
    }

    @SuppressWarnings("all")
    protected Self castSelf() {
        return (Self) this;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        SerializeUtils.writeKeyValue(properties, out);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        SerializeUtils.readKeyValue(in, writable()::put);
    }

    @Override
    public JSONObject toJson() {
        return new JSONObject(readable());
    }

    @Override
    public void fromJson(JSONObject json) {
        writable().putAll(json);
    }
}
