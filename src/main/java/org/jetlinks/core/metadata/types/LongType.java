package org.jetlinks.core.metadata.types;

public class LongType extends NumberType {
    public static final String ID = "long";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "长整型";
    }

    @Override
    public String getDescription() {
        return "64位整型数字";
    }

}
