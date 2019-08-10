package org.jetlinks.core.support.types;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.DataTypeCodec;
import org.jetlinks.core.metadata.types.ArrayType;
import org.jetlinks.core.metadata.types.DataTypes;
import org.jetlinks.core.support.JetLinksDataTypeCodecs;

import java.util.Map;

import static java.util.Optional.ofNullable;

@Getter
@Setter
public class JetLinksArrayCodec implements DataTypeCodec<ArrayType> {

    @Override
    public String getTypeId() {
        return ArrayType.ID;
    }

    @Override
    public ArrayType decode(ArrayType type, Map<String, Object> config) {
        JSONObject jsonObject = new JSONObject(config);

        ofNullable(jsonObject.getJSONObject("eleType"))
                .map(eleType -> {
                    DataType dataType = DataTypes.lookup(eleType.getString("type")).get();

                     JetLinksDataTypeCodecs.getCodec(dataType.getId())
                            .ifPresent(codec->codec.decode(dataType,eleType));

                    return dataType;
                })
                .ifPresent(type::setElementType);

        return type;
    }

    @Override
    public Map<String, Object> encode(ArrayType type) {
        JSONObject json = new JSONObject();
        JetLinksDataTypeCodecs.getCodec(type.getId())
                .ifPresent(codec->{
                    json.put("eleType", codec.encode(type.getElementType()));

                });

        return json;
    }
}
