package org.jetlinks.core.metadata.types;

public class IntType extends NumberType {
    public static final String ID = "int";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "整型";
    }

    @Override
    public String getDescription() {
        return "32位整型数字";
    }

}
