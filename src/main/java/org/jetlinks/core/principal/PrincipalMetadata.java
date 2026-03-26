package org.jetlinks.core.principal;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class PrincipalMetadata implements Serializable {

    /**
     * 身份类型
     */
    private String type;

    /**
     * 身份标识
     */
    private String identifier;

    /**
     * 名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 凭证类型
     */
    private CredentialType credentialType;

    /**
     * 凭证描述
     */
    private CredentialSpec credentialSpec;
}
