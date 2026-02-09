package org.jetlinks.core.principal;


import jakarta.annotation.Nonnull;

public interface Identity {

    // 身份类型
    @Nonnull
    String getType();

    // 身份标识
    @Nonnull
    String getIdentifier();


    static Identity create(String type,String identifier){
        return new SimpleIdentity(type,identifier);
    }
}
