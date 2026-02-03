package org.jetlinks.core.device.identity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IdentityMetadata {

    /**
     * 身份类型
     */
    private String type;

    /**
     * 身份标识
     */
    private String identity;

    /**
     * 凭证类型
     */
    private CredentialType credentialType;
}
