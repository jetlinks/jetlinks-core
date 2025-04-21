package org.jetlinks.core.metadata.validator;

/**
 * 复杂密码正则校验器
 * <p>规则：</p>
 * <li>至少一个大写字母</li>
 * <li>至少一个小写字母</li>
 * <li>至少一个数字</li>
 * <li>至少一个特殊字符</li>
 * <li>密码长度 [4,maxLength] </li>
 */
public class ComplexPasswordRegexValidator extends RegexValidator {

    public ComplexPasswordRegexValidator(int maxLength) {
        super(String.format("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[\\p{Punct}]).{%d,%d}$", 4, maxLength));
    }
}
