package org.jetlinks.core.device.identity;


import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public interface Identity {

    // 身份类型
    @Nonnull
    String getType();

    // 身份标识
    @Nonnull
    String getIdentifier();

    // 身份证明
    @Nullable
    Credential getCredential();

}
