package org.jetlinks.core.metadata.validator;

import org.jetlinks.core.metadata.ValidateResult;

public interface Validator {

    ValidateResult validate(Object value);

    /**
     * 自定义正则校验器
     *
     * @param regex 正则表达式
     * @param errorMessage 自定义校验错误信息
     * @return Validator
     */
    static Validator regexValidator(String regex, String errorMessage) {
        return new RegexValidator(regex, errorMessage);
    }

    /**
     * 复杂密码校验器
     * <p>密码长度为：[4,maxLength]</p>
     *
     * @param maxLength 密码最大长度
     * @param errorMessage 自定义校验错误信息
     * @return Validator
     */
    static Validator complexPasswordValidator(int maxLength, String errorMessage) {
        return new ComplexPasswordRegexValidator(maxLength, errorMessage);
    }
}
