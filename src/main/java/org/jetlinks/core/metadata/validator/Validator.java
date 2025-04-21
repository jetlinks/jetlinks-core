package org.jetlinks.core.metadata.validator;

public interface Validator {

    boolean validate(Object value);

    /**
     * 自定义正则校验器
     *
     * @param regex 正则表达式
     * @return Validator
     */
    static Validator regexValidator(String regex) {
        return new RegexValidator(regex);
    }

    /**
     * 复杂密码校验器
     * <p>密码长度为：[4,maxLength]</p>
     *
     * @param maxLength 密码最大长度
     * @return Validator
     */
    static Validator complexPasswordValidator(int maxLength) {
        return new ComplexPasswordRegexValidator(maxLength);
    }
}
