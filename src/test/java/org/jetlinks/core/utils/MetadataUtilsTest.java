package org.jetlinks.core.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.validator.CreateGroup;
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
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static java.lang.annotation.ElementType.*;

public class MetadataUtilsTest {


    @Test
    public void testParseExpands(){

    }

    @Test
    public void testAttr() {

        ObjectType type = (ObjectType) MetadataUtils.parseType(ResolvableType.forType(TestEntity.class));

        System.out.println(JSON.toJSONString(type, SerializerFeature.PrettyFormat));

        // tshark -i any -f "tcp port 28400" -Y "tcp.flags.reset == 1 or tcp.flags.fin == 1"
    }

    @Test
    public void testCollectionParse() {
        ObjectType type = (ObjectType) MetadataUtils.parseType(ResolvableType.forType(CollectionsTest.class));

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
        @NotBlank(groups = CreateGroup.class)
        private String deviceId;

        @Schema(title = "产品ID")
        @Pattern(regexp = "^[0-9a-zA-Z_\\-]+$")
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

    @Getter
    @Setter
    public static class CollectionsTest {


        @Schema(title = "set集合")
        private Set<String> set;

        @Schema(title = "list集合")
        private List<String> list;

        @Schema(title = "collection集合")
        private Collection<String> collection;
    }

}