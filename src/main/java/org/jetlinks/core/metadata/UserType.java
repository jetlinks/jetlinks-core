package org.jetlinks.core.metadata;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.metadata.types.AbstractType;

import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

public class UserType extends AbstractType<UserType> {

    public static final String ID = "user";

    /**
     * 用户的属性标识,默认id
     *
     * @see org.jetlinks.core.things.relation.ObjectProperty
     * @see org.jetlinks.core.things.relation.PropertyOperation
     */
    private String property = "id";

    public static UserType ofProperty(String property) {
        UserType type = new UserType();
        type.setProperty(Objects.requireNonNull(property));
        return type;
    }

    @Override
    public ValidateResult validate(Object value) {
        return ValidateResult.success(value);
    }

    @Override
    public Object format(Object value) {
        return value;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return LocaleUtils.resolveMessage("message.metadata.type.user", LocaleUtils.current(), "用户");
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }


    @Override
    public JSONObject toJson() {
        JSONObject json = super.toJson();
        json.put("property", this.getProperty());
        return json;
    }

    @Override
    public void fromJson(JSONObject json) {
        super.fromJson(json);
        ofNullable(json.getString("property"))
                .ifPresent(this::setProperty);
    }
}
