package org.jetlinks.core.metadata;

import org.jetlinks.core.metadata.types.AbstractType;

import java.util.Objects;

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
        return "用户";
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }
}
