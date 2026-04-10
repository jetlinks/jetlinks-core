package org.jetlinks.core.principal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
public class CredentialSpec {

    // token 凭证描述
    private TokenSpec token;


    public static CredentialSpec of(TokenSpec token) {
        CredentialSpec spec = new CredentialSpec();
        spec.token = token;
        return spec;
    }


    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TokenSpec{

        // token 格式
        private ContentFormat format;

        // 最大长度
        private int length;

    }


}
