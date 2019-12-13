package org.jetlinks.core.metadata.types;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.ValidateResult;

import java.util.Optional;

@Getter
@Setter
public class FileType extends AbstractType<FileType> implements DataType, Converter<String> {
    public static final String ID = "file";

    private BodyType bodyType = BodyType.url;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "文件";
    }

    public FileType bodyType(BodyType type) {
        this.bodyType = type;
        return this;
    }

    @Override
    public ValidateResult validate(Object value) {
        return ValidateResult.success(String.valueOf(value));
    }

    @Override
    public String format(Object value) {
        return String.valueOf(value);
    }

    @Override
    public String convert(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    public enum BodyType {
        url,
        base64,
        binary;

        public static Optional<BodyType> of(String name) {
            if (name == null) {
                return Optional.empty();
            }
            for (BodyType value : values()) {
                if (value.name().equalsIgnoreCase(name)) {
                    return Optional.of(value);
                }
            }
            return Optional.empty();
        }
    }
}
