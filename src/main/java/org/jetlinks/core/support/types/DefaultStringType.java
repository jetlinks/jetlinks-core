package org.jetlinks.core.support.types;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.Jsonable;
import org.jetlinks.core.metadata.ValidateResult;
import org.jetlinks.core.metadata.unit.ValueUnit;

import static java.util.Optional.ofNullable;

@Getter
@Setter
public class DefaultStringType implements org.jetlinks.core.metadata.types.StringType, Jsonable {

    private JetLinksStandardValueUnit unit;

    private Integer maxLength;

    @Override
    public ValidateResult validate(Object value) {

        return value != null && value.toString().length() > 0
                ? ValidateResult.success()
                : ValidateResult.fail("字符串不能为空");
    }

    @Override
    public ValueUnit getUnit() {
        return unit;
    }

    @Override
    public String getId() {
        return "string";
    }

    @Override
    public String getName() {
        return "字符串";
    }

    @Override
    public String getDescription() {
        return "字符串";
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        if (unit != null) {
            json.put("unit", unit.toJson());
            json.put("maxLength",unit.toJson());
        }
        return json;
    }

    @Override
    public void fromJson(JSONObject json) {
        setMaxLength(json.getInteger("maxLength"));
        ofNullable(json.get("unit"))
                .map(JetLinksStandardValueUnit::of)
                .ifPresent(this::setUnit);
    }
}
