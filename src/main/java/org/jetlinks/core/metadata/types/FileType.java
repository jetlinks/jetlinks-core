package org.jetlinks.core.metadata.types;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.ValidateResult;
import org.jetlinks.core.metadata.unit.ValueUnits;
import org.springframework.http.MediaType;

import java.math.RoundingMode;
import java.util.Optional;

import static java.util.Optional.ofNullable;

@Getter
@Setter
public class FileType extends AbstractType<FileType> implements DataType, Converter<String> {
    public static final String ID = "file";

    private BodyType bodyType = BodyType.url;

    private MediaType mediaType = MediaType.ALL;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return LocaleUtils.resolveMessage("message.metadata.type.file", LocaleUtils.current(), "文件");
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

    public FileType mediaType(MediaType type) {
        if (type != null) {
            this.mediaType = type;
        }
        return this;
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

    @Override
    public JSONObject toJson() {
        JSONObject json = super.toJson();
        json.put("bodyType", this.getBodyType().name());
        json.put("mediaType", this.getMediaType().toString());
        return json;
    }

    @Override
    public void fromJson(JSONObject json) {
        super.fromJson(json);
        Optional.ofNullable(json.get("bodyType"))
                .map(String::valueOf)
                .flatMap(FileType.BodyType::of)
                .ifPresent(this::setBodyType);

        Optional.ofNullable(json.get("mediaType"))
                .map(String::valueOf)
                .map(MediaType::parseMediaType)
                .ifPresent(this::setMediaType);

    }
}
