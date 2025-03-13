package org.jetlinks.core.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.annotation.Attr;
import org.jetlinks.core.annotation.Expands;
import org.jetlinks.core.annotation.ui.Selector;
import org.jetlinks.core.metadata.types.ObjectType;
import org.junit.Test;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.ElementType.PARAMETER;

public class MetadataUtilsTest {


    @Test
    public void testParseExpands(){

    }

    @Test
    public void testAttr() {

        ObjectType type = (ObjectType) MetadataUtils.parseType(ResolvableType.forType(TestEntity.class));

        System.out.println(JSON.toJSONString(type, SerializerFeature.PrettyFormat));

    }


    @Target({FIELD, METHOD, PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @Selector(type = "device")
    public @interface DeviceSelector {

        @AliasFor(annotation = Selector.class)
        boolean multiple() default false;

        CustomAnnotation custom() default @CustomAnnotation;

        CustomAnnotation[] customArr() default {
            @CustomAnnotation,
            @CustomAnnotation
        };
    }


    @Target({FIELD, METHOD, PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    public @interface CustomAnnotation {
        String x() default "1";

        String y() default "0";
    }

    @Getter
    @Setter
    public static class TestEntity {

        @Schema(title = "设备ID")
        @DeviceSelector(multiple = true,custom = @CustomAnnotation(x = "2"))
        private String deviceId;

        @Schema(title = "产品ID")
        public String productId;

        @Expands({@Attr(key = "k1", value = "v1"), @Attr(key = "k2", value = "v2")})
        @Expands(key = "test2", value = {
            @Attr(key = "k1", value = "v1"),
            @Attr(key = "k2", value = "v2")
        })
        public String getProductId() {
            return productId;
        }
    }

}